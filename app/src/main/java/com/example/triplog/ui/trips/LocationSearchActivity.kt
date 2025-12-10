package com.example.triplog.ui.trips

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triplog.databinding.ActivityLocationSearchBinding
import com.example.triplog.network.PhotonFeature
import com.example.triplog.network.RetrofitInstance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationSearchActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_LOCATION_NAME = "location_name"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
    }
    
    private lateinit var binding: ActivityLocationSearchBinding
    private lateinit var adapter: PhotonLocationAdapter
    private var searchJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupSearchInput()
        setupClickListeners()
        
        // Automatycznie pokaż klawiaturę
        binding.editTextSearch.requestFocus()
    }
    
    private fun setupRecyclerView() {
        adapter = PhotonLocationAdapter { location ->
            selectLocation(location)
        }
        binding.recyclerViewSuggestions.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSuggestions.adapter = adapter
    }
    
    private fun setupSearchInput() {
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                
                // Pokaż/ukryj przycisk czyszczenia
                binding.imageViewClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Debounce wyszukiwania
                searchJob?.cancel()
                if (query.length >= 2) {
                    searchJob = lifecycleScope.launch {
                        delay(400)
                        searchLocations(query)
                    }
                } else {
                    showEmptyState()
                }
            }
        })
        
        // Wyszukaj po naciśnięciu Enter
        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.editTextSearch.text.toString().trim()
                if (query.length >= 2) {
                    searchJob?.cancel()
                    lifecycleScope.launch { searchLocations(query) }
                }
                true
            } else false
        }
    }
    
    private fun setupClickListeners() {
        binding.imageViewBack.setOnClickListener {
            finish()
        }
        
        binding.imageViewClear.setOnClickListener {
            binding.editTextSearch.text.clear()
            showEmptyState()
        }
    }
    
    private suspend fun searchLocations(query: String) {
        showLoading()
        
        try {
            val response = RetrofitInstance.photonApi.search(
                query = query,
                limit = 10
            )
            
            // Filtruj tylko miejsca (miasta, wsie, POI) - usuń duplikaty
            val uniqueResults = response.features
                .filter { feature ->
                    val type = feature.properties.type
                    type in listOf("city", "town", "village", "locality", "hamlet", 
                                   "suburb", "neighbourhood", "district",
                                   "attraction", "tourism", "amenity", "building")
                    || feature.properties.osm_key in listOf("place", "tourism", "amenity", "leisure")
                }
                .distinctBy { feature ->
                    "${feature.properties.name},${feature.properties.countrycode}"
                }
                .take(6)
            
            if (uniqueResults.isNotEmpty()) {
                showResults(uniqueResults)
            } else {
                // Jeśli nie ma wyników po filtrze, pokaż wszystkie
                val allResults = response.features.distinctBy { 
                    "${it.properties.name},${it.properties.countrycode}" 
                }.take(6)
                
                if (allResults.isNotEmpty()) {
                    showResults(allResults)
                } else {
                    showNoResults()
                }
            }
        } catch (e: CancellationException) {
            // Ignore - coroutine was cancelled
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd wyszukiwania: ${e.message}", Toast.LENGTH_SHORT).show()
            showEmptyState()
        }
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.textViewEmpty.visibility = View.GONE
        binding.recyclerViewSuggestions.visibility = View.GONE
    }
    
    private fun showResults(results: List<PhotonFeature>) {
        binding.progressBar.visibility = View.GONE
        binding.textViewEmpty.visibility = View.GONE
        binding.recyclerViewSuggestions.visibility = View.VISIBLE
        adapter.submitList(results)
    }
    
    private fun showNoResults() {
        binding.progressBar.visibility = View.GONE
        binding.textViewEmpty.text = "Nie znaleziono lokalizacji"
        binding.textViewEmpty.visibility = View.VISIBLE
        binding.recyclerViewSuggestions.visibility = View.GONE
        adapter.submitList(emptyList())
    }
    
    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.textViewEmpty.visibility = View.GONE
        binding.recyclerViewSuggestions.visibility = View.GONE
        adapter.submitList(emptyList())
    }
    
    private fun selectLocation(location: PhotonFeature) {
        val displayName = location.properties.getFullDisplayName()
        
        val resultIntent = Intent().apply {
            putExtra(EXTRA_LOCATION_NAME, displayName)
            putExtra(EXTRA_LATITUDE, location.geometry.getLat())
            putExtra(EXTRA_LONGITUDE, location.geometry.getLon())
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
