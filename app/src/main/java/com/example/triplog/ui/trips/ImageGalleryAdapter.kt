package com.example.triplog.ui.trips

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
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
                    val rotatedBitmap = rotateBitmapIfRequired(bitmap, file.absolutePath)
                    imageView.setImageBitmap(rotatedBitmap)
                } else {
                    imageView.setImageResource(R.drawable.ic_triplog_foreground)
                }
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.ic_triplog_foreground)
            }

            itemView.setOnClickListener {
                onImageClick(position, currentList)
            }
        }
        
        private fun rotateBitmapIfRequired(bitmap: Bitmap, imagePath: String): Bitmap {
            return try {
                val exif = ExifInterface(imagePath)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    else -> return bitmap
                }
                
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } catch (e: Exception) {
                bitmap
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

