package az.kompressor.app.domain.repository

import android.net.Uri
import az.kompressor.app.domain.model.Car
import az.kompressor.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getCars(): Flow<Resource<List<Car>>>
    fun searchCars(query: String): Flow<Resource<List<Car>>>
    fun getCarById(carId: String): Flow<Resource<Car>>
    fun postCar(car: Car, imageUris: List<Uri>): Flow<Resource<Unit>>
    fun getCarsByUser(uid: String): Flow<Resource<List<Car>>>
    fun deleteCarById(carId: String): Flow<Resource<Unit>>
    fun updateCar(car: Car, newImageUris: List<Uri>, existingImageUrls: List<String>): Flow<Resource<Unit>>
    suspend fun incrementViewCount(carId: String)
    suspend fun bumpCar(carId: String)
    fun getAllCarsAdmin(): Flow<Resource<List<Car>>>
    suspend fun updateCarStatus(carId: String, status: String, sellerUid: String, carTitle: String)
    suspend fun isAdmin(uid: String): Boolean
}
