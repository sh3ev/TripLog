# TripLog 🌍✈️

Aplikacja do planowania i dokumentowania podróży. Zapisuj wspomnienia, dodawaj zdjęcia i sprawdzaj pogodę w jednym miejscu.

## ✨ Najważniejsze funkcje

- 📸 **Galeria zdjęć** – dodawaj swoje zdjęcia z każdej podróży.
- 🗺️ **Wybór i edycja lokalizacji** – wyszukiwarka z podpowiedziami, edytowalna karta celu podróży
- 📅 **Wybór zakresu dat** – planuj daty swoich wyjazdów
- ☁️ **Prognoza pogody** – automatyczne pobieranie pogody dla wybranej lokalizacji i dat
- 🔍 **Wyszukiwarka podróży** – szybkie filtrowanie i przeglądanie historii
- 👤 **Profil użytkownika** – edycja danych, zmiana hasła, personalizacja
- 🗑️ **Usuwanie i edycja podróży** – pełna kontrola nad swoimi wpisami

## 📱 Zrzuty ekranu


## 🚀 Instalacja

### Wymagania
- Android 7.0 (API 24) lub nowszy
- Android Studio

### Konfiguracja
1. **Sklonuj repozytorium**
   ```bash
   git clone https://github.com/sh3ev/TripLog.git
   cd TripLog
   ```
2. **Dodaj klucze API**
   - [OpenWeatherMap](https://openweathermap.org/api) – pogoda

   
   W pliku `local.properties`:
   ```properties
   OPENWEATHER_API_KEY=twój_klucz_api
   ```
3. **Zbuduj i uruchom**
   ```bash
   ./gradlew installDebug
   ```

## 🛠️ Technologie

- **Język:** Kotlin
- **Architektura:** MVVM, Repository
- **Baza danych:** Room (SQLite)
- **Mapy:** Photon
- **Zdjęcia:** Android Photo Picker (PickMultipleVisualMedia)
- **UI:** Material Design 3, Material Components

## 📂 Struktura projektu (skrót)

```
app/src/main/java/com/example/triplog/
├── data/          # Baza danych Room (encje, DAO)
├── network/       # API (pogoda, lokalizacje)
├── ui/            # Ekrany aplikacji
│   ├── login/     # Logowanie
│   ├── register/  # Rejestracja
│   ├── main/      # Lista podróży
│   ├── profile/   # Profil użytkownika
│   └── trips/     # Dodawanie/edycja/szczegóły podróży
└── utils/         # Narzędzia pomocnicze
```

## 📄 Licencja

MIT License - zobacz [LICENSE](LICENSE)
