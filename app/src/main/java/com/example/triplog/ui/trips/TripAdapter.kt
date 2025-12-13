package com.example.triplog.ui.trips

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.exifinterface.media.ExifInterface
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class TripAdapter(
    private val database: AppDatabase,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onItemClick: (TripEntity) -> Unit,
    private val onItemLongClick: (TripEntity) -> Unit,
    private val onEditClick: (TripEntity) -> Unit,
    private val onDeleteClick: (TripEntity) -> Unit,
    private val onCalendarClick: (TripEntity) -> Unit = { }
) : ListAdapter<TripEntity, TripAdapter.TripViewHolder>(TripDiffCallback()) {

    companion object {
        const val VIEW_TYPE_VERTICAL = 0
        const val VIEW_TYPE_HORIZONTAL = 1
    }

    var isHorizontalMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    // Bitmap cache - max 20MB
    private val imageCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isHorizontalMode) VIEW_TYPE_HORIZONTAL else VIEW_TYPE_VERTICAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_HORIZONTAL) {
            R.layout.item_trip_horizontal
        } else {
            R.layout.item_trip
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
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
        private val textViewCountdown: TextView = itemView.findViewById(R.id.textViewCountdown)
        private val buttonCalendar: LinearLayout = itemView.findViewById(R.id.buttonCalendar)
        private val buttonDelete: LinearLayout = itemView.findViewById(R.id.buttonDelete)
        private val buttonMore: LinearLayout = itemView.findViewById(R.id.buttonMore)
        
        private var imageLoadingJob: Job? = null

        fun cancelImageLoading() {
            imageLoadingJob?.cancel()
            imageLoadingJob = null
        }

        fun bind(trip: TripEntity) {
            textViewTitle.text = trip.title
            
            // Formatuj zakres dat
            textViewDate.text = formatDateRange(trip.date, trip.endDate)
            
            // Pokaż countdown dla przyszłych podróży
            updateCountdown(trip)

            // Cancel any previous image loading
            cancelImageLoading()
            
            // Set default image
            imageView.setImageResource(R.drawable.ic_triplog_foreground)
            
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
                                val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                                
                                // Apply EXIF rotation
                                rawBitmap?.let { rotateBitmapIfRequired(it, file.absolutePath) }
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

            // Calendar button - edit dates
            buttonCalendar.setOnClickListener {
                onCalendarClick(trip)
            }

            // Delete button
            buttonDelete.setOnClickListener {
                onDeleteClick(trip)
            }

            // More button - edit trip
            buttonMore.setOnClickListener {
                onEditClick(trip)
            }
        }
        
        private fun updateCountdown(trip: TripEntity) {
            try {
                val startDate = LocalDate.parse(trip.date)
                val today = LocalDate.now()
                val daysUntil = ChronoUnit.DAYS.between(today, startDate)
                
                when {
                    daysUntil > 0 -> {
                        textViewCountdown.visibility = View.VISIBLE
                        textViewCountdown.text = when {
                            daysUntil == 1L -> "Jutro wyjazd!"
                            daysUntil < 7 -> "$daysUntil dni do wyjazdu"
                            else -> "$daysUntil dni do wyjazdu"
                        }
                    }
                    daysUntil == 0L -> {
                        textViewCountdown.visibility = View.VISIBLE
                        textViewCountdown.text = "Dziś wyjazd!"
                    }
                    else -> {
                        textViewCountdown.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                textViewCountdown.visibility = View.GONE
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

    class TripDiffCallback : DiffUtil.ItemCallback<TripEntity>() {
        override fun areItemsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TripEntity, newItem: TripEntity): Boolean {
            return oldItem == newItem
        }
    }
    
    private fun formatDateRange(startDate: String?, endDate: String?): String {
        if (startDate.isNullOrEmpty()) return "Brak daty"
        
        return try {
            val polishLocale = Locale.forLanguageTag("pl")
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", polishLocale)
            val start = LocalDate.parse(startDate)
            
            if (endDate.isNullOrEmpty() || startDate == endDate) {
                start.format(formatter)
            } else {
                val end = LocalDate.parse(endDate)
                val shortFormatter = DateTimeFormatter.ofPattern("d MMM", polishLocale)
                
                if (start.year == end.year) {
                    "${start.format(shortFormatter)} - ${end.format(formatter)}"
                } else {
                    "${start.format(formatter)} - ${end.format(formatter)}"
                }
            }
        } catch (e: Exception) {
            startDate
        }
    }
}

