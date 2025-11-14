# TripLog - Dziennik Podróży z Galerią i Pogodą

Aplikacja Android do zarządzania podróżami z funkcjami galerii zdjęć i pobierania aktualnej pogody.

## Funkcje

- ✅ Logowanie i rejestracja użytkowników (SHA-256 hash hasła)
- ✅ Lista podróży z miniaturami zdjęć
- ✅ Dodawanie/edycja podróży z wyborem zdjęć z galerii
- ✅ Pobieranie aktualnej lokalizacji (GPS)
- ✅ Pobieranie aktualnej pogody z OpenWeather API
- ✅ Wyszukiwanie podróży
- ✅ Wysyłanie emaili z danymi podróży

## Technologie

- **Kotlin** - język programowania
- **Room (SQLite)** - lokalna baza danych
- **Retrofit** - komunikacja z API
- **Coroutines** - asynchroniczne operacje
- **FusedLocationProviderClient** - lokalizacja GPS
- **RecyclerView** - lista podróży
- **ViewBinding** - binding widoków

## Konfiguracja

### 1. Klucz API OpenWeather

Aby aplikacja mogła pobierać dane pogodowe, musisz skonfigurować klucz API:

1. Zarejestruj się na [OpenWeatherMap](https://openweathermap.org/api) i uzyskaj darmowy klucz API
2. Otwórz plik `local.properties` w głównym katalogu projektu
3. Dodaj linię:
   ```
   OPENWEATHER_API_KEY=twój_klucz_api_tutaj
   ```
4. Zastąp `twój_klucz_api_tutaj` swoim rzeczywistym kluczem API

**Uwaga:** Plik `local.properties` jest już w `.gitignore` i nie będzie commitowany do repozytorium.

### 2. Alternatywnie - zmienna środowiskowa

Możesz również ustawić zmienną środowiskową:
```bash
export OPENWEATHER_API_KEY=twój_klucz_api
```

## Instalacja

1. Sklonuj repozytorium
2. Otwórz projekt w Android Studio
3. Skonfiguruj klucz API (patrz wyżej)
4. Zsynchronizuj projekt Gradle
5. Uruchom aplikację

## Struktura projektu

```
app/src/main/java/com/example/triplog/
├── data/
│   ├── entities/          # Encje Room (UserEntity, TripEntity)
│   ├── dao/               # DAO dla operacji na bazie danych
│   └── AppDatabase.kt     # Konfiguracja bazy Room
├── network/
│   ├── WeatherApi.kt      # Interfejs Retrofit dla API pogody
│   ├── RetrofitInstance.kt
│   └── WeatherResponse.kt # Modele odpowiedzi API
├── ui/
│   ├── login/             # Ekran logowania
│   ├── register/          # Ekran rejestracji
│   ├── main/              # Główny ekran z listą podróży
│   └── trips/             # Ekrany związane z podróżami
├── utils/                 # Narzędzia pomocnicze
└── config/                # Konfiguracja (klucze API)
```

## Bezpieczeństwo

⚠️ **WAŻNE:** 
- Klucze API są przechowywane w `local.properties`, który jest w `.gitignore`
- Nigdy nie commituj plików z prawdziwymi kluczami API
- W produkcji rozważ użycie bardziej zaawansowanych metod przechowywania kluczy (np. Android Keystore)

## Licencja

Ten projekt jest przykładem edukacyjnym.

