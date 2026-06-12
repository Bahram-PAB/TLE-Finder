package com.example.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CelestrakRepository
import com.example.data.GroundStation
import com.example.data.SatellitePass
import com.example.data.SatellitePassCalculator
import com.example.data.SatelliteTle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface TleUiState {
    object Idle : TleUiState
    object Loading : TleUiState
    data class Success(val satellites: List<SatelliteTle>) : TleUiState
    data class Error(val message: String) : TleUiState
}

class TleViewModel(
    application: Application,
    private val repository: CelestrakRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, CelestrakRepository())

    private val sharedPrefs = application.getSharedPreferences("tle_prefs", Context.MODE_PRIVATE)

    private val _isEnglish = MutableStateFlow(sharedPrefs.getBoolean("is_english", false))
    val isEnglish: StateFlow<Boolean> = _isEnglish.asStateFlow()

    fun toggleLanguage() {
        val newValue = !_isEnglish.value
        sharedPrefs.edit().putBoolean("is_english", newValue).apply()
        _isEnglish.value = newValue
    }

    private val _favorites = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val favorites: StateFlow<List<Pair<String, String>>> = _favorites.asStateFlow()

    // Ground stations state
    private val _groundStations = MutableStateFlow<List<GroundStation>>(emptyList())
    val groundStations: StateFlow<List<GroundStation>> = _groundStations.asStateFlow()

    // Pass prediction state (satellite name/noradId paired with pass prediction result list)
    private val _passResult = MutableStateFlow<Pair<String, List<SatellitePass>>?>(null)
    val passResult: StateFlow<Pair<String, List<SatellitePass>>?> = _passResult.asStateFlow()

    init {
        loadFavorites()
        loadGroundStations()
    }

    private fun loadFavorites() {
        val favSet = sharedPrefs.getStringSet("favorites_set", emptySet()) ?: emptySet()
        val list = favSet.mapNotNull {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                null
            }
        }.sortedBy { it.second }
        _favorites.value = list
    }

    fun isFavorite(noradId: String): Boolean {
        return _favorites.value.any { it.first == noradId }
    }

    fun toggleFavorite(noradId: String, name: String) {
        val current = _favorites.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.first == noradId }

        if (existingIndex != -1) {
            current.removeAt(existingIndex)
        } else {
            current.add(noradId to name)
        }

        val set = current.map { "${it.first}|${it.second}" }.toSet()
        sharedPrefs.edit().putStringSet("favorites_set", set).apply()

        _favorites.value = current.sortedBy { it.second }
    }

    // --- Ground Station Operations ---

    private fun loadGroundStations() {
        val rawSet = sharedPrefs.getStringSet("ground_stations_set", emptySet()) ?: emptySet()
        val stations = rawSet.mapNotNull { deserializeStation(it) }.toMutableList()

        if (stations.isEmpty()) {
            // Seed a default station
            val defaultStation = GroundStation(
                id = UUID.randomUUID().toString(),
                name = "ایستگاه تهران (پیش‌فرض)",
                latitude = 35.6892,
                longitude = 51.3890,
                altitude = 1200.0,
                isPrimary = true
            )
            stations.add(defaultStation)
            saveStationsList(stations)
        }
        _groundStations.value = stations.sortedByDescending { it.isPrimary }
    }

    private fun saveStationsList(list: List<GroundStation>) {
        val serialized = list.map { serializeStation(it) }.toSet()
        sharedPrefs.edit().putStringSet("ground_stations_set", serialized).apply()
        _groundStations.value = list.sortedByDescending { it.isPrimary }
    }

    fun addGroundStation(name: String, lat: Double, lng: Double, alt: Double): Boolean {
        val current = _groundStations.value.toMutableList()
        if (current.size >= 10) {
            return false // Limit reached
        }

        val designator = name.trim().ifEmpty { "ایستگاه جدید" }
        val newStation = GroundStation(
            id = UUID.randomUUID().toString(),
            name = designator,
            latitude = lat,
            longitude = lng,
            altitude = alt,
            isPrimary = current.isEmpty()
        )
        current.add(newStation)
        saveStationsList(current)
        return true
    }

    fun deleteGroundStation(id: String) {
        val current = _groundStations.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val wasPrimary = current[index].isPrimary
            current.removeAt(index)
            if (wasPrimary && current.isNotEmpty()) {
                // Assign first remaining station as primary
                current[0] = current[0].copy(isPrimary = true)
            }
            saveStationsList(current)
        }
    }

    fun setPrimaryGroundStation(id: String) {
        val current = _groundStations.value.map {
            it.copy(isPrimary = (it.id == id))
        }
        saveStationsList(current)
    }

    private fun serializeStation(station: GroundStation): String {
        return "${station.id}::${station.name}::${station.latitude}::${station.longitude}::${station.altitude}::${station.isPrimary}"
    }

    private fun deserializeStation(str: String): GroundStation? {
        return try {
            val parts = str.split("::")
            if (parts.size < 6) return null
            GroundStation(
                id = parts[0],
                name = parts[1],
                latitude = parts[2].toDoubleOrNull() ?: 0.0,
                longitude = parts[3].toDoubleOrNull() ?: 0.0,
                altitude = parts[4].toDoubleOrNull() ?: 0.0,
                isPrimary = parts[5].toBoolean()
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- Pass Calculation ---

    fun calculatePass(tle: SatelliteTle) {
        val activeStation = _groundStations.value.firstOrNull { it.isPrimary } 
            ?: _groundStations.value.firstOrNull()
        
        if (activeStation == null) {
            _passResult.value = tle.name to emptyList()
            return
        }

        val passes = SatellitePassCalculator.calculatePasses(tle, activeStation)
        _passResult.value = tle.name to passes
    }

    fun clearPassResult() {
        _passResult.value = null
    }

    fun getAppVersion(): String {
        return try {
            val packageInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName,0)
            packageInfo.versionName ?: "2.7"
        } catch (e: Exception) {
            "2.7"
        }
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<TleUiState>(TleUiState.Idle)
    val uiState: StateFlow<TleUiState> = _uiState.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    fun searchSatellite() {
        val trimmedQuery = _query.value.trim()
        if (trimmedQuery.isEmpty()) {
            _uiState.value = TleUiState.Error("لطفاً نام یا شناسه NORAD ماهواره را وارد کنید.")
            return
        }

        _uiState.value = TleUiState.Loading

        viewModelScope.launch {
            val isNumber = trimmedQuery.all { it.isDigit() }
            val result = if (isNumber) {
                repository.fetchTleByCatalogNumber(trimmedQuery)
            } else {
                repository.fetchTleByName(trimmedQuery)
            }

            result.onSuccess { list ->
                _uiState.value = TleUiState.Success(list)
            }.onFailure { exception ->
                val friendlyMessage = when {
                    exception.message?.contains("Unable to resolve host") == true ||
                    exception.message?.contains("connect timed out") == true ->
                        "خطا در شبکه. لطفاً اتصال اینترنت خود را چک کنید."
                    else -> exception.message ?: "خطایی در دریافت اطلاعات به وجود آمد."
                }
                _uiState.value = TleUiState.Error(friendlyMessage)
            }
        }
    }

    fun quickSearch(satelliteNameOrId: String) {
        _query.value = satelliteNameOrId
        searchSatellite()
    }
}
