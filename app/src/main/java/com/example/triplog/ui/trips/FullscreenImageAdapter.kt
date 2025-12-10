package com.example.triplog.ui.trips

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
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
                    val rotatedBitmap = rotateBitmapIfRequired(bitmap, file.absolutePath)
                    imageView.setImageBitmap(rotatedBitmap)
                } else {
                    imageView.setImageResource(R.drawable.ic_triplog_foreground)
                }
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.ic_triplog_foreground)
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
}

