package az.kompressor.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.kompressor.app.domain.model.Car
import az.kompressor.app.domain.repository.CarRepository
import az.kompressor.app.domain.usecase.GetCarsUseCase
import az.kompressor.app.domain.usecase.SearchCarsUseCase
import az.kompressor.app.util.Resource
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
    val fuelType: String = "",       // "" = any
    val transmission: String = "",   // "" = any
    val city: String = "",           // "" = any
    val sortBy: String = "newest"    // "newest" | "price_asc" | "price_desc"
) {
    val isActive: Boolean
        get() = fuelType.isNotBlank() || transmission.isNotBlank() || city.isNotBlank() || sortBy != "newest"

    /** Human-readable summary for the chip label */
    fun label(): String {
        val parts = mutableListOf<String>()
        if (fuelType.isNotBlank()) parts.add(fuelType)
        if (transmission.isNotBlank()) parts.add(transmission)
        if (city.isNotBlank()) parts.add(city)
        when (sortBy) {
            "price_asc" -> parts.add("↑ Price")
            "price_desc" -> parts.add("↓ Price")
        }
        return parts.joinToString(" · ")
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCarsUseCase: GetCarsUseCase,
    private val searchCarsUseCase: SearchCarsUseCase,
    private val carRepository: CarRepository
) : ViewModel() {

    // Raw data from Firestore
    private var allCars: List<Car> = emptyList()

    // Current search query
    private var currentQuery: String = ""

    // Exposed state
    private val _carsState = MutableStateFlow<Resource<List<Car>>>(Resource.Loading)
    val carsState: StateFlow<Resource<List<Car>>> = _carsState

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    init {
        seedAndLoad()
    }

    private fun seedAndLoad() {
        viewModelScope.launch {
            // Only seed once per install — SharedPrefs flag prevents wiping
            // favourited car IDs on every restart.
            val prefs = context.getSharedPreferences("kompressor_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("dummy_seeded", false)) {
                carRepository.seedDummyData()
                prefs.edit().putBoolean("dummy_seeded", true).apply()
            }
            loadCars()
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

    fun search(query: String) {
        currentQuery = query
        if (query.isBlank()) {
            _carsState.value = Resource.Success(applyFilter(allCars))
            return
        }
        val q = query.lowercase()
        val filtered = allCars.filter { car ->
            car.brand.lowercase().contains(q) ||
            car.model.lowercase().contains(q) ||
            car.city.lowercase().contains(q) ||
            car.title.lowercase().contains(q)
        }
        _carsState.value = Resource.Success(applyFilter(filtered))
    }

    fun applyFilter(filter: FilterState) {
        _filterState.value = filter
        reapply()
    }

    fun clearFilter() {
        _filterState.value = FilterState()
        reapply()
    }

    private fun reapply() {
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

    /** Apply current FilterState to a list and return the result */
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
            else         -> result.sortedByDescending { it.createdAt } // newest
        }

        return result
    }
}
