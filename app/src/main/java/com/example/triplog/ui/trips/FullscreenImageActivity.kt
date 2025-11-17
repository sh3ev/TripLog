package com.example.triplog.ui.trips

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.triplog.data.AppDatabase
import com.example.triplog.databinding.ActivityFullscreenImageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullscreenImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullscreenImageBinding
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tripId = intent.getLongExtra("TRIP_ID", -1)
        val initialPosition = intent.getIntExtra("POSITION", 0)

        if (tripId == -1L) {
            finish()
            return
        }

        binding.buttonClose.setOnClickListener {
            finish()
        }

        loadImages(tripId, initialPosition)
    }

    private fun loadImages(tripId: Long, initialPosition: Int) {
        lifecycleScope.launch {
            val images = withContext(Dispatchers.IO) {
                database.tripImageDao().getImagesByTripIdSync(tripId)
            }

            if (images.isEmpty()) {
                finish()
                return@launch
            }

            val adapter = FullscreenImageAdapter(images)
            binding.viewPagerImages.adapter = adapter
            binding.viewPagerImages.setCurrentItem(initialPosition, false)

            updateCounter(initialPosition + 1, images.size)

            binding.viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateCounter(position + 1, images.size)
                }
            })
        }
    }

    private fun updateCounter(current: Int, total: Int) {
        binding.textViewImageCounter.text = "$current / $total"
    }
}

