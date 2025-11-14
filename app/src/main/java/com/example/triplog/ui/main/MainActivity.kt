package com.example.triplog.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TripAdapter
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var currentUserEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserEmail = SharedPreferencesHelper.getLoggedInUser(this)
        if (currentUserEmail == null) {
            navigateToLogin()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Moje Podróże"

        setupRecyclerView()
        setupSearch()
        observeTrips()

        binding.fabAddTrip.setOnClickListener {
            startActivity(Intent(this, AddTripActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh trips when returning from AddTripActivity
        observeTrips()
    }

    private fun setupRecyclerView() {
        adapter = TripAdapter(
            onItemClick = { trip ->
                val intent = Intent(this, TripDetailsActivity::class.java)
                intent.putExtra("TRIP_ID", trip.id)
                startActivity(intent)
            },
            onItemLongClick = { trip ->
                showTripOptionsDialog(trip)
            }
        )

        binding.recyclerViewTrips.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTrips.adapter = adapter
    }

    private fun setupSearch() {
        binding.editTextSearch.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    observeTrips()
                } else {
                    observeSearchResults(newText)
                }
                return true
            }
        })
    }

    private fun observeTrips() {
        currentUserEmail?.let { email ->
            lifecycleScope.launch {
                database.tripDao().getTripsByUser(email).collect { trips ->
                    adapter.submitList(trips)
                }
            }
        }
    }

    private fun observeSearchResults(query: String) {
        currentUserEmail?.let { email ->
            lifecycleScope.launch {
                database.tripDao().searchTrips(email, query).collect { trips ->
                    adapter.submitList(trips)
                }
            }
        }
    }

    private fun showTripOptionsDialog(trip: TripEntity) {
        val options = arrayOf("Edytuj", "Usuń", "Wyślij email")
        AlertDialog.Builder(this)
            .setTitle(trip.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Edit
                        val intent = Intent(this, AddTripActivity::class.java)
                        intent.putExtra("TRIP_ID", trip.id)
                        startActivity(intent)
                    }
                    1 -> {
                        // Delete
                        showDeleteConfirmationDialog(trip)
                    }
                    2 -> {
                        // Send email
                        sendTripEmail(trip)
                    }
                }
            }
            .show()
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
                    database.tripDao().deleteTrip(trip)
                }
                Toast.makeText(this@MainActivity, "Podróż usunięta", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Błąd usuwania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendTripEmail(trip: TripEntity) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("example@email.com")) // Można zmienić na rzeczywisty adres
            putExtra(Intent.EXTRA_SUBJECT, "Podróż: ${trip.title}")
            putExtra(
                Intent.EXTRA_TEXT,
                "Tytuł: ${trip.title}\n" +
                        "Opis: ${trip.description}\n" +
                        "Data: ${trip.date}\n" +
                        if (trip.weatherSummary != null) "Pogoda: ${trip.weatherSummary}\n" else ""
            )
        }
        startActivity(Intent.createChooser(intent, "Wyślij email"))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                SharedPreferencesHelper.clearLoggedInUser(this)
                navigateToLogin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

