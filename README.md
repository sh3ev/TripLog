# TripLog - Dziennik Podróży z Galerią i Pogodą

Aplikacja Android do zarządzania podróżami z funkcjami galerii zdjęć, map i pobierania aktualnej pogody.

## Funkcje

- ✅ Logowanie i rejestracja użytkowników (SHA-256 hash hasła)
- ✅ Lista podróży z miniaturami pierwszego zdjęcia
- ✅ **Dodawanie wielu zdjęć do każdej podróży** - możliwość wyboru wielu zdjęć naraz
- ✅ **Galeria zdjęć w szczegółach podróży** - poziome przewijanie między zdjęciami
- ✅ **Pełnoekranowy widok zdjęć** - kliknięcie na zdjęcie otwiera pełnoekranowy widok z możliwością przełączania między zdjęciami
- ✅ Dodawanie/edycja podróży z możliwością usuwania pojedynczych zdjęć
- ✅ Pobieranie aktualnej lokalizacji (GPS)
- ✅ **Mapa z zaznaczoną lokalizacją** - interaktywna mapa OpenStreetMap w szczegółach podróży
- ✅ Pobieranie aktualnej pogody z OpenWeather API
- ✅ **Wyszukiwanie podróży** - wyszukiwanie w czasie rzeczywistym po tytule i opisie
- ✅ Menu użytkownika (wylogowanie) dostępne po kliknięciu w avatar

## Technologie

- **Kotlin** - język programowania
- **Room (SQLite)** - lokalna baza danych z migracjami
- **Retrofit** - komunikacja z API
- **Coroutines** - asynchroniczne operacje
- **Flow** - reaktywne strumienie danych
- **FusedLocationProviderClient** - lokalizacja GPS
- **RecyclerView** - lista podróży i galeria zdjęć
- **ViewPager2** - pełnoekranowy widok zdjęć
- **WebView** - wyświetlanie map OpenStreetMap (Leaflet)
- **ViewBinding** - binding widoków
- **Material Design** - komponenty UI

## Konfiguracja

### 1. Klucz API OpenWeather

Aby aplikacja mogła pobierać dane pogodowe, musisz skonfigurować klucz API:

