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
import android.content.Intent
import com.example.triplog.R
import com.example.triplog.data.AppDatabase
import com.example.triplog.databinding.ActivityTripDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
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

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Szczegóły podróży"

        setupImageGallery()
        loadTripDetails()
        loadTripImages()
        observeWeather()
    }

    private fun setupImageGallery() {
        imageGalleryAdapter = ImageGalleryAdapter { position, images ->
            openFullscreenImage(position)
        }
        binding.recyclerViewImages.apply {
            adapter = imageGalleryAdapter
            layoutManager = LinearLayoutManager(this@TripDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun loadTripImages() {
        lifecycleScope.launch {
            database.tripImageDao().getImagesByTripId(tripId).collect { images ->
                imageGalleryAdapter.submitList(images)
                binding.recyclerViewImages.visibility = if (images.isEmpty()) View.GONE else View.VISIBLE
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
                    binding.textViewDate.text = trip.date

                    if (trip.latitude != null && trip.longitude != null) {
                        val lat = trip.latitude!!
                        val lon = trip.longitude!!
                        binding.textViewLocation.text = String.format(Locale.getDefault(), "%.4f°N, %.4f°E", lat, lon)
                        binding.buttonRefreshWeather.setOnClickListener {
                            viewModel.fetchWeather(lat, lon)
                        }
                        binding.buttonRefreshWeather.isEnabled = true
                        
                        // Show map
                        showMap(lat, lon)
                        
                        // Automatycznie pobierz pogodę jeśli brak zapisanej
                        if (trip.weatherSummary.isNullOrEmpty()) {
                            viewModel.fetchWeather(lat, lon)
                        }
                    } else {
                        binding.textViewLocation.text = "Brak lokalizacji"
                        binding.buttonRefreshWeather.isEnabled = false
                        
                        // Hide map, show no location message
                        binding.webViewMap.visibility = View.GONE
                        binding.textViewNoLocation.visibility = View.VISIBLE
                    }

                    if (!trip.weatherSummary.isNullOrEmpty()) {
                        displayWeatherData(trip.weatherSummary, null)
                    } else {
                        binding.textViewWeather.text = "Brak danych pogodowych"
                        binding.textViewWeatherDescription.visibility = View.GONE
                        binding.textViewWeatherLastUpdate.visibility = View.GONE
                        binding.imageViewWeatherIcon.visibility = View.GONE
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

    private fun observeWeather() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weatherState.collect { state ->
                    when (state) {
                        is WeatherState.Loading -> {
                            binding.textViewWeather.text = "Ładowanie pogody..."
                            binding.textViewWeatherDescription.visibility = View.GONE
                            binding.textViewWeatherLastUpdate.visibility = View.GONE
                            binding.imageViewWeatherIcon.visibility = View.GONE
                            binding.buttonRefreshWeather.isEnabled = false
                        }
                        is WeatherState.Success -> {
                            val summary = state.summary
                            val description = state.response.weather.firstOrNull()?.description
                            displayWeatherData(summary, description)
                            binding.buttonRefreshWeather.isEnabled = true
                            // Zapisz pogodę w bazie
                            viewModel.updateTripWeather(tripId, summary)
                            lastWeatherUpdateTime = System.currentTimeMillis()
                            updateLastUpdateTime()
                        }
                        is WeatherState.Error -> {
                            binding.textViewWeather.text = state.message
                            binding.textViewWeatherDescription.visibility = View.GONE
                            binding.textViewWeatherLastUpdate.visibility = View.GONE
                            binding.imageViewWeatherIcon.visibility = View.GONE
                            binding.buttonRefreshWeather.isEnabled = true
                            Toast.makeText(this@TripDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun displayWeatherData(summary: String, description: String?) {
        // Parse temperature from summary (format: "XX°C, description")
        val parts = summary.split(", ")
        val temperature = parts.firstOrNull() ?: summary
        val weatherDescription = description ?: parts.getOrNull(1) ?: ""

        binding.textViewWeather.text = temperature
        if (weatherDescription.isNotEmpty()) {
            val capitalized = weatherDescription.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            binding.textViewWeatherDescription.text = capitalized
            binding.textViewWeatherDescription.visibility = View.VISIBLE
        } else {
            binding.textViewWeatherDescription.visibility = View.GONE
        }
        binding.imageViewWeatherIcon.visibility = View.VISIBLE
    }

    private fun updateLastUpdateTime() {
        if (lastWeatherUpdateTime > 0) {
            val timeString = timeFormat.format(Date(lastWeatherUpdateTime))
            binding.textViewWeatherLastUpdate.text = "Ostatnia aktualizacja: $timeString"
            binding.textViewWeatherLastUpdate.visibility = View.VISIBLE
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

