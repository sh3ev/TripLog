package com.example.triplog.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private const val PHOTON_BASE_URL = "https://photon.komoot.io/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val weatherRetrofit = Retrofit.Builder()
        .baseUrl(WEATHER_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val photonRetrofit = Retrofit.Builder()
        .baseUrl(PHOTON_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherApi: WeatherApi = weatherRetrofit.create(WeatherApi::class.java)
    val photonApi: PhotonApi = photonRetrofit.create(PhotonApi::class.java)
}

