# TripLog - Dziennik Podróży 🌍✈️

Polska aplikacja Android do zarządzania podróżami z funkcjami galerii zdjęć, map i pobierania aktualnej pogody.

## Funkcje

### 🔐 Autoryzacja
- Logowanie i rejestracja użytkowników
- Bezpieczne hashowanie haseł (SHA-256)
- Automatyczne logowanie przy kolejnych uruchomieniach
- Potwierdzenie wyjścia z aplikacji (dwukrotne wstecz)

### 📸 Zarządzanie podróżami
- Lista podróży z miniaturami pierwszego zdjęcia
- Dodawanie wielu zdjęć do każdej podróży
- Galeria zdjęć w szczegółach podróży (poziome przewijanie)
- Pełnoekranowy widok zdjęć z możliwością przełączania
- Wyszukiwanie podróży w czasie rzeczywistym (po tytule i opisie)
- Edycja i usuwanie podróży z automatycznym czyszczeniem plików zdjęć
- Komunikat "Brak podróży" gdy lista jest pusta

### 🗺️ Lokalizacja i mapy
- Pobieranie aktualnej lokalizacji GPS
- Interaktywna mapa OpenStreetMap w szczegółach podróży
- Wyświetlanie współrzędnych geograficznych

### ☁️ Pogoda
- Pobieranie aktualnej pogody z OpenWeather API
- Automatyczne pobieranie pogody przy wejściu w szczegóły podróży
- Możliwość ręcznego odświeżenia danych pogodowych
- Zapisywanie danych pogodowych w bazie

### 🎨 Interfejs użytkownika
- Całkowicie polski interfejs
- Material Design
- Loading states przy logowaniu
- DatePicker z ograniczeniem do dzisiejszej daty
- Menu użytkownika (wylogowanie) po kliknięciu w avatar

## Technologie

- **Kotlin** - język programowania
- **MVVM-lite** - architektura aplikacji
- **Room (SQLite)** - lokalna baza danych z migracjami
- **Retrofit** - komunikacja z API pogodowym
- **Coroutines + Flow** - asynchroniczne operacje i reaktywne strumienie
- **StateFlow** - zarządzanie stanem UI w ViewModelach
- **FusedLocationProviderClient** - lokalizacja GPS
- **RecyclerView + ListAdapter** - lista podróży i galeria zdjęć
- **LruCache** - cachowanie bitmap dla wydajności
- **ViewPager2** - pełnoekranowy widok zdjęć
- **WebView + Leaflet** - wyświetlanie map OpenStreetMap
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
├── config/                # Konfiguracja (klucze API)
│   └── ApiConfig.kt
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
│   │   ├── LoginActivity.kt
│   │   ├── LoginViewModel.kt
│   │   └── LoginViewModelFactory.kt
│   ├── register/          # Ekran rejestracji
│   │   └── RegisterActivity.kt
│   ├── main/              # Główny ekran z listą podróży
│   │   └── MainActivity.kt
│   └── trips/             # Ekrany związane z podróżami
│       ├── AddTripActivity.kt           # Dodawanie/edycja podróży
│       ├── TripDetailsActivity.kt       # Szczegóły podróży
│       ├── TripDetailsViewModel.kt      # ViewModel szczegółów
│       ├── TripDetailsViewModelFactory.kt
│       ├── FullscreenImageActivity.kt   # Pełnoekranowy widok zdjęć
│       ├── TripAdapter.kt               # Adapter listy podróży
│       ├── ImageGalleryAdapter.kt       # Adapter galerii zdjęć
│       ├── SelectedImageAdapter.kt      # Adapter wybranych zdjęć
│       └── FullscreenImageAdapter.kt    # Adapter ViewPager2
└── utils/                 # Narzędzia pomocnicze
    ├── SharedPreferencesHelper.kt
    └── PasswordHasher.kt
```

## Zrzuty ekranu

| Logowanie | Lista podróży | Szczegóły podróży |
|-----------|---------------|-------------------|
| ![Login](screenshots/login.png) | ![List](screenshots/list.png) | ![Details](screenshots/details.png) |

## Baza danych

Aplikacja używa Room Database z następującymi tabelami:

- **users** - dane użytkowników (email, hasło SHA-256, imię)
- **trips** - informacje o podróżach (tytuł, opis, data, lokalizacja GPS, pogoda)
- **trip_images** - zdjęcia podróży (relacja 1:N z trips, z indeksem kolejności)

### Relacje
- `UserEntity` → `TripEntity` (1:N, CASCADE DELETE)
- `TripEntity` → `TripImageEntity` (1:N, CASCADE DELETE)

Baza danych automatycznie migruje z wersji 1 do wersji 2 przy pierwszym uruchomieniu.

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

## Budowanie projektu

```bash
# Sklonuj repozytorium
git clone https://github.com/sh3ev/TripLog.git
cd TripLog

# Skonfiguruj klucz API
echo "OPENWEATHER_API_KEY=twój_klucz_api" >> local.properties

# Zbuduj projekt
./gradlew assembleDebug

# Zainstaluj na podłączonym urządzeniu
./gradlew installDebug
```

## Licencja

MIT License - zobacz plik [LICENSE](LICENSE)

## Autor

[@sh3ev](https://github.com/sh3ev)

