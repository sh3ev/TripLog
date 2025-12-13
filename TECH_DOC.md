# Dokumentacja techniczna TripLog

## Technologie i architektura

- **Język:** Kotlin
- **Platforma:** Android (minSdk 24)
- **UI:** Material Design 3, Material Components, własne ikony SVG
- **Baza danych:** Room (SQLite)
- **Mapy:** Google Maps SDK
- **Zarządzanie zdjęciami:** Android Photo Picker (PickMultipleVisualMedia), wsparcie dla wielu zdjęć naraz
- **Wzorce projektowe:** MVVM (ViewModel, LiveData), Repository
- **Inne:** Data Binding, RecyclerView, BottomSheetBehavior, ActivityResult API

## Struktura projektu

- `app/src/main/java/com/example/triplog/` – kod źródłowy aplikacji
- `data/` – warstwa dostępu do danych (Room, DAO, encje)
- `ui/` – ekrany, adaptery, logika UI
- `utils/` – klasy pomocnicze
- `res/` – zasoby (layouty, drawable, kolory, stringi)

## Główne komponenty

- **AddTripActivity** – dodawanie/edycja podróży, obsługa zdjęć, lokalizacji, dat
- **TripDetailsActivity** – szczegóły podróży, galeria, mapa, pogoda
- **ProfileActivity** – profil użytkownika, zmiana hasła
- **RegisterActivity/LoginActivity** – rejestracja i logowanie
- **LocationSearchActivity** – wyszukiwanie lokalizacji (Google Places API)
- **DateRangePickerActivity** – wybór zakresu dat

## Integracje i uprawnienia

- **Photon API** – wyświetlanie mapy i markerów, autouzupełnianie lokalizacji
- **Uprawnienia:**
  - `INTERNET`, `ACCESS_FINE_LOCATION`, `READ_MEDIA_IMAGES`/`READ_EXTERNAL_STORAGE`
  - Obsługa runtime permissions dla zdjęć

## Testowanie i uruchamianie

- Projekt uruchamiany przez Android Studio (Gradle)
- Emulator lub urządzenie fizyczne (Android 7.0+)
- Photon nie potrzebuje klucz API

## Uwagi

- Photo Picker na Androidzie 13+ nie pokazuje wszystkich zdjęć z dysku – ograniczenie systemowe
- Kod zgodny z Material Design
