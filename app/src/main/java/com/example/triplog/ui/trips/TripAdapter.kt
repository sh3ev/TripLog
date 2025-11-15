package com.example.triplog.ui.trips

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.R
import com.example.triplog.data.entities.TripEntity
import java.io.File

class TripAdapter(
    private val onItemClick: (TripEntity) -> Unit,
    private val onItemLongClick: (TripEntity) -> Unit,
    private val onEditClick: (TripEntity) -> Unit,
    private val onDeleteClick: (TripEntity) -> Unit
) : ListAdapter<TripEntity, TripAdapter.TripViewHolder>(TripDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewThumbnail)
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val imageButtonEdit: ImageButton = itemView.findViewById(R.id.imageButtonEdit)
        private val imageButtonDelete: ImageButton = itemView.findViewById(R.id.imageButtonDelete)

        fun bind(trip: TripEntity) {
            textViewTitle.text = trip.title
            textViewDate.text = trip.date

            // Load image if exists
            if (!trip.imagePath.isNullOrEmpty()) {
                val file = File(trip.imagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_foreground)
            }

            itemView.setOnClickListener {
                onItemClick(trip)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(trip)
                true
            }

            imageButtonEdit.setOnClickListener {
                onEditClick(trip)
            }

            imageButtonDelete.setOnClickListener {
                onDeleteClick(trip)
            }
        }
    }

    class TripDiffCallback : DiffUtil.ItemCallback<TripEntity>() {
        override fun areItemsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean {
            return oldItem == newItem
        }
    }
}

