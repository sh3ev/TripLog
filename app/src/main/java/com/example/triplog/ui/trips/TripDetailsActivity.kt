package com.example.triplog.ui.trips

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.example.triplog.R
import com.example.triplog.config.ApiConfig
import com.example.triplog.data.AppDatabase
import com.example.triplog.databinding.ActivityTripDetailsBinding
import com.example.triplog.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class TripDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripDetailsBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: TripDetailsViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this, TripDetailsViewModelFactory(database))[TripDetailsViewModel::class.java]
    }
    private var tripId: Long = -1
    private var lastWeatherUpdateTime: Long = 0
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private lateinit var imageGalleryAdapter: ImageGalleryAdapter
    private lateinit var weatherForecastAdapter: WeatherDayAdapter
    private var totalImages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) {
            Toast.makeText(this, "Błąd: brak ID podróży", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Przycisk wstecz
        binding.buttonBack.setOnClickListener { finish() }

        setupImageGallery()
        setupWeatherForecast()
        loadTripDetails()
        loadTripImages()
    }

    private fun setupImageGallery() {
        imageGalleryAdapter = ImageGalleryAdapter { position, images ->
            openFullscreenImage(position)
        }
        
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewImages.apply {
            adapter = imageGalleryAdapter
            this.layoutManager = layoutManager
        }
        
        // Snap scrolling - zatrzymuje się na każdym zdjęciu
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerViewImages)
        
        // Aktualizuj licznik zdjęć przy scrollowaniu
        binding.recyclerViewImages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (position != RecyclerView.NO_POSITION && totalImages > 0) {
                        binding.textViewImageCounter.text = "${position + 1} / $totalImages"
                    }
                }
            }
        })
    }
    
    private fun setupWeatherForecast() {
        weatherForecastAdapter = WeatherDayAdapter()
        binding.recyclerViewWeatherForecast.apply {
            adapter = weatherForecastAdapter
            layoutManager = LinearLayoutManager(this@TripDetailsActivity)
            isNestedScrollingEnabled = false
        }
    }

    private fun loadTripImages() {
        lifecycleScope.launch {
            database.tripImageDao().getImagesByTripId(tripId).collect { images ->
                totalImages = images.size
                imageGalleryAdapter.submitList(images)
                
                if (images.isEmpty()) {
                    binding.layoutImages.visibility = View.GONE
                } else {
                    binding.layoutImages.visibility = View.VISIBLE
                    binding.layoutImageIndicator.visibility = if (images.size > 1) View.VISIBLE else View.GONE
                    binding.textViewImageCounter.text = "1 / ${images.size}"
                }
            }
        }
    }

    private fun openFullscreenImage(position: Int) {
        val intent = Intent(this, FullscreenImageActivity::class.java)
        intent.putExtra("TRIP_ID", tripId)
        intent.putExtra("POSITION", position)
        startActivity(intent)
    }

    private fun loadTripDetails() {
        lifecycleScope.launch {
            try {
                val trip = withContext(Dispatchers.IO) {
                    database.tripDao().getTripById(tripId)
                }
                
                if (trip == null) {
                    Toast.makeText(this@TripDetailsActivity, "Podróż nie znaleziona", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                try {
                    binding.textViewTitle.text = trip.title
                    binding.textViewDescription.text = trip.description
                    
                    // Wyświetl zakres dat
                    binding.textViewDate.text = formatDateRange(trip.date, trip.endDate)

                    if (trip.latitude != null && trip.longitude != null) {
                        val lat = trip.latitude!!
                        val lon = trip.longitude!!
                        
                        // Wyświetl nazwę lokalizacji
                        if (!trip.locationName.isNullOrEmpty()) {
                            // Wyświetl tylko miasto (pierwsza część przed przecinkiem)
                            val cityName = trip.locationName!!.split(",").firstOrNull()?.trim() ?: trip.locationName
                            binding.textViewLocation.text = cityName
                        } else {
                            binding.textViewLocation.text = String.format(Locale.getDefault(), "%.4f°N, %.4f°E", lat, lon)
                        }
                        
                        // Pokaż sekcję lokalizacji i mapę
                        binding.layoutDestination.visibility = View.VISIBLE
                        binding.cardMap.visibility = View.VISIBLE
                        binding.textViewNoLocation.visibility = View.GONE
                        
                        binding.buttonRefreshWeather.setOnClickListener {
                            loadWeatherForecast(lat, lon, trip.date, trip.endDate)
                        }
                        binding.buttonRefreshWeather.isEnabled = true
                        
                        // Show map
                        showMap(lat, lon)
                        
                        // Załaduj prognozę pogody
                        loadWeatherForecast(lat, lon, trip.date, trip.endDate)
                    } else {
                        // Ukryj sekcję lokalizacji i mapę
                        binding.layoutDestination.visibility = View.GONE
                        binding.cardMap.visibility = View.GONE
                        binding.textViewNoLocation.visibility = View.VISIBLE
                        
                        binding.buttonRefreshWeather.isEnabled = false
                        binding.textViewNoWeather.visibility = View.VISIBLE
                        binding.recyclerViewWeatherForecast.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@TripDetailsActivity, "Błąd wyświetlania danych: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TripDetailsActivity, "Błąd ładowania: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
    
    private fun formatDateRange(startDate: String?, endDate: String?): String {
        if (startDate.isNullOrEmpty()) return "Brak daty"
        
        return try {
            val polishLocale = Locale.forLanguageTag("pl")
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", polishLocale)
            val start = LocalDate.parse(startDate)
            
            if (endDate.isNullOrEmpty() || startDate == endDate) {
                start.format(formatter)
            } else {
                val end = LocalDate.parse(endDate)
                val shortFormatter = DateTimeFormatter.ofPattern("d MMM", polishLocale)
                
                if (start.year == end.year) {
                    "${start.format(shortFormatter)} - ${end.format(formatter)}"
                } else {
                    "${start.format(formatter)} - ${end.format(formatter)}"
                }
            }
        } catch (e: Exception) {
            startDate
        }
    }
    
    private fun loadWeatherForecast(lat: Double, lon: Double, startDate: String?, endDate: String?) {
        if (startDate.isNullOrEmpty()) {
            binding.textViewNoWeather.visibility = View.VISIBLE
            binding.recyclerViewWeatherForecast.visibility = View.GONE
            return
        }
        
        binding.progressBarWeather.visibility = View.VISIBLE
        binding.recyclerViewWeatherForecast.visibility = View.GONE
        binding.textViewNoWeather.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val forecast = withContext(Dispatchers.IO) {
                    RetrofitInstance.weatherApi.getForecast(
                        latitude = lat,
                        longitude = lon,
                        apiKey = ApiConfig.OPENWEATHER_API_KEY
                    )
                }
                
                val start = LocalDate.parse(startDate)
                val end = if (!endDate.isNullOrEmpty()) LocalDate.parse(endDate) else start
                
                // Grupuj prognozy po dniach
                val forecastByDay = forecast.list.groupBy { item ->
                    Instant.ofEpochSecond(item.dt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.mapValues { (_, items) ->
                    items.minByOrNull { item ->
                        val hour = Instant.ofEpochSecond(item.dt)
                            .atZone(ZoneId.systemDefault())
                            .hour
                        kotlin.math.abs(hour - 12)
                    }
                }
                
                // Utwórz listę dni z pogodą
                val weatherDays = mutableListOf<DayWeather>()
                var currentDate = start
                
                while (!currentDate.isAfter(end)) {
                    val forecastItem = forecastByDay[currentDate]
                    
                    if (forecastItem != null) {
                        weatherDays.add(
                            DayWeather(
                                date = currentDate,
                                temperature = forecastItem.main.temp,
                                description = forecastItem.weather.firstOrNull()?.description ?: "",
                                iconCode = forecastItem.weather.firstOrNull()?.icon ?: "01d",
                                isAvailable = true
                            )
                        )
                    } else {
                        weatherDays.add(
                            DayWeather(
                                date = currentDate,
                                temperature = 0.0,
                                description = "",
                                iconCode = "",
                                isAvailable = false
                            )
                        )
                    }
                    
                    currentDate = currentDate.plusDays(1)
                }
                
                binding.progressBarWeather.visibility = View.GONE
                
                if (weatherDays.isEmpty()) {
                    binding.textViewNoWeather.visibility = View.VISIBLE
                    binding.recyclerViewWeatherForecast.visibility = View.GONE
                } else {
                    binding.textViewNoWeather.visibility = View.GONE
                    binding.recyclerViewWeatherForecast.visibility = View.VISIBLE
                    weatherForecastAdapter.submitList(weatherDays)
                }
                
            } catch (e: Exception) {
                binding.progressBarWeather.visibility = View.GONE
                binding.textViewNoWeather.visibility = View.VISIBLE
                binding.textViewNoWeather.text = "Błąd pobierania prognozy"
                binding.recyclerViewWeatherForecast.visibility = View.GONE
            }
        }
    }

    private fun showMap(latitude: Double, longitude: Double) {
        binding.webViewMap.visibility = View.VISIBLE
        binding.textViewNoLocation.visibility = View.GONE
        
        val webView = binding.webViewMap
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        // HTML with OpenStreetMap using Leaflet
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body { margin: 0; padding: 0; }
                    #map { height: 250px; width: 100%; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([$latitude, $longitude], 13);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);
                    var marker = L.marker([$latitude, $longitude]).addTo(map);
                    marker.bindPopup("<b>Lokalizacja podróży</b>").openPopup();
                </script>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL("https://example.com", html, "text/html", "UTF-8", null)
    }

}

