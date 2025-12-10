package com.example.triplog.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triplog.R
import com.example.triplog.data.AppDatabase
import com.example.triplog.data.entities.TripEntity
import com.example.triplog.databinding.ActivityMainBinding
import com.example.triplog.ui.login.LoginActivity
import com.example.triplog.ui.trips.AddTripActivity
import com.example.triplog.ui.trips.TripAdapter
import com.example.triplog.ui.trips.TripDetailsActivity
import com.example.triplog.utils.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var currentUserEmail: String? = null
    private lateinit var tripAdapter: TripAdapter
    private var searchJob: Job? = null
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageView3.setOnClickListener { view ->
            showAvatarMenu(view)
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

        // Fetch user and set the welcome message
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                currentUserEmail?.let { database.userDao().getUserByEmail(it) }
            }
            user?.let {
                binding.textView7.text = "Witaj, ${it.name}!"
            }
        }

        loadTrips()
    }

    private fun setupSearchBar() {
        // Clear button functionality
        binding.editTextText.setOnTouchListener { v, event ->
            val drawableEnd = 2 // Right drawable index
            if (event.rawX >= (binding.editTextText.right - binding.editTextText.compoundDrawables[drawableEnd].bounds.width())) {
                binding.editTextText.text.clear()
                return@setOnTouchListener true
            }
            false
        }

        // Search functionality with debounce
        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
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


    private fun showAvatarMenu(anchor: View) {
        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(arrayOf("Wyloguj")) { _, which ->
                when (which) {
                    0 -> showLogoutConfirmationDialog()
                }
            }
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Wylogowanie")
            .setMessage("Czy na pewno chcesz się wylogować?")
            .setPositiveButton("Wyloguj") { _, _ ->
                SharedPreferencesHelper.clearLoggedInUser(this)
                navigateToLogin()
            }
            .setNegativeButton("Anuluj", null)
            .show()
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
}
