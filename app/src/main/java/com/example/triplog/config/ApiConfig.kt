package com.example.triplog.config

import com.example.triplog.BuildConfig

/**
 * Konfiguracja kluczy API
 * 
 * Klucz API jest pobierany z BuildConfig, który jest generowany z local.properties
 * 
 * Aby skonfigurować klucz API:
 * 1. Dodaj do pliku local.properties (który jest w .gitignore):
 *    OPENWEATHER_API_KEY=twój_klucz_api
 * 2. Lub ustaw zmienną środowiskową OPENWEATHER_API_KEY
 * 3. Jeśli żadne z powyższych nie jest ustawione, użyty zostanie domyślny "YOUR_API_KEY_HERE"
 */
object ApiConfig {
    val OPENWEATHER_API_KEY: String = BuildConfig.OPENWEATHER_API_KEY
}

