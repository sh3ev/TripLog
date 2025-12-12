package com.example.triplog.ui.main

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triplog.R
import com.example.triplog.data.AppDatabase
import com.example.triplog.data.entities.TripEntity
import com.example.triplog.databinding.ActivityMainBinding
import com.example.triplog.ui.login.LoginActivity
import com.example.triplog.ui.profile.ProfileActivity
import com.example.triplog.ui.trips.AddTripActivity
import com.example.triplog.ui.trips.TripAdapter
import com.example.triplog.ui.trips.TripDetailsActivity
import com.example.triplog.utils.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var currentUserEmail: String? = null
    private lateinit var tripAdapter: TripAdapter
    private var searchJob: Job? = null
    private var backPressedTime: Long = 0

    private val profileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadUserProfile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageView3.setOnClickListener {
            navigateToProfile()
        }

        binding.buttonAddTrip.setOnClickListener {
            navigateToAddTrip()
        }

        currentUserEmail = SharedPreferencesHelper.getLoggedInUser(this)
        if (currentUserEmail == null) {
            navigateToLogin()
            return
        }

        setupRecyclerView()
        setupSearchBar()
        loadUserProfile()
        loadTrips()
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                currentUserEmail?.let { database.userDao().getUserByEmail(it) }
            }
            user?.let {
                // Ustaw powitanie
                val displayName = when {
                    !it.firstName.isNullOrBlank() -> it.firstName
                    else -> it.name.split(" ").firstOrNull() ?: it.name
                }
                binding.textView7.text = displayName

                // Załaduj zdjęcie profilowe lub placeholder
                if (!it.profileImagePath.isNullOrBlank()) {
                    val file = File(it.profileImagePath!!)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(it.profileImagePath)
                        val rotatedBitmap = rotateBitmapIfRequired(bitmap, it.profileImagePath!!)
                        binding.imageView3.setImageBitmap(rotatedBitmap)
                    } else {
                        binding.imageView3.setImageResource(R.drawable.ic_person_placeholder)
                    }
                } else {
                    binding.imageView3.setImageResource(R.drawable.ic_person_placeholder)
                }
            }
        }
    }

    private fun setupSearchBar() {
        // Show/hide clear button based on text
        binding.imageViewClearSearch.setOnClickListener {
            binding.editTextText.text.clear()
        }

        // Search functionality with debounce
        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Show/hide clear button
                binding.imageViewClearSearch.visibility = 
                    if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
                
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce delay
                    searchTrips(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchTrips(query: String) {
        currentUserEmail?.let { email ->
            lifecycleScope.launch {
                if (query.isBlank()) {
                    // If search is empty, load all trips
                    database.tripDao().getTripsByUser(email).collect { trips ->
                        tripAdapter.submitList(trips)
                        updateEmptyState(trips.isEmpty())
                    }
                } else {
                    // Search trips by query
                    database.tripDao().searchTrips(email, query).collect { trips ->
                        tripAdapter.submitList(trips)
                        updateEmptyState(trips.isEmpty())
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh with current search query
        val currentQuery = binding.editTextText.text.toString()
        if (currentQuery.isBlank()) {
            loadTrips()
        } else {
            searchTrips(currentQuery)
        }
    }

    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(
            database = database,
            lifecycleScope = lifecycleScope,
            onItemClick = { trip ->
                navigateToTripDetails(trip)
            },
            onItemLongClick = { trip ->
                // Long click functionality if needed
            },
            onEditClick = { trip ->
                navigateToEditTrip(trip)
            },
            onDeleteClick = { trip ->
                showDeleteConfirmationDialog(trip)
            }
        )
        binding.recyclerViewTrips.apply {
            adapter = tripAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun loadTrips() {
        currentUserEmail?.let { email ->
            lifecycleScope.launch {
                database.tripDao().getTripsByUser(email).collect { trips ->
                    tripAdapter.submitList(trips)
                    updateEmptyState(trips.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewTrips.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }


    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        profileLauncher.launch(intent)
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToAddTrip() {
        startActivity(Intent(this, AddTripActivity::class.java))
    }

    private fun navigateToTripDetails(trip: TripEntity) {
        val intent = Intent(this, TripDetailsActivity::class.java)
        intent.putExtra("TRIP_ID", trip.id)
        startActivity(intent)
    }

    private fun navigateToEditTrip(trip: TripEntity) {
        val intent = Intent(this, AddTripActivity::class.java)
        intent.putExtra("TRIP_ID", trip.id)
        startActivity(intent)
    }

    private fun showDeleteConfirmationDialog(trip: TripEntity) {
        AlertDialog.Builder(this)
            .setTitle("Usuń podróż")
            .setMessage("Czy na pewno chcesz usunąć podróż \"${trip.title}\"?")
            .setPositiveButton("Usuń") { _, _ ->
                deleteTrip(trip)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun deleteTrip(trip: TripEntity) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Pobierz i usuń fizyczne pliki zdjęć
                    val images = database.tripImageDao().getImagesByTripIdSync(trip.id)
                    images.forEach { image ->
                        val file = java.io.File(image.imagePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    database.tripDao().deleteTrip(trip)
                }
                Toast.makeText(this@MainActivity, "Podróż usunięta", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Błąd usuwania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            return
        } else {
            Toast.makeText(this, "Naciśnij ponownie, aby wyjść", Toast.LENGTH_SHORT).show()
        }
        backPressedTime = System.currentTimeMillis()
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
