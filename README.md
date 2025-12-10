# TripLog 🌍✈️

Aplikacja Android do planowania i dokumentowania podróży. Zapisuj wspomnienia, dodawaj zdjęcia i sprawdzaj pogodę w jednym miejscu.

## ✨ Główne funkcje

- 📸 **Galeria zdjęć** - dodawaj wiele zdjęć do każdej podróży
- 🗺️ **Wyszukiwanie lokalizacji** - znajdź miejsce docelowe z podpowiedziami
- 📅 **Wybór dat** - intuicyjny kalendarz z możliwością wyboru zakresu
- ☁️ **Prognoza pogody** - sprawdź pogodę na wybrane dni przed wyjazdem
- 🔍 **Wyszukiwarka** - szybko znajdź swoje podróże
- 👤 **Profil użytkownika** - personalizuj swoje konto

## 📱 Zrzuty ekranu

| Ekran główny | Szczegóły podróży | Profil |
|:------------:|:-----------------:|:------:|
| Lista Twoich podróży | Zdjęcia, mapa i pogoda | Edycja danych |

## 🚀 Instalacja

### Wymagania
- Android 7.0 (API 24) lub nowszy
- Android Studio

### Konfiguracja

1. **Sklonuj repozytorium**
   \`\`\`bash
   git clone https://github.com/sh3ev/TripLog.git
   cd TripLog
   \`\`\`

2. **Dodaj klucz API pogody**
   
   Utwórz darmowe konto na [OpenWeatherMap](https://openweathermap.org/api) i dodaj klucz do `local.properties`:
   \`\`\`properties
   OPENWEATHER_API_KEY=twój_klucz_api
   \`\`\`

3. **Zbuduj i uruchom**
   \`\`\`bash
   ./gradlew installDebug
   \`\`\`

## 🛠️ Technologie

| Kategoria | Technologia |
|-----------|-------------|
| Język | Kotlin |
| Architektura | MVVM |
| Baza danych | Room |
| Sieć | Retrofit |
| Mapy | OpenStreetMap (Leaflet) |
| Pogoda | OpenWeather API |
| Lokalizacje | Photon API |

## 📂 Struktura projektu

\`\`\`
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
\`\`\`

## 📄 Licencja

MIT License - zobacz [LICENSE](LICENSE)

---

Stworzone z ❤️ przez [@sh3ev](https://github.com/sh3ev)