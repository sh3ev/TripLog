package com.example.triplog.ui.trips

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.R
import com.example.triplog.data.entities.TripImageEntity
import java.io.File

class ImageGalleryAdapter(
    private val onImageClick: (Int, List<TripImageEntity>) -> Unit
) : ListAdapter<TripImageEntity, ImageGalleryAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewTripPhoto)

        fun bind(image: TripImageEntity, position: Int) {
            try {
                val file = File(image.imagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.ic_launcher_foreground)
            }

            itemView.setOnClickListener {
                onImageClick(position, currentList)
            }
        }
    }

    class ImageDiffCallback : DiffUtil.ItemCallback<TripImageEntity>() {
        override fun areItemsTheSame(oldItem: TripImageEntity, newItem: TripImageEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TripImageEntity, newItem: TripImageEntity): Boolean {
            return oldItem == newItem
        }
    }
}