1. Zarejestruj się na [OpenWeatherMap](https://openweathermap.org/api) i uzyskaj darmowy klucz API
2. Otwórz plik `local.properties` w **głównym katalogu projektu** (obok `build.gradle.kts`)
3. Dodaj linię:
   ```properties
   OPENWEATHER_API_KEY=twój_klucz_api_tutaj
   ```
4. Zastąp `twój_klucz_api_tutaj` swoim rzeczywistym kluczem API
5. **Zrób Clean & Rebuild projektu** w Android Studio (Build → Clean Project, potem Build → Rebuild Project)

**Uwaga:** 
- Plik `local.properties` jest już w `.gitignore` i nie będzie commitowany do repozytorium
- Klucz API jest automatycznie wczytywany podczas budowania aplikacji przez `build.gradle.kts`

### 2. Alternatywnie - zmienna środowiskowa

Możesz również ustawić zmienną środowiskową:
```bash
export OPENWEATHER_API_KEY=twój_klucz_api
```

**Uwaga:** Jeśli klucz nie zostanie znaleziony w `local.properties` ani w zmiennych środowiskowych, aplikacja użyje domyślnej wartości `YOUR_API_KEY_HERE` i wyświetli odpowiedni komunikat błędu.

## Instalacja

1. Sklonuj repozytorium
2. Otwórz projekt w Android Studio
3. Skonfiguruj klucz API (patrz wyżej)
4. Zsynchronizuj projekt Gradle (File → Sync Project with Gradle Files)
5. Zrób Clean & Rebuild projektu (Build → Clean Project, potem Build → Rebuild Project)
6. Uruchom aplikację

## Używanie aplikacji

### Dodawanie podróży z wieloma zdjęciami

1. Kliknij przycisk "Dodaj podróż" na ekranie głównym
2. Wypełnij formularz (tytuł, opis, data)
3. Kliknij "Dodaj zdjęcia" - możesz wybrać wiele zdjęć naraz
4. Wybrane zdjęcia pojawią się jako miniatury - możesz usunąć każde klikając X
5. Opcjonalnie: pobierz lokalizację GPS
6. Zapisz podróż

### Przeglądanie zdjęć

- **W szczegółach podróży:** Zdjęcia wyświetlają się w poziomej galerii - możesz przewijać w lewo/prawo
- **Pełnoekranowy widok:** Kliknij na dowolne zdjęcie, aby otworzyć pełnoekranowy widok
- **Przełączanie zdjęć:** W pełnoekranowym widoku przesuń palcem w lewo/prawo, aby przełączać między zdjęciami

### Wyszukiwanie podróży

- Wpisz tekst w pole "Wyszukaj podróż" na ekranie głównym
- Lista automatycznie filtruje się w czasie rzeczywistym
- Wyszukiwanie obejmuje tytuł i opis podróży
- Kliknij X, aby wyczyścić wyszukiwanie

### Mapa lokalizacji

- Jeśli podróż ma zapisaną lokalizację GPS, w szczegółach podróży zobaczysz interaktywną mapę
- Mapa pokazuje dokładną lokalizację podróży z markerem
- Możesz przybliżać i oddalać mapę

## Struktura projektu

```
app/src/main/java/com/example/triplog/
├── data/
│   ├── entities/          # Encje Room (UserEntity, TripEntity, TripImageEntity)
│   ├── dao/               # DAO dla operacji na bazie danych
│   │   ├── UserDao.kt
│   │   ├── TripDao.kt
│   │   └── TripImageDao.kt
│   └── AppDatabase.kt     # Konfiguracja bazy Room z migracjami
├── network/
│   ├── WeatherApi.kt      # Interfejs Retrofit dla API pogody
│   ├── RetrofitInstance.kt
│   └── WeatherResponse.kt # Modele odpowiedzi API
├── ui/
│   ├── login/             # Ekran logowania
│   ├── register/          # Ekran rejestracji
│   ├── main/              # Główny ekran z listą podróży i wyszukiwaniem
│   │   └── MainActivity.kt
│   └── trips/             # Ekrany związane z podróżami
│       ├── AddTripActivity.kt      # Dodawanie/edycja podróży z wieloma zdjęciami
│       ├── TripDetailsActivity.kt  # Szczegóły podróży z galerią, mapą i pogodą
│       ├── FullscreenImageActivity.kt # Pełnoekranowy widok zdjęć
│       ├── TripAdapter.kt          # Adapter dla listy podróży
│       ├── ImageGalleryAdapter.kt  # Adapter dla galerii zdjęć
│       ├── SelectedImageAdapter.kt # Adapter dla wybranych zdjęć podczas dodawania
│       └── FullscreenImageAdapter.kt # Adapter dla ViewPager2
├── utils/                 # Narzędzia pomocnicze
│   ├── SharedPreferencesHelper.kt
│   └── PasswordHasher.kt
└── config/                # Konfiguracja (klucze API)
    └── ApiConfig.kt
```

## Baza danych

Aplikacja używa Room Database z następującymi tabelami:

- **users** - dane użytkowników (email, hasło, imię)
- **trips** - podstawowe informacje o podróżach (tytuł, opis, data, lokalizacja, pogoda)
- **trip_images** - zdjęcia podróży (relacja 1:N z trips, z indeksem kolejności)

Baza danych automatycznie migruje z wersji 1 do wersji 2 przy pierwszym uruchomieniu po aktualizacji.

## Bezpieczeństwo

⚠️ **WAŻNE:** 
- Klucze API są przechowywane w `local.properties`, który jest w `.gitignore`
- Nigdy nie commituj plików z prawdziwymi kluczami API
- Hasła użytkowników są hashowane przy użyciu SHA-256 przed zapisaniem w bazie danych
- W produkcji rozważ użycie bardziej zaawansowanych metod przechowywania kluczy (np. Android Keystore)
- Zdjęcia są przechowywane lokalnie w katalogu aplikacji

## Wymagania

- Android Studio Hedgehog | 2023.1.1 lub nowszy
- Android SDK 24 (Android 7.0) lub nowszy
- Kotlin 1.9+
- Gradle 8.0+

## Licencja

Ten projekt jest przykładem edukacyjnym.

## Autor

Projekt stworzony jako przykład aplikacji Android z wykorzystaniem Room, Retrofit i innych nowoczesnych technologii.

