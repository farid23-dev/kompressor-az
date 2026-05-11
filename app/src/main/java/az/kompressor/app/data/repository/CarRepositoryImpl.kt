package az.kompressor.app.data.repository

import android.net.Uri
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
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : CarRepository {

    private val carsCollection = firestore.collection("cars")

    override fun getCars(): Flow<Resource<List<Car>>> = flow {
        emit(Resource.Loading)
        try {
            val snapshot = carsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val cars = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CarDto::class.java)?.copy(id = doc.id)?.toDomain()
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

    override fun postCar(car: Car, imageUris: List<Uri>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            // 1. Upload images to Firebase Storage
            val imageUrls = imageUris.map { uri ->
                val ref = storage.reference.child("cars/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString()
            }

            // 2. Save car document to Firestore
            val carDto = CarDto(
                title = car.title,
                brand = car.brand,
                model = car.model,
                year = car.year,
                price = car.price,
                mileage = car.mileage,
                fuelType = car.fuelType,
                transmission = car.transmission,
                city = car.city,
                imageUrls = imageUrls,
                sellerUid = car.sellerUid,
                createdAt = System.currentTimeMillis()
            )
            carsCollection.add(carDto).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to post car"))
        }
    }

    override suspend fun seedDummyData() {
        val snapshot = carsCollection.limit(1).get().await()
        if (!snapshot.isEmpty) return

        val dummyCars = listOf(
            CarDto(title = "BMW 5 Series", brand = "BMW", model = "5 Series", year = 2021, price = 45000, mileage = 32000, fuelType = "Petrol", transmission = "Automatic", city = "Baku", imageUrls = listOf("https://images.unsplash.com/photo-1555215695-3004980ad54e?w=800"), sellerUid = "demo", createdAt = System.currentTimeMillis()),
            CarDto(title = "Mercedes E-Class", brand = "Mercedes", model = "E-Class", year = 2020, price = 52000, mileage = 45000, fuelType = "Diesel", transmission = "Automatic", city = "Baku", imageUrls = listOf("https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=800"), sellerUid = "demo", createdAt = System.currentTimeMillis() - 1000),
            CarDto(title = "Toyota Camry", brand = "Toyota", model = "Camry", year = 2022, price = 38000, mileage = 18000, fuelType = "Hybrid", transmission = "Automatic", city = "Ganja", imageUrls = listOf("https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?w=800"), sellerUid = "demo", createdAt = System.currentTimeMillis() - 2000),
            CarDto(title = "Volkswagen Passat", brand = "Volkswagen", model = "Passat", year = 2019, price = 28000, mileage = 67000, fuelType = "Petrol", transmission = "Manual", city = "Sumqayit", imageUrls = listOf("https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?w=800"), sellerUid = "demo", createdAt = System.currentTimeMillis() - 3000),
            CarDto(title = "Audi A6", brand = "Audi", model = "A6", year = 2020, price = 49000, mileage = 41000, fuelType = "Petrol", transmission = "Automatic", city = "Baku", imageUrls = listOf("https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6?w=800"), sellerUid = "demo", createdAt = System.currentTimeMillis() - 4000),
            CarDto(title = "Hyundai Tucson", brand = "Hyundai", model = "Tucson", year = 2023, price = 35000, mileage = 8000, fuelType = "Petrol", transmission = "Automatic", city = "Baku", imageUrls = listOf("https://images.unsplash.com/photo-1633158829585-23ba8f7c8caf?w=800"), sellerUid = "demo", createdAt = System.currentTimeMillis() - 5000)
        )
        dummyCars.forEach { car -> carsCollection.add(car).await() }
    }
}
