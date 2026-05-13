package az.kompressor.app.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import az.kompressor.app.data.remote.dto.CarDto
import az.kompressor.app.data.remote.dto.toDomain
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class CarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : CarRepository {

    private val carsCollection = firestore.collection("cars")

    override fun getCars(): Flow<Resource<List<Car>>> = flow {
        emit(Resource.Loading)
        try {
            val now = System.currentTimeMillis()
            val thirtyDays = 30L * 24 * 60 * 60 * 1000
            val snapshot = carsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val cars = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CarDto::class.java)?.copy(id = doc.id)?.toDomain()
            }.filter { car ->
                // Only show approved, non-expired listings in public feed
                val expiry = if (car.expiresAt > 0) car.expiresAt else (car.createdAt + thirtyDays)
                expiry > now && car.status == "approved"
            }
            emit(Resource.Success(cars))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load cars"))
        }
    }

    override fun searchCars(query: String): Flow<Resource<List<Car>>> = flow {
        emit(Resource.Loading)
        try {
            val snapshot = carsCollection.get().await()
            val cars = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CarDto::class.java)?.copy(id = doc.id)?.toDomain()
            }.filter { car ->
                car.status == "approved" && (
                    car.title.contains(query, ignoreCase = true) ||
                    car.brand.contains(query, ignoreCase = true) ||
                    car.model.contains(query, ignoreCase = true) ||
                    car.city.contains(query, ignoreCase = true)
                )
            }
            emit(Resource.Success(cars))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Search failed"))
        }
    }

    override fun getCarById(carId: String): Flow<Resource<Car>> = flow {
        emit(Resource.Loading)
        try {
            val doc = carsCollection.document(carId).get().await()
            val car = doc.toObject(CarDto::class.java)?.copy(id = doc.id)?.toDomain()
                ?: throw Exception("Car not found")
            emit(Resource.Success(car))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load car"))
        }
    }

    override fun getCarsByUser(uid: String): Flow<Resource<List<Car>>> = flow {
        emit(Resource.Loading)
        try {
            // No orderBy here: combining whereEqualTo + orderBy on different fields requires
            // a composite Firestore index that may not exist. Sort client-side instead.
            val snapshot = carsCollection
                .whereEqualTo("sellerUid", uid)
                .get().await()
            val cars = snapshot.documents
                .mapNotNull { doc -> doc.toObject(CarDto::class.java)?.copy(id = doc.id)?.toDomain() }
                .sortedByDescending { it.createdAt }
            emit(Resource.Success(cars))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load your listings"))
        }
    }

    /**
     * Upload one image URI → Firebase Storage → return download URL.
     * Each step is isolated so error messages pinpoint the exact failure.
     */
    private suspend fun uploadUri(uri: Uri, sellerUid: String): String {
        // 1. Resolve UID
        val uid = sellerUid.ifBlank {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("UPLOAD_FAIL: not signed in")
        }

        // 2. Read bytes synchronously via ContentResolver (scoped storage safe on API 29+)
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("UPLOAD_FAIL: ContentResolver returned null stream for $uri")
        } catch (e: Exception) {
            throw Exception("UPLOAD_FAIL [read]: ${e.message}")
        }
        if (bytes.isEmpty()) throw Exception("UPLOAD_FAIL: image bytes are empty")

        // 3. In-memory compression
        val compressed = compressBytes(bytes)
        android.util.Log.d("KompressorUpload", "Compressed ${bytes.size}B → ${compressed.size}B uid=$uid")

        // 4. Upload bytes
        val path = "car_images/$uid/${UUID.randomUUID()}.jpg"
        val ref  = storage.reference.child(path)
        val meta = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg").build()

        try {
            ref.putBytes(compressed, meta).await()
            android.util.Log.d("KompressorUpload", "putBytes OK → $path")
        } catch (e: com.google.firebase.storage.StorageException) {
            throw Exception("UPLOAD_FAIL [putBytes] code=${e.errorCode} http=${e.httpResultCode}: ${e.message}")
        } catch (e: Exception) {
            throw Exception("UPLOAD_FAIL [putBytes]: ${e.message}")
        }

        // 5. Get download URL
        return try {
            val url = ref.downloadUrl.await().toString()
            android.util.Log.d("KompressorUpload", "downloadUrl OK → $url")
            url
        } catch (e: com.google.firebase.storage.StorageException) {
            throw Exception("UPLOAD_FAIL [downloadUrl] code=${e.errorCode} http=${e.httpResultCode}: ${e.message}")
        } catch (e: Exception) {
            throw Exception("UPLOAD_FAIL [downloadUrl]: ${e.message}")
        }
    }

    private fun compressBytes(input: ByteArray): ByteArray {
        val original = android.graphics.BitmapFactory.decodeByteArray(input, 0, input.size)
            ?: return input
        val maxDim = 1280
        val scaled = if (original.width > maxDim || original.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(original.width, original.height)
            android.graphics.Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            ).also { original.recycle() }
        } else original
        val out = java.io.ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, out)
        scaled.recycle()
        return out.toByteArray()
    }

    override fun updateCar(
        car: Car,
        newImageUris: List<Uri>,
        existingImageUrls: List<String>
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val newUrls = newImageUris.map { uri -> uploadUri(uri, car.sellerUid) }
            val finalImageUrls = existingImageUrls + newUrls

            val updates = mapOf(
                "title"        to "${car.brand} ${car.model}".trim(),
                "brand"        to car.brand,
                "model"        to car.model,
                "year"         to car.year,
                "price"        to car.price,
                "mileage"      to car.mileage,
                "fuelType"     to car.fuelType,
                "transmission" to car.transmission,
                "city"         to car.city,
                "phone"        to car.phone,
                "description"  to car.description,
                "imageUrls"    to finalImageUrls
            )
            carsCollection.document(car.id).update(updates).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to update listing"))
        }
    }

    override fun deleteCarById(carId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            carsCollection.document(carId).delete().await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to delete car"))
        }
    }

    override fun postCar(car: Car, imageUris: List<Uri>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            // Compress each image before uploading (≤1280px / 82% JPEG → ~200KB avg)
            val imageUrls = imageUris.map { uri -> uploadUri(uri, car.sellerUid) }
            val now = System.currentTimeMillis()
            val carDto = CarDto(
                title = car.title, brand = car.brand, model = car.model,
                year = car.year, price = car.price, mileage = car.mileage,
                fuelType = car.fuelType, transmission = car.transmission,
                city = car.city, description = car.description, phone = car.phone,
                imageUrls = imageUrls, sellerUid = car.sellerUid,
                sellerName = car.sellerName,
                createdAt = now,
                expiresAt = now + 30L * 24 * 60 * 60 * 1000,  // auto-expire after 30 days
                status = run {
                    // Admins skip the approval queue — post goes live immediately
                    val uid = car.sellerUid.ifBlank { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
                    val isAdmin = try { firestore.collection("admins").document(uid).get().await().exists() } catch (_: Exception) { false }
                    if (isAdmin) "approved" else "pending"
                }
            )
            val docRef = carsCollection.add(carDto).await()

            // Notify all admins about the new pending listing (skip if admin posted — auto-approved)
            val postedStatus = carDto.status
            if (postedStatus == "pending") {
                try {
                    val adminDocs = firestore.collection("admins").get().await()
                    val notif = mapOf(
                        "carId"      to docRef.id,
                        "carTitle"   to car.title,
                        "status"     to "new_listing",
                        "message"    to "${car.sellerName} submitted a new listing: ${car.title}",
                        "timestamp"  to System.currentTimeMillis(),
                        "read"       to false
                    )
                    for (adminDoc in adminDocs.documents) {
                        firestore.collection("notifications")
                            .document(adminDoc.id)
                            .collection("items")
                            .add(notif).await()
                    }
                } catch (_: Exception) { /* non-critical */ }
            }

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to post car"))
        }
    }

    override suspend fun bumpCar(carId: String) {
        try {
            val now = System.currentTimeMillis()
            carsCollection.document(carId).update(
                mapOf(
                    "createdAt" to now,
                    "expiresAt" to now + 30L * 24 * 60 * 60 * 1000
                )
            ).await()
        } catch (_: Exception) {
            // Silent — bump failure is non-critical
        }
    }

    override suspend fun incrementViewCount(carId: String) {
        try {
            carsCollection.document(carId)
                .update("viewCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
        } catch (_: Exception) {
            // Silent — never block the UI for a view count write
        }
    }

    override fun getAllCarsAdmin(): Flow<Resource<List<Car>>> = flow {
        emit(Resource.Loading)
        try {
            val snapshot = carsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val cars = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CarDto::class.java)?.copy(id = doc.id)?.toDomain()
            }.sortedWith(compareBy(
                // Pending first, then approved, then rejected
                { when (it.status) { "pending" -> 0; "approved" -> 1; else -> 2 } },
                { -it.createdAt }
            ))
            emit(Resource.Success(cars))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load all cars"))
        }
    }

    override suspend fun updateCarStatus(carId: String, status: String, sellerUid: String, carTitle: String) {
        try {
            carsCollection.document(carId).update("status", status).await()
            // Write in-app notification for the seller
            val notif = mapOf(
                "carId"     to carId,
                "carTitle"  to carTitle,
                "status"    to status,
                "timestamp" to System.currentTimeMillis(),
                "read"      to false
            )
            firestore.collection("notifications")
                .document(sellerUid)
                .collection("items")
                .add(notif).await()
        } catch (_: Exception) {}
    }

    override suspend fun isAdmin(uid: String): Boolean {
        return try {
            firestore.collection("admins").document(uid).get().await().exists()
        } catch (_: Exception) { false }
    }
}
