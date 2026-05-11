package az.kompressor.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
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
                // If expiresAt is set, hide expired listings. If legacy (0), use createdAt + 30d.
                val expiry = if (car.expiresAt > 0) car.expiresAt else (car.createdAt + thirtyDays)
                expiry > now
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
                car.title.contains(query, ignoreCase = true) ||
                car.brand.contains(query, ignoreCase = true) ||
                car.model.contains(query, ignoreCase = true) ||
                car.city.contains(query, ignoreCase = true)
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
     * Compress a content URI to ≤1280px / 82% JPEG and return a file:// URI.
     * Falls back to original URI on any error (better to upload large than fail).
     */
    private fun compressUri(uri: Uri): Uri {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return uri
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val maxDim = 1280
            val scaled = if (original.width > maxDim || original.height > maxDim) {
                val scale = maxDim.toFloat() / maxOf(original.width, original.height)
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt(),
                    true
                ).also { original.recycle() }
            } else original

            val tmp = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tmp).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, 82, out) }
            scaled.recycle()
            Uri.fromFile(tmp)
        } catch (e: Exception) {
            uri // fallback: upload original if compression fails
        }
    }

    private suspend fun uploadUri(uri: Uri, sellerUid: String): String {
        val compressed = compressUri(uri)
        val ref = storage.reference.child("car_images/$sellerUid/${UUID.randomUUID()}.jpg")
        ref.putFile(compressed).await()
        return ref.downloadUrl.await().toString()
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
                expiresAt = now + 30L * 24 * 60 * 60 * 1000   // auto-expire after 30 days
            )
            carsCollection.add(carDto).await()
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

    override suspend fun seedDummyData() {
        // Wipe ALL existing cars (demo + any leftovers from previous sessions)
        try {
            val all = carsCollection.get().await()
            all.documents.forEach { it.reference.delete().await() }
        } catch (_: Exception) { /* silently skip if rules deny */ }

        val now = System.currentTimeMillis()
        val dummyCars = listOf(

            // ── 1. BMW 5 Series 520i — Petrol / Automatic / Baku ─────────────
            CarDto(
                title = "BMW 5 Series 520i M Sport",
                brand = "BMW", model = "5 Series", year = 2022,
                price = 87000, mileage = 18500,
                fuelType = "Petrol", transmission = "Automatic",
                city = "Baku", phone = "+994501112233",
                sellerName = "Kompressor Demo",
                description = "M Sport package with full aerodynamic kit. Black Sapphire Metallic exterior with cognac Merino leather interior. Features include: wireless Apple CarPlay & Android Auto, ambient lighting with 40 colours, Harman Kardon surround sound (16 speakers), adaptive LED headlights with laser high beam, 360° camera system, parking assistant plus, active cruise control with stop & go. One private owner from new. Full BMW dealer service history, last serviced at 15,000km. No accidents, non-smoker vehicle. Two keys. Test drives welcome in Baku.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=1200&q=85",
                    "https://images.unsplash.com/photo-1520050206274-a1ae44613e6d?w=1200&q=85",
                    "https://images.unsplash.com/photo-1607853202273-797f1c22a38e?w=1200&q=85"
                ),
                sellerUid = "demo",
                createdAt = now,
                expiresAt = now + 30L * 24 * 60 * 60 * 1000
            ),

            // ── 2. Mercedes-Benz C300 AMG — Petrol / Automatic / Baku ────────
            CarDto(
                title = "Mercedes-Benz C300 AMG Line",
                brand = "Mercedes", model = "C300", year = 2023,
                price = 96000, mileage = 7200,
                fuelType = "Petrol", transmission = "Automatic",
                city = "Baku", phone = "+994552223344",
                sellerName = "Kompressor Demo",
                description = "Practically brand new — only 7,200km from new. Polar White with AMG Line exterior and Night Package (black chrome trim). Interior: black ARTICO/DINAMICA with red stitching, 64-colour ambient lighting, Burmester 3D surround sound system. Tech package includes: MBUX with Hey Mercedes voice control, augmented reality navigation, digital rear-view mirror, Driving Assistance Package (active lane keeping, blind spot assist, automatic emergency braking). 9G-TRONIC automatic, 258hp, 0–100 in 6.2s. Under full Mercedes 2-year warranty. Official Azerbaijan purchase — full paperwork.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=1200&q=85",
                    "https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=1200&q=85",
                    "https://images.unsplash.com/photo-1553440569-bcc63803a83d?w=1200&q=85"
                ),
                sellerUid = "demo",
                createdAt = now - 3_600_000,
                expiresAt = now + 30L * 24 * 60 * 60 * 1000 - 3_600_000
            ),

            // ── 3. Tesla Model Y Long Range — Electric / Automatic / Baku ────
            CarDto(
                title = "Tesla Model Y Long Range AWD",
                brand = "Tesla", model = "Model Y", year = 2023,
                price = 79000, mileage = 14300,
                fuelType = "Electric", transmission = "Automatic",
                city = "Baku", phone = "+994703334455",
                sellerName = "Kompressor Demo",
                description = "Dual motor All-Wheel Drive with 533km WLTP range on a single charge. Pearl White Multi-Coat with all-black premium interior. Autopilot included (Traffic-Aware Cruise, Autosteer). Full Self-Driving capability (Basic) — all over-the-air updates applied. 15.4\" touchscreen, premium audio (14 speakers, 1 subwoofer, 2 amps), heated front & rear seats, panoramic glass roof. Charges at any Tesla Supercharger (0–80% in approx. 25 min at V3 Supercharger). Home charging cable included. One owner, garaged, never used in off-road conditions. Remaining Tesla 4-year/80,000km warranty transferable.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1619767886558-efdc259cde1a?w=1200&q=85",
                    "https://images.unsplash.com/photo-1560958089-b8a1929cea89?w=1200&q=85",
                    "https://images.unsplash.com/photo-1593941707882-a5bba14938c7?w=1200&q=85"
                ),
                sellerUid = "demo",
                createdAt = now - 7_200_000,
                expiresAt = now + 30L * 24 * 60 * 60 * 1000 - 7_200_000
            )
        )

        dummyCars.forEach { car -> carsCollection.add(car).await() }
    }
}
