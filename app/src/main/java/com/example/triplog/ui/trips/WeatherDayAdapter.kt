package com.example.triplog.ui.trips

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.databinding.ItemWeatherDayBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.graphics.BitmapFactory

data class DayWeather(
    val date: LocalDate,
    val temperature: Double,
    val description: String,
    val iconCode: String,
    val isAvailable: Boolean = true // false jeśli brak prognozy
)

class WeatherDayAdapter : ListAdapter<DayWeather, WeatherDayAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWeatherDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemWeatherDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(weather: DayWeather) {
            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("pl"))
            binding.textViewDate.text = weather.date.format(formatter)
                .replaceFirstChar { it.uppercase() }

            if (weather.isAvailable) {
                binding.textViewTemperature.text = "${weather.temperature.toInt()}°C"
                binding.textViewDescription.text = weather.description
                    .replaceFirstChar { it.uppercase() }

                // Załaduj ikonę pogody z OpenWeather
                loadWeatherIcon(binding.imageViewWeatherIcon, weather.iconCode)
            } else {
                binding.textViewTemperature.text = "—"
                binding.textViewDescription.text = "Brak prognozy"
                binding.imageViewWeatherIcon.setImageResource(android.R.drawable.ic_menu_help)
            }
        }
        
        private fun loadWeatherIcon(imageView: ImageView, iconCode: String) {
            if (iconCode.isEmpty()) return
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("https://openweathermap.org/img/wn/${iconCode}@2x.png")
                    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    // Ignoruj błędy ładowania ikony
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DayWeather>() {
        override fun areItemsTheSame(oldItem: DayWeather, newItem: DayWeather): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DayWeather, newItem: DayWeather): Boolean {
            return oldItem == newItem
        }
    }
}
