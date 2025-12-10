package com.example.triplog.ui.trips

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.databinding.ItemLocationSuggestionBinding
import com.example.triplog.network.GeocodingResponse

class LocationSuggestionAdapter(
    private val onItemClick: (GeocodingResponse) -> Unit
) : ListAdapter<GeocodingResponse, LocationSuggestionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLocationSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(location: GeocodingResponse) {
            // Użyj polskiej nazwy jeśli dostępna
            val localName = location.local_names?.get("pl") ?: location.name
            binding.textViewName.text = localName
            
            // Opis: region, kraj
            val description = buildString {
                location.state?.let { append(it) }
                if (isNotEmpty()) append(", ")
                append(getPolishCountryName(location.country))
            }
            binding.textViewDescription.text = description
        }
        
        private fun getPolishCountryName(countryCode: String): String {
            return when (countryCode.uppercase()) {
                "PL" -> "Polska"
                "DE" -> "Niemcy"
                "FR" -> "Francja"
                "ES" -> "Hiszpania"
                "IT" -> "Włochy"
                "GB", "UK" -> "Wielka Brytania"
                "US" -> "Stany Zjednoczone"
                "CZ" -> "Czechy"
                "SK" -> "Słowacja"
                "UA" -> "Ukraina"
                "AT" -> "Austria"
                "CH" -> "Szwajcaria"
                "NL" -> "Holandia"
                "BE" -> "Belgia"
                "PT" -> "Portugalia"
                "GR" -> "Grecja"
                "HR" -> "Chorwacja"
                "HU" -> "Węgry"
                "SE" -> "Szwecja"
                "NO" -> "Norwegia"
                "DK" -> "Dania"
                "FI" -> "Finlandia"
                "IE" -> "Irlandia"
                "RU" -> "Rosja"
                "JP" -> "Japonia"
                "CN" -> "Chiny"
                "AU" -> "Australia"
                "CA" -> "Kanada"
                "MX" -> "Meksyk"
                "BR" -> "Brazylia"
                "AR" -> "Argentyna"
                "EG" -> "Egipt"
                "TR" -> "Turcja"
                "TH" -> "Tajlandia"
                "IN" -> "Indie"
                "KR" -> "Korea Południowa"
                else -> countryCode
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<GeocodingResponse>() {
        override fun areItemsTheSame(oldItem: GeocodingResponse, newItem: GeocodingResponse): Boolean {
            return oldItem.lat == newItem.lat && oldItem.lon == newItem.lon
        }

        override fun areContentsTheSame(oldItem: GeocodingResponse, newItem: GeocodingResponse): Boolean {
            return oldItem == newItem
        }
    }
}
