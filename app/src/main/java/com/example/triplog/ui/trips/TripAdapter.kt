package com.example.triplog.ui.trips

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.R
import com.example.triplog.data.AppDatabase
import com.example.triplog.data.entities.TripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TripAdapter(
    private val database: AppDatabase,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onItemClick: (TripEntity) -> Unit,
    private val onItemLongClick: (TripEntity) -> Unit,
    private val onEditClick: (TripEntity) -> Unit,
    private val onDeleteClick: (TripEntity) -> Unit
) : ListAdapter<TripEntity, TripAdapter.TripViewHolder>(TripDiffCallback()) {

    // Bitmap cache - max 20MB
    private val imageCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: TripViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelImageLoading()
    }

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewThumbnail)
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val imageButtonEdit: ImageButton = itemView.findViewById(R.id.imageButtonEdit)
        private val imageButtonDelete: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        
        private var imageLoadingJob: Job? = null

        fun cancelImageLoading() {
            imageLoadingJob?.cancel()
            imageLoadingJob = null
        }

        fun bind(trip: TripEntity) {
            textViewTitle.text = trip.title
            textViewDate.text = trip.date

            // Cancel any previous image loading
            cancelImageLoading()
            
            // Set default image
            imageView.setImageResource(R.drawable.ic_launcher_foreground)
            
            // Load first image from trip_images table with caching
            imageLoadingJob = lifecycleScope.launch {
                val firstImage = withContext(Dispatchers.IO) {
                    database.tripImageDao().getImagesByTripIdSync(trip.id).firstOrNull()
                }
                
                firstImage?.let { tripImage ->
                    val cachedBitmap = imageCache.get(tripImage.imagePath)
                    if (cachedBitmap != null) {
                        imageView.setImageBitmap(cachedBitmap)
                    } else {
                        val file = File(tripImage.imagePath)
                        if (file.exists()) {
                            val bitmap = withContext(Dispatchers.IO) {
                                // Decode with sampling for memory efficiency
                                val options = BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                BitmapFactory.decodeFile(file.absolutePath, options)
                                
                                options.inSampleSize = calculateInSampleSize(options, 200, 200)
                                options.inJustDecodeBounds = false
                                BitmapFactory.decodeFile(file.absolutePath, options)
                            }
                            bitmap?.let {
                                imageCache.put(tripImage.imagePath, it)
                                imageView.setImageBitmap(it)
                            }
                        }
                    }
                }
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
        
        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val (height: Int, width: Int) = options.run { outHeight to outWidth }
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
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

