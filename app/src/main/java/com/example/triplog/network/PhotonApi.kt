package com.example.triplog.network

import retrofit2.http.GET
import retrofit2.http.Query

interface PhotonApi {
    @GET("api/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("lang") language: String = "en"
    ): PhotonResponse
}

data class PhotonResponse(
    val type: String,
    val features: List<PhotonFeature>
)

data class PhotonFeature(
    val type: String,
    val properties: PhotonProperties,
    val geometry: PhotonGeometry
)

data class PhotonProperties(
    val osm_type: String? = null,
    val osm_id: Long? = null,
    val osm_key: String? = null,
    val osm_value: String? = null,
    val type: String? = null,
    val countrycode: String? = null,
    val name: String? = null,
    val country: String? = null,
    val state: String? = null,
    val county: String? = null,
    val city: String? = null,
    val street: String? = null,
    val housenumber: String? = null
) {
    fun getDisplayName(): String {
        return name ?: city ?: county ?: state ?: country ?: ""
    }
    
    fun getFullDisplayName(): String {
        val parts = mutableListOf<String>()
        name?.let { parts.add(it) }
        if (city != null && city != name) parts.add(city)
        state?.let { parts.add(it) }
        getPolishCountryName()?.let { parts.add(it) }
        return parts.joinToString(", ")
    }
    
    fun getPolishCountryName(): String? {
        return when (countrycode?.uppercase()) {
            "PL" -> "Polska"
            "DE" -> "Niemcy"
            "FR" -> "Francja"
            "ES" -> "Hiszpania"
            "IT" -> "Włochy"
            "GB", "UK" -> "Wielka Brytania"
            "US" -> "Stany Zjednoczone"
            "CZ" -> "Czechy"
            "SK" -> "Słowacja"
            "UA" -> "Ukraina"
            "AT" -> "Austria"
            "CH" -> "Szwajcaria"
            "NL" -> "Holandia"
            "BE" -> "Belgia"
            "PT" -> "Portugalia"
            "GR" -> "Grecja"
            "HR" -> "Chorwacja"
            "HU" -> "Węgry"
            "SE" -> "Szwecja"
            "NO" -> "Norwegia"
            "DK" -> "Dania"
            "FI" -> "Finlandia"
            "IE" -> "Irlandia"
            "RU" -> "Rosja"
            "JP" -> "Japonia"
            "CN" -> "Chiny"
            "AU" -> "Australia"
            "CA" -> "Kanada"
            "MX" -> "Meksyk"
            "BR" -> "Brazylia"
            "AR" -> "Argentyna"
            "EG" -> "Egipt"
            "TR" -> "Turcja"
            "TH" -> "Tajlandia"
            "IN" -> "Indie"
            "KR" -> "Korea Południowa"
            else -> country
        }
    }
}

data class PhotonGeometry(
    val type: String,
    val coordinates: List<Double> // [lon, lat]
) {
    fun getLat(): Double = coordinates.getOrNull(1) ?: 0.0
    fun getLon(): Double = coordinates.getOrNull(0) ?: 0.0
}
