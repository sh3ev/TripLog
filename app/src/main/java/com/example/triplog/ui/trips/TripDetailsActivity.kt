package com.example.triplog.ui.trips

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import com.example.triplog.ui.login.LoginActivity
import com.example.triplog.utils.SharedPreferencesHelper
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
                    } else {
                        binding.textViewLocation.text = "Brak lokalizacji"
                        binding.buttonRefreshWeather.isEnabled = false
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_trip_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_delete_trip -> {
                showDeleteConfirmationDialog()
                true
            }
            R.id.menu_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Wylogowanie")
            .setMessage("Czy na pewno chcesz się wylogować?")
            .setPositiveButton("Wyloguj") { _, _ ->
                SharedPreferencesHelper.clearLoggedInUser(this)
                navigateToLogin()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDeleteConfirmationDialog() {
        lifecycleScope.launch {
            val trip = withContext(Dispatchers.IO) {
                database.tripDao().getTripById(tripId)
            }
            trip?.let {
                AlertDialog.Builder(this@TripDetailsActivity)
                    .setTitle("Usuń podróż")
                    .setMessage("Czy na pewno chcesz usunąć podróż \"${it.title}\"?")
                    .setPositiveButton("Usuń") { _, _ ->
                        deleteTrip()
                    }
                    .setNegativeButton("Anuluj", null)
                    .show()
            }
        }
    }

    private fun deleteTrip() {
        lifecycleScope.launch {
            try {
                viewModel.deleteTrip(tripId)
                Toast.makeText(this@TripDetailsActivity, "Podróż usunięta", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@TripDetailsActivity, "Błąd usuwania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class TripDetailsViewModelFactory(private val database: AppDatabase) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripDetailsViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

