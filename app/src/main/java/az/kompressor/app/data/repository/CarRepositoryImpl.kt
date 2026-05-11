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
        // Check if demo data is already at target count — skip if so
        val existing = carsCollection.whereEqualTo("sellerUid", "demo").get().await()
        if (existing.size() >= 14) return

        // Wipe any old/stale demo entries before re-seeding
        existing.documents.forEach { it.reference.delete().await() }

        val now = System.currentTimeMillis()
        val dummyCars = listOf(

            // ── 1. BMW 3 Series — Petrol / Automatic / Baku ──────────────────
            CarDto(
                title = "BMW 3 Series 320i",
                brand = "BMW", model = "3 Series", year = 2022,
                price = 62000, mileage = 21000,
                fuelType = "Petrol", transmission = "Automatic",
                city = "Baku", phone = "+994501112233",
                description = "M Sport package, black leather interior, ambient lighting, wireless CarPlay. One owner, no accidents, full dealer service history.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=800",
                    "https://images.unsplash.com/photo-1520050206274-a1ae44613e6d?w=800"
                ),
                sellerUid = "demo", createdAt = now
            ),

            // ── 2. Tesla Model 3 — Electric / Automatic / Baku ──────────────
            CarDto(
                title = "Tesla Model 3 Long Range",
                brand = "Tesla", model = "Model 3", year = 2023,
                price = 89000, mileage = 9500,
                fuelType = "Electric", transmission = "Automatic",
                city = "Baku", phone = "+994552223344",
                description = "Dual motor AWD, 576 km range, Autopilot, 15\" touchscreen, over-the-air updates. White interior, premium sound system. Never in accident.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1560958089-b8a1929cea89?w=800",
                    "https://images.unsplash.com/photo-1554744512-d6c603f27c54?w=800"
                ),
                sellerUid = "demo", createdAt = now - 60_000
            ),

            // ── 3. Mercedes-Benz C200 — Petrol / Automatic / Baku ───────────
            CarDto(
                title = "Mercedes-Benz C200 AMG Line",
                brand = "Mercedes", model = "C200", year = 2021,
                price = 71000, mileage = 34000,
                fuelType = "Petrol", transmission = "Automatic",
                city = "Baku", phone = "+994703334455",
                description = "AMG styling pack, panoramic sunroof, Burmester audio, MBUX infotainment. Maintained at official Mercedes service centre every 10k km.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=800",
                    "https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=800"
                ),
                sellerUid = "demo", createdAt = now - 120_000
            ),

            // ── 4. Toyota RAV4 — Hybrid / Automatic / Ganja ─────────────────
            CarDto(
                title = "Toyota RAV4 Hybrid",
                brand = "Toyota", model = "RAV4", year = 2023,
                price = 54000, mileage = 12000,
                fuelType = "Hybrid", transmission = "Automatic",
                city = "Ganja", phone = "+994514445566",
                description = "2.5L hybrid AWD, 8-inch touchscreen, Toyota Safety Sense, heated seats. Fuel consumption only 6.0L/100km. Under full Toyota warranty until 2026.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?w=800",
                    "https://images.unsplash.com/photo-1581540222194-0def2dda95b8?w=800"
                ),
                sellerUid = "demo", createdAt = now - 180_000
            ),

            // ── 5. Audi Q5 — Diesel / Automatic / Baku ──────────────────────
            CarDto(
                title = "Audi Q5 40 TDI Quattro",
                brand = "Audi", model = "Q5", year = 2020,
                price = 67000, mileage = 52000,
                fuelType = "Diesel", transmission = "Automatic",
                city = "Baku", phone = "+994705556677",
                description = "Quattro permanent AWD, S-line exterior, virtual cockpit, matrix LED headlights. Regularly serviced at Audi Centre Baku. 2 keys.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6?w=800",
                    "https://images.unsplash.com/photo-1547744152-14d985cb937f?w=800"
                ),
                sellerUid = "demo", createdAt = now - 240_000
            ),

            // ── 6. Range Rover Sport — Petrol / Automatic / Baku ────────────
            CarDto(
                title = "Range Rover Sport HSE",
                brand = "Land Rover", model = "Range Rover Sport", year = 2021,
                price = 118000, mileage = 38000,
                fuelType = "Petrol", transmission = "Automatic",
                city = "Baku", phone = "+994776667788",
                description = "3.0L 6-cylinder, 7-seat configuration, air suspension, Meridian sound, 360° camera, off-road terrain management. Immaculate condition.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?w=800",
                    "https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?w=800"
                ),
                sellerUid = "demo", createdAt = now - 300_000
            ),

            // ── 7. Kia Sportage — LPG / Manual / Sumqayıt ───────────────────
            CarDto(
                title = "Kia Sportage 2.0 LPG",
                brand = "Kia", model = "Sportage", year = 2019,
                price = 22000, mileage = 78000,
                fuelType = "LPG", transmission = "Manual",
                city = "Sumqayıt", phone = "+994507778899",
                description = "Factory LPG system, very economical to run (~0.25 AZN/km). Cruise control, rear camera, heated front seats. Minor scuff on rear bumper, priced accordingly.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1633158829585-23ba8f7c8caf?w=800"
                ),
                sellerUid = "demo", createdAt = now - 360_000
            ),

            // ── 8. Volkswagen Tiguan — Diesel / Automatic / Baku ────────────
            CarDto(
                title = "Volkswagen Tiguan 2.0 TDI",
                brand = "Volkswagen", model = "Tiguan", year = 2020,
                price = 41000, mileage = 61000,
                fuelType = "Diesel", transmission = "Automatic",
                city = "Baku", phone = "+994518889900",
                description = "R-Line package, panoramic roof, DSG gearbox, active lane assist, park assist. Diesel — excellent for long-distance driving. Full VW service history.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1471444928139-48c5bf5173f8?w=800",
                    "https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?w=800"
                ),
                sellerUid = "demo", createdAt = now - 420_000
            ),

            // ── 9. Porsche Macan — Petrol / Automatic / Baku ────────────────
            CarDto(
                title = "Porsche Macan S",
                brand = "Porsche", model = "Macan", year = 2022,
                price = 134000, mileage = 15000,
                fuelType = "Petrol", transmission = "Automatic",
                city = "Baku", phone = "+994709990011",
                description = "2.9L twin-turbo V6 380hp, Sport Chrono package, PASM air suspension, BOSE sound, 21\" wheels. Full Porsche warranty remaining. Only driven on weekends.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800",
                    "https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?w=800"
                ),
                sellerUid = "demo", createdAt = now - 480_000
            ),

            // ── 10. Honda CR-V — Hybrid / Automatic / Mingəçevir ────────────
            CarDto(
                title = "Honda CR-V e:HEV Hybrid",
                brand = "Honda", model = "CR-V", year = 2022,
                price = 47000, mileage = 27000,
                fuelType = "Hybrid", transmission = "Automatic",
                city = "Mingəçevir", phone = "+994500011223",
                description = "Self-charging hybrid, Honda Sensing safety suite, 9\" touchscreen with wireless Apple CarPlay. Fuel avg 5.8L/100km. Purchased new in Azerbaijan.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1606152421802-db97b9c7a11b?w=800"
                ),
                sellerUid = "demo", createdAt = now - 540_000
            ),

            // ── 11. Chevrolet Malibu — Petrol / Automatic / Lənkəran ────────
            CarDto(
                title = "Chevrolet Malibu 1.5T",
                brand = "Chevrolet", model = "Malibu", year = 2020,
                price = 19500, mileage = 83000,
                fuelType = "Petrol", transmission = "Automatic",
                city = "Lənkəran", phone = "+994551122334",
                description = "Turbo 1.5L, automatic transmission, original paint, no frame damage. MyLink infotainment, rear camera. Ideal city/highway car at a budget price.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=800"
                ),
                sellerUid = "demo", createdAt = now - 600_000
            ),

            // ── 12. Nissan Qashqai — Petrol / Manual / Şirvan ───────────────
            CarDto(
                title = "Nissan Qashqai 1.6 Acenta",
                brand = "Nissan", model = "Qashqai", year = 2018,
                price = 17000, mileage = 94000,
                fuelType = "Petrol", transmission = "Manual",
                city = "Şirvan", phone = "+994702233445",
                description = "1.6L 114hp, ProPilot cruise, Around View Monitor, heated steering wheel. New tyres fitted 5,000km ago. Belt replaced at 80k. Honest seller — viewing welcome.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1609521263047-f8f205293f24?w=800"
                ),
                sellerUid = "demo", createdAt = now - 660_000
            ),

            // ── 13. Lexus RX 350 — Petrol / Automatic / Baku ────────────────
            CarDto(
                title = "Lexus RX 350 F-Sport",
                brand = "Lexus", model = "RX 350", year = 2021,
                price = 82000, mileage = 29000,
                fuelType = "Petrol", transmission = "Automatic",
                city = "Baku", phone = "+994773344556",
                description = "3.5L V6 302hp, F-Sport with adaptive variable suspension, Mark Levinson 15-speaker audio, triple-beam LED headlights. Platinum white pearl. One owner.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1502877338535-766e1452684a?w=800",
                    "https://images.unsplash.com/photo-1567818735868-e71b99932e29?w=800"
                ),
                sellerUid = "demo", createdAt = now - 720_000
            ),

            // ── 14. Škoda Octavia — Diesel / Semi-Automatic / Sumqayıt ──────
            CarDto(
                title = "Škoda Octavia 2.0 TDI DSG",
                brand = "Škoda", model = "Octavia", year = 2019,
                price = 24000, mileage = 71000,
                fuelType = "Diesel", transmission = "Semi-Automatic",
                city = "Sumqayıt", phone = "+994504455667",
                description = "2.0 TDI 150hp with DSG automatic gearbox, virtual cockpit, Canton audio, heated seats, front & rear parking sensors. Diesel averages 5.2L/100km on highway.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1617814076668-8b1a8ac3c58d?w=800",
                    "https://images.unsplash.com/photo-1542282088-fe8426682b8f?w=800"
                ),
                sellerUid = "demo", createdAt = now - 780_000
            ),

            // ── 15. Hyundai Ioniq 6 — Electric / Automatic / Baku ───────────
            CarDto(
                title = "Hyundai Ioniq 6 Standard Range",
                brand = "Hyundai", model = "Ioniq 6", year = 2024,
                price = 74000, mileage = 3200,
                fuelType = "Electric", transmission = "Automatic",
                city = "Baku", phone = "+994515566778",
                description = "Brand new — only 3,200km. 400V ultra-fast charging (10–80% in 18 min), 429km WLTP range, digital side mirrors, 12\" dual screen, over-the-air updates. Still under full warranty.",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1593941707882-a5bba14938c7?w=800",
                    "https://images.unsplash.com/photo-1565043666747-69f6646db940?w=800"
                ),
                sellerUid = "demo", createdAt = now - 840_000
            )
        )

        dummyCars.forEach { car -> carsCollection.add(car).await() }
    }
}
