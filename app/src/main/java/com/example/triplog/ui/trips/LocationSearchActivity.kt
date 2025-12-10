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
import com.example.triplog.config.ApiConfig
import com.example.triplog.databinding.ActivityLocationSearchBinding
import com.example.triplog.network.GeocodingResponse
import com.example.triplog.network.RetrofitInstance
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
    private lateinit var adapter: LocationSuggestionAdapter
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
        adapter = LocationSuggestionAdapter { location ->
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
                        delay(300) // Krótszy delay dla lepszego UX
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
            val results = RetrofitInstance.geocodingApi.getCoordinates(
                query = query,
                limit = 10, // Pobierz więcej wyników do filtrowania
                apiKey = ApiConfig.OPENWEATHER_API_KEY
            )
            
            // Usuń duplikaty - miejsca z prawie identycznymi współrzędnymi
            val uniqueResults = results.distinctBy { location ->
                // Zaokrąglij do 2 miejsc po przecinku (~1km dokładność)
                val latRounded = (location.lat * 100).toLong()
                val lonRounded = (location.lon * 100).toLong()
                "$latRounded,$lonRounded"
            }.take(5)
            
            if (uniqueResults.isNotEmpty()) {
                showResults(uniqueResults)
            } else {
                showNoResults()
            }
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
    
    private fun showResults(results: List<GeocodingResponse>) {
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
        binding.textViewEmpty.text = "Wpisz nazwę miejsca, aby wyszukać"
        binding.textViewEmpty.visibility = View.VISIBLE
        binding.recyclerViewSuggestions.visibility = View.GONE
        adapter.submitList(emptyList())
    }
    
    private fun selectLocation(location: GeocodingResponse) {
        val displayName = location.getPolishDisplayName()
        
        val resultIntent = Intent().apply {
            putExtra(EXTRA_LOCATION_NAME, displayName)
            putExtra(EXTRA_LATITUDE, location.lat)
            putExtra(EXTRA_LONGITUDE, location.lon)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    private fun GeocodingResponse.getPolishDisplayName(): String {
        val localName = local_names?.get("pl") ?: name
        val polishCountry = getPolishCountryName(country)
        return buildString {
            append(localName)
            state?.let { append(", $it") }
            append(", $polishCountry")
        }
    }
    
    private fun getPolishCountryName(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "PL" -> "Polska"
            "DE" -> "Niemcy"
            "FR" -> "Francja"
            "ES" -> "Hiszpania"
            "IT" -> "Włochy"
            "GB", "UK" -> "Wielka Brytania"
            "US" -> "Stany Zjednoczone"
            "CZ" -> "Czechy"
            "SK" -> "Słowacja"
            "UA" -> "Ukraina"
            "AT" -> "Austria"
            "CH" -> "Szwajcaria"
            "NL" -> "Holandia"
            "BE" -> "Belgia"
            "PT" -> "Portugalia"
            "GR" -> "Grecja"
            "HR" -> "Chorwacja"
            "HU" -> "Węgry"
            "SE" -> "Szwecja"
            "NO" -> "Norwegia"
            "DK" -> "Dania"
            "FI" -> "Finlandia"
            "IE" -> "Irlandia"
            "RU" -> "Rosja"
            "JP" -> "Japonia"
            "CN" -> "Chiny"
            "AU" -> "Australia"
            "CA" -> "Kanada"
            "MX" -> "Meksyk"
            "BR" -> "Brazylia"
            "AR" -> "Argentyna"
            "EG" -> "Egipt"
            "TR" -> "Turcja"
            "TH" -> "Tajlandia"
            "IN" -> "Indie"
            "KR" -> "Korea Południowa"
            else -> countryCode
        }
    }
}
