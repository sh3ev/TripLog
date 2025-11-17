package com.example.triplog.ui.trips

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.R
import com.example.triplog.data.entities.TripImageEntity
import java.io.File

class FullscreenImageAdapter(
    private val images: List<TripImageEntity>
) : RecyclerView.Adapter<FullscreenImageAdapter.FullscreenImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullscreenImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fullscreen_image, parent, false)
        return FullscreenImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: FullscreenImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    class FullscreenImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewFullscreen)

        fun bind(image: TripImageEntity) {
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
        }
    }
}

