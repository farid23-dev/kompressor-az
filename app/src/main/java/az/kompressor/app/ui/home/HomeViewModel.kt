package az.kompressor.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.domain.repository.NotificationRepository
import az.kompressor.app.domain.usecase.GetCarsUseCase
import az.kompressor.app.domain.usecase.SearchCarsUseCase
import az.kompressor.app.domain.usecase.GetFavoritesUseCase
import az.kompressor.app.domain.usecase.ToggleFavoriteUseCase
import az.kompressor.app.util.Resource
import az.kompressor.app.R
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterState(
    val fuelType: String = "",
    val transmission: String = "",
    val city: String = "",
    val sortBy: String = "newest"
) {
    val isActive: Boolean
        get() = fuelType.isNotBlank() || transmission.isNotBlank() || city.isNotBlank() || sortBy != "newest"

    fun label(context: Context): String {
        val parts = mutableListOf<String>()
        if (fuelType.isNotBlank()) parts.add(fuelType)
        if (transmission.isNotBlank()) parts.add(transmission)
        if (city.isNotBlank()) parts.add(city)
        when (sortBy) {
            "price_asc" -> parts.add(context.getString(R.string.filter_price_asc))
            "price_desc" -> parts.add(context.getString(R.string.filter_price_desc))
        }
        return parts.joinToString(" · ")
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCarsUseCase: GetCarsUseCase,
    private val carRepository: CarRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private var allCars: List<Car> = emptyList()

    private var currentQuery: String = ""

    private val _carsState = MutableStateFlow<Resource<List<Car>>>(Resource.Loading)
    val carsState: StateFlow<Resource<List<Car>>> = _carsState

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    init {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid
            if (uid != null) {
                _isAdmin.value = carRepository.isAdmin(uid)
                notificationRepository.getNotifications(uid)
                    .onEach { list -> _unreadCount.value = list.count { !it.read } }
                    .launchIn(viewModelScope)
            }
            loadCars()
            observeFavorites()
        }
    }

    private fun observeFavorites() {
        getFavoritesUseCase().onEach { list ->
            _favoriteIds.value = list.map { it.id }.toSet()
        }.launchIn(viewModelScope)
    }

    fun toggleFavorite(car: Car) {
        viewModelScope.launch {
            if (_favoriteIds.value.contains(car.id)) {
                toggleFavoriteUseCase.remove(car.id)
            } else {
                toggleFavoriteUseCase.add(car)
            }
        }
    }

    fun loadCars() {
        currentQuery = ""
        getCarsUseCase().onEach { resource ->
            if (resource is Resource.Success) {
                allCars = resource.data
                _carsState.value = Resource.Success(applyFilter(allCars))
            } else {
                _carsState.value = resource
            }
        }.launchIn(viewModelScope)
    }

    fun applyFilter(filter: FilterState) {
        _filterState.value = filter
        reApply()
    }

    fun clearFilter() {
        _filterState.value = FilterState()
        reApply()
    }

    private fun reApply() {
        val base = if (currentQuery.isBlank()) allCars else {
            val q = currentQuery.lowercase()
            allCars.filter { car ->
                car.brand.lowercase().contains(q) ||
                car.model.lowercase().contains(q) ||
                car.city.lowercase().contains(q) ||
                car.title.lowercase().contains(q)
            }
        }
        _carsState.value = Resource.Success(applyFilter(base))
    }

    private fun applyFilter(cars: List<Car>): List<Car> {
        val f = _filterState.value
        var result = cars

        if (f.fuelType.isNotBlank()) {
            result = result.filter { it.fuelType.equals(f.fuelType, ignoreCase = true) }
        }
        if (f.transmission.isNotBlank()) {
            result = result.filter { it.transmission.equals(f.transmission, ignoreCase = true) }
        }
        if (f.city.isNotBlank()) {
            result = result.filter { it.city.lowercase().contains(f.city.lowercase()) }
        }

        result = when (f.sortBy) {
            "price_asc"  -> result.sortedBy { it.price }
            "price_desc" -> result.sortedByDescending { it.price }
            else         -> result.sortedByDescending { it.createdAt }
        }

        return result
    }
}
