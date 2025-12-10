package com.example.triplog.ui.trips

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.databinding.ItemLocationSuggestionBinding
import com.example.triplog.network.PhotonFeature

class PhotonLocationAdapter(
    private val onItemClick: (PhotonFeature) -> Unit
) : ListAdapter<PhotonFeature, PhotonLocationAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(location: PhotonFeature) {
            val props = location.properties
            
            // Nazwa główna
            binding.textViewName.text = props.getDisplayName()
            
            // Opis: miasto/region, kraj
            val descriptionParts = mutableListOf<String>()
            
            // Dodaj miasto jeśli inne niż nazwa
            if (props.city != null && props.city != props.name) {
                descriptionParts.add(props.city)
            }
            
            // Dodaj region/stan
            props.state?.let { 
                if (it != props.name && it != props.city) {
                    descriptionParts.add(it) 
                }
            }
            
            // Dodaj kraj (po polsku)
            props.getPolishCountryName()?.let { descriptionParts.add(it) }
            
            binding.textViewDescription.text = descriptionParts.joinToString(", ")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PhotonFeature>() {
        override fun areItemsTheSame(oldItem: PhotonFeature, newItem: PhotonFeature): Boolean {
            return oldItem.properties.osm_id == newItem.properties.osm_id
        }

        override fun areContentsTheSame(oldItem: PhotonFeature, newItem: PhotonFeature): Boolean {
            return oldItem == newItem
        }
    }
}
