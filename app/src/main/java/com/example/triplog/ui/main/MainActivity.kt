package com.example.triplog.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var currentUserEmail: String? = null
    private lateinit var tripAdapter: TripAdapter

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

    override fun onResume() {
        super.onResume()
        loadTrips()
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
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    private fun showAvatarMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_main, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
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
                    database.tripDao().deleteTrip(trip)
                }
                Toast.makeText(this@MainActivity, "Podróż usunięta", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Błąd usuwania: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
