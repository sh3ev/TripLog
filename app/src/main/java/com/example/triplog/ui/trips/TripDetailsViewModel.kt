package com.example.triplog.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triplog.config.ApiConfig
import com.example.triplog.data.AppDatabase
import com.example.triplog.network.RetrofitInstance
import com.example.triplog.network.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripDetailsViewModel(private val database: AppDatabase) : ViewModel() {
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState

    fun fetchWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                val apiKey = ApiConfig.OPENWEATHER_API_KEY
                
                if (apiKey == "YOUR_API_KEY_HERE" || apiKey.isEmpty()) {
                    _weatherState.value = WeatherState.Error(
                        "Brak klucza API. Dodaj OPENWEATHER_API_KEY=twój_klucz do pliku local.properties"
                    )
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.weatherApi.getWeather(latitude, longitude, apiKey)
                }
                
                val weatherSummary = "${response.main.temp}°C, ${response.weather.firstOrNull()?.description ?: "brak opisu"}"
                _weatherState.value = WeatherState.Success(weatherSummary, response)
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error("Błąd pobierania pogody: ${e.message}")
            }
        }
    }

    suspend fun updateTripWeather(tripId: Long, weatherSummary: String) {
        withContext(Dispatchers.IO) {
            val trip = database.tripDao().getTripById(tripId)
            trip?.let {
                database.tripDao().updateTrip(it.copy(weatherSummary = weatherSummary))
            }
        }
    }

    suspend fun deleteTrip(tripId: Long) {
        withContext(Dispatchers.IO) {
            val trip = database.tripDao().getTripById(tripId)
            trip?.let {
                database.tripDao().deleteTrip(it)
            }
        }
    }
}

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val summary: String, val response: WeatherResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

