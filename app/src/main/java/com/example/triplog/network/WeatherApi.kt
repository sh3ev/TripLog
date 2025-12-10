package com.example.triplog.network

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pl"
    ): WeatherResponse
    
    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "pl"
    ): ForecastResponse
}

// Odpowiedź dla prognozy 5-dniowej
data class ForecastResponse(
    val list: List<ForecastItem>,
    val city: ForecastCity
)

data class ForecastItem(
    val dt: Long, // Unix timestamp
    val main: ForecastMain,
    val weather: List<ForecastWeather>,
    val dt_txt: String // "2024-12-11 12:00:00"
)

data class ForecastMain(
    val temp: Double,
    val temp_min: Double,
    val temp_max: Double,
    val humidity: Int
)

data class ForecastWeather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class ForecastCity(
    val name: String,
    val country: String
)

