package com.example.triplog.ui.trips

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.triplog.data.AppDatabase
import com.example.triplog.databinding.ActivityTripDetailsBinding
import kotlinx.coroutines.launch
import java.io.File

class TripDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTripDetailsBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: TripDetailsViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this, TripDetailsViewModelFactory(database))[TripDetailsViewModel::class.java]
    }
    private var tripId: Long = -1

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

        loadTripDetails()
        observeWeather()
    }

    private fun loadTripDetails() {
        lifecycleScope.launch {
            try {
                val trip = database.tripDao().getTripById(tripId)
                trip?.let {
                    binding.textViewTitle.text = it.title
                    binding.textViewDescription.text = it.description
                    binding.textViewDate.text = it.date

                    if (!it.imagePath.isNullOrEmpty()) {
                        val file = File(it.imagePath)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            binding.imageViewTrip.setImageBitmap(bitmap)
                        }
                    }

                    if (it.latitude != null && it.longitude != null) {
                        val lat = it.latitude!!
                        val lon = it.longitude!!
                        binding.textViewLocation.text = "Szerokość: $lat, Długość: $lon"
                        binding.buttonGetWeather.setOnClickListener {
                            viewModel.fetchWeather(lat, lon)
                        }
                        binding.buttonGetWeather.isEnabled = true
                    } else {
                        binding.textViewLocation.text = "Brak lokalizacji"
                        binding.buttonGetWeather.isEnabled = false
                    }

                    if (!it.weatherSummary.isNullOrEmpty()) {
                        binding.textViewWeather.text = it.weatherSummary
                    }
                } ?: run {
                    Toast.makeText(this@TripDetailsActivity, "Podróż nie znaleziona", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TripDetailsActivity, "Błąd ładowania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeWeather() {
        lifecycleScope.launch {
            viewModel.weatherState.collect { state ->
                when (state) {
                    is WeatherState.Loading -> {
                        binding.textViewWeather.text = "Ładowanie pogody..."
                        binding.buttonGetWeather.isEnabled = false
                    }
                    is WeatherState.Success -> {
                        binding.textViewWeather.text = state.summary
                        binding.buttonGetWeather.isEnabled = true
                        // Zapisz pogodę w bazie
                        viewModel.updateTripWeather(tripId, state.summary)
                    }
                    is WeatherState.Error -> {
                        binding.textViewWeather.text = state.message
                        binding.buttonGetWeather.isEnabled = true
                        Toast.makeText(this@TripDetailsActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
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

