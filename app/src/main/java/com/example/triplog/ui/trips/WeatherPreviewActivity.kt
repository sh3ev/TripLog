package com.example.triplog.ui.trips

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triplog.config.ApiConfig
import com.example.triplog.databinding.ActivityWeatherPreviewBinding
import com.example.triplog.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeatherPreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOCATION_NAME = "location_name"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_START_DATE = "start_date"
        const val EXTRA_END_DATE = "end_date"
    }

    private lateinit var binding: ActivityWeatherPreviewBinding
    private lateinit var adapter: WeatherDayAdapter

    private var locationName: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pobierz dane z intent
        locationName = intent.getStringExtra(EXTRA_LOCATION_NAME) ?: ""
        latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
        startDate = intent.getStringExtra(EXTRA_START_DATE)?.let { LocalDate.parse(it) }
        endDate = intent.getStringExtra(EXTRA_END_DATE)?.let { LocalDate.parse(it) }

        setupUI()
        setupRecyclerView()
        loadWeatherForecast()

        binding.imageViewBack.setOnClickListener { finish() }
        binding.buttonContinue.setOnClickListener { returnResult() }
    }

    private fun setupUI() {
        binding.textViewLocation.text = locationName.split(",").firstOrNull() ?: locationName

        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("pl"))
        val dateRange = if (startDate != null && endDate != null) {
            "${startDate!!.format(formatter)} - ${endDate!!.format(formatter)}"
        } else ""
        binding.textViewDateRange.text = dateRange
    }

    private fun setupRecyclerView() {
        adapter = WeatherDayAdapter()
        binding.recyclerViewWeather.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewWeather.adapter = adapter
    }

    private fun loadWeatherForecast() {
        if (startDate == null || endDate == null) return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val forecast = withContext(Dispatchers.IO) {
                    RetrofitInstance.weatherApi.getForecast(
                        latitude = latitude,
                        longitude = longitude,
                        apiKey = ApiConfig.OPENWEATHER_API_KEY
                    )
                }

                // Grupuj prognozy po dniach (bierzemy prognozę na godzinę 12:00 lub najbliższą)
                val forecastByDay = forecast.list.groupBy { item ->
                    Instant.ofEpochSecond(item.dt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.mapValues { (_, items) ->
                    // Wybierz prognozę najbliższą godzinie 12:00
                    items.minByOrNull { item ->
                        val hour = Instant.ofEpochSecond(item.dt)
                            .atZone(ZoneId.systemDefault())
                            .hour
                        kotlin.math.abs(hour - 12)
                    }
                }

                // Utwórz listę dni z pogodą dla zakresu podróży
                val weatherDays = mutableListOf<DayWeather>()
                var currentDate = startDate!!

                while (!currentDate.isAfter(endDate)) {
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

                binding.progressBar.visibility = View.GONE
                adapter.submitList(weatherDays)

                // Aktualizuj info
                val availableDays = weatherDays.count { it.isAvailable }
                val totalDays = weatherDays.size
                binding.textViewInfo.text = if (availableDays < totalDays) {
                    "Prognoza dostępna dla $availableDays z $totalDays dni (max 5 dni do przodu)"
                } else {
                    "Prognoza pogody na wszystkie dni podróży"
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@WeatherPreviewActivity, "Błąd pobierania pogody: ${e.message}", Toast.LENGTH_SHORT).show()

                // Pokaż dni bez pogody
                val weatherDays = mutableListOf<DayWeather>()
                var currentDate = startDate!!
                while (!currentDate.isAfter(endDate)) {
                    weatherDays.add(
                        DayWeather(
                            date = currentDate,
                            temperature = 0.0,
                            description = "",
                            iconCode = "",
                            isAvailable = false
                        )
                    )
                    currentDate = currentDate.plusDays(1)
                }
                adapter.submitList(weatherDays)
            }
        }
    }

    private fun returnResult() {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_LOCATION_NAME, locationName)
            putExtra(EXTRA_LATITUDE, latitude)
            putExtra(EXTRA_LONGITUDE, longitude)
            putExtra(EXTRA_START_DATE, startDate.toString())
            putExtra(EXTRA_END_DATE, endDate.toString())
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
