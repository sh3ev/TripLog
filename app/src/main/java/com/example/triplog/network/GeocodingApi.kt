package com.example.triplog.network

import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("direct")
    suspend fun getCoordinates(
        @Query("q") query: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeocodingResponse>
}

data class GeocodingResponse(
    val name: String,
    val local_names: Map<String, String>? = null,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
) {
    fun getDisplayName(): String {
        val localName = local_names?.get("pl") ?: name
        return buildString {
            append(localName)
            state?.let { append(", $it") }
            append(", $country")
        }
    }
}
