package com.example.triplog.network

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String
)

data class Main(
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double
)

data class Weather(
    val main: String,
    val description: String
)

