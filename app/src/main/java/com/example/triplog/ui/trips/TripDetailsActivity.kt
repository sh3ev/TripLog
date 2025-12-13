package com.example.triplog.ui.trips

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.R
import com.example.triplog.config.ApiConfig
import com.example.triplog.data.AppDatabase
import com.example.triplog.data.entities.TripEntity
import com.example.triplog.databinding.ActivityTripDetailsBinding
import com.example.triplog.network.RetrofitInstance
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TripDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripDetailsBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var tripId: Long = -1
    private var currentTrip: TripEntity? = null
    private lateinit var imageGalleryAdapter: ImageGalleryAdapter
    private lateinit var weatherForecastAdapter: WeatherDayAdapter
    private var totalImages = 0

    private val dateEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val startDate = result.data?.getStringExtra(DateRangePickerActivity.EXTRA_START_DATE)
            val endDate = result.data?.getStringExtra(DateRangePickerActivity.EXTRA_END_DATE)
            
            if (startDate != null && endDate != null) {
                updateTripDates(startDate, endDate)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tripId = intent.getLongExtra("TRIP_ID", -1)
        if (tripId == -1L) {
            Toast.makeText(this, "Błąd: brak ID podróży", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupBottomSheet()
        setupImageGallery()
        setupWeatherForecast()
        setupNavigation()
        loadTripDetails()
        loadTripImages()
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetContainer)
        
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        
        // peekHeight - navbar + trochę contentu
        val bottomNavHeight = (72 * density).toInt()
        val collapsedContentHeight = (100 * density).toInt()
        bottomSheetBehavior.peekHeight = bottomNavHeight + collapsedContentHeight
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.halfExpandedRatio = 0.55f
        
        // Stan początkowy: half-expanded
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        
        val statusBarHeight = getStatusBarHeight()
        
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        binding.bottomSheet.setPadding(0, statusBarHeight, 0, 0)
                        binding.layoutWeather.visibility = View.VISIBLE
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        binding.bottomSheet.setPadding(0, 0, 0, 0)
                        binding.layoutWeather.visibility = View.VISIBLE
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        binding.bottomSheet.setPadding(0, 0, 0, 0)
                        binding.layoutWeather.visibility = View.GONE
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Opcjonalne animacje podczas przesuwania
            }
        })
    }
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun setupNavigation() {
        binding.navBack.setOnClickListener { finish() }
        
        binding.navCalendar.setOnClickListener {
            currentTrip?.let { trip ->
                val intent = Intent(this, DateRangePickerActivity::class.java).apply {
                    putExtra(DateRangePickerActivity.EXTRA_EDIT_MODE, true)
                    putExtra(DateRangePickerActivity.EXTRA_TRIP_ID, trip.id)
                    putExtra(DateRangePickerActivity.EXTRA_START_DATE, trip.date)
                    putExtra(DateRangePickerActivity.EXTRA_END_DATE, trip.endDate ?: trip.date)
                    putExtra(DateRangePickerActivity.EXTRA_LOCATION_NAME, trip.title)
                }
                dateEditLauncher.launch(intent)
            }
        }
        
        binding.navEdit.setOnClickListener {
            currentTrip?.let { trip ->
                val intent = Intent(this, AddTripActivity::class.java)
                intent.putExtra("TRIP_ID", trip.id)
                startActivity(intent)
            }
        }
        
        binding.navDelete.setOnClickListener {
            currentTrip?.let { trip ->
                showDeleteConfirmationDialog(trip)
            }
        }
    }
    
    private fun updateTripDates(startDate: String, endDate: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                currentTrip?.let { trip ->
                    val updatedTrip = trip.copy(date = startDate, endDate = endDate)
                    database.tripDao().updateTrip(updatedTrip)
                }
            }
            Toast.makeText(this@TripDetailsActivity, "Daty zaktualizowane", Toast.LENGTH_SHORT).show()
            loadTripDetails() // Odśwież dane
        }
    }
    
    private fun showDeleteConfirmationDialog(trip: TripEntity) {
        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle("Usuń podróż")
            .setMessage("Czy na pewno chcesz usunąć podróż \"${trip.title}\"?")
            .setPositiveButton("Usuń") { _, _ -> deleteTrip(trip) }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    
    private fun deleteTrip(trip: TripEntity) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val images = database.tripImageDao().getImagesByTripIdSync(trip.id)
                    images.forEach { image ->
                        val file = File(image.imagePath)
                        if (file.exists()) file.delete()
                    }
                    database.tripDao().deleteTrip(trip)
                }
                Toast.makeText(this@TripDetailsActivity, "Podróż usunięta", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@TripDetailsActivity, "Błąd usuwania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
        
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerViewImages)
        
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
                
                currentTrip = trip

                binding.textViewTitle.text = trip.title
                binding.textViewDate.text = formatDateRange(trip.date, trip.endDate)
                
                // Opis
                if (!trip.description.isNullOrBlank()) {
                    binding.layoutDescription.visibility = View.VISIBLE
                    binding.textViewDescription.text = trip.description
                } else {
                    binding.layoutDescription.visibility = View.GONE
                }

                if (trip.latitude != null && trip.longitude != null) {
                    val lat = trip.latitude!!
                    val lon = trip.longitude!!
                    
                    // Wyświetl nazwę lokalizacji
                    if (!trip.locationName.isNullOrEmpty()) {
                        val cityName = trip.locationName!!.split(",").firstOrNull()?.trim() ?: trip.locationName
                        binding.textViewLocation.text = cityName
                    } else {
                        binding.textViewLocation.text = String.format(Locale.getDefault(), "%.4f°N, %.4f°E", lat, lon)
                    }
                    
                    binding.layoutDestination.visibility = View.VISIBLE
                    binding.textViewNoLocation.visibility = View.GONE
                    
                    binding.buttonRefreshWeather.setOnClickListener {
                        loadWeatherForecast(lat, lon, trip.date, trip.endDate)
                    }
                    binding.buttonRefreshWeather.isEnabled = true
                    
                    // Pokaż mapę
                    showMap(lat, lon)
                    
                    // Załaduj prognozę pogody
                    loadWeatherForecast(lat, lon, trip.date, trip.endDate)
                } else {
                    binding.layoutDestination.visibility = View.GONE
                    binding.textViewNoLocation.visibility = View.VISIBLE
                    binding.buttonRefreshWeather.isEnabled = false
                    binding.textViewNoWeather.visibility = View.VISIBLE
                    binding.recyclerViewWeatherForecast.visibility = View.GONE
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun showMap(latitude: Double, longitude: Double) {
        binding.webViewMap.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            
            val mapHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                    <style>
                        body { margin: 0; padding: 0; }
                        #map { width: 100%; height: 100vh; }
                        .leaflet-control-attribution { display: none; }
                        .leaflet-control-zoom { display: none; }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <script>
                        // Przesunięcie mapy w górę - marker będzie w górnej 1/3 ekranu
                        var offsetLat = $latitude - 0.015;
                        
                        var map = L.map('map', {
                            zoomControl: false,
                            attributionControl: false
                        }).setView([offsetLat, $longitude], 13);
                        
                        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                            maxZoom: 19
                        }).addTo(map);
                        
                        var marker = L.marker([$latitude, $longitude]).addTo(map);
                    </script>
                </body>
                </html>
            """.trimIndent()
            
            loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null)
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (bottomSheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}
