package com.example.triplog.ui.trips

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triplog.data.AppDatabase
import com.example.triplog.data.entities.TripEntity
import com.example.triplog.data.entities.TripImageEntity
import com.example.triplog.databinding.ActivityAddTripBinding
import com.example.triplog.utils.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTripActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTripBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val selectedImagePaths = mutableListOf<String>()
    private var tripId: Long? = null
    private var currentUserEmail: String? = null
    private lateinit var selectedImageAdapter: SelectedImageAdapter
    
    // Lokalizacja
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentLocationName: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            saveImageFromUri(uri)
        }
    }
    
    private val locationSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                currentLocationName = data.getStringExtra(LocationSearchActivity.EXTRA_LOCATION_NAME)
                currentLatitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LATITUDE, 0.0)
                currentLongitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LONGITUDE, 0.0)
                
                updateLocationUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserEmail = SharedPreferencesHelper.getLoggedInUser(this)
            ?: run {
                finish()
                return
            }

        tripId = intent.getLongExtra("TRIP_ID", -1).takeIf { it != -1L }

        setupImageRecyclerView()
        setupLocationSelection()

        if (tripId != null) {
            binding.buttonSave.text = "Zaktualizuj"
            loadTripData()
        }

        binding.buttonSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.editTextDate.setOnClickListener {
            showDatePicker()
        }

        binding.buttonSave.setOnClickListener {
            saveTrip()
        }
    }

    private fun setupLocationSelection() {
        // Kliknięcie otwiera ekran wyszukiwania
        binding.layoutSelectLocation.setOnClickListener {
            val intent = Intent(this, LocationSearchActivity::class.java)
            locationSearchLauncher.launch(intent)
        }
        
        // Czyszczenie lokalizacji
        binding.imageViewClearLocation.setOnClickListener {
            clearLocation()
        }
    }
    
    private fun updateLocationUI() {
        if (currentLocationName != null) {
            binding.textViewSelectedLocation.text = currentLocationName
            binding.layoutSelectedLocation.visibility = View.VISIBLE
            binding.textViewLocationHint.text = "Zmień lokalizację"
        } else {
            binding.layoutSelectedLocation.visibility = View.GONE
            binding.textViewLocationHint.text = "Dokąd chcesz jechać?"
        }
    }
    
    private fun clearLocation() {
        currentLocationName = null
        currentLatitude = null
        currentLongitude = null
        updateLocationUI()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // Jeśli pole daty ma już wartość, sparsuj ją i ustaw jako początkową
        val currentDateText = binding.editTextDate.text.toString()
        if (currentDateText.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(currentDateText)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // Jeśli nie można sparsować, użyj dzisiejszej daty
            }
        }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.editTextDate.setText(dateFormat.format(selectedDate.time))
            },
            year,
            month,
            day
        )
        
        // Ogranicz datę do dzisiaj (podróże są z przeszłości)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        
        datePickerDialog.show()
    }

    private fun setupImageRecyclerView() {
        selectedImageAdapter = SelectedImageAdapter { imagePath ->
            removeImage(imagePath)
        }
        binding.recyclerViewSelectedImages.apply {
            adapter = selectedImageAdapter
            layoutManager = LinearLayoutManager(this@AddTripActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun removeImage(imagePath: String) {
        selectedImagePaths.remove(imagePath)
        selectedImageAdapter.submitList(selectedImagePaths.toList())
    }

    private fun saveImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val imageDir = File(getExternalFilesDir(null), "trip_images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }
            val imageFile = File(imageDir, "trip_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(imageFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            
            selectedImagePaths.add(imageFile.absolutePath)
            selectedImageAdapter.submitList(selectedImagePaths.toList())
            Toast.makeText(this, "Zdjęcie dodane", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd zapisu zdjęcia: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTripData() {
        tripId?.let { id ->
            lifecycleScope.launch {
                try {
                    val trip = withContext(Dispatchers.IO) {
                        database.tripDao().getTripById(id)
                    }
                    trip?.let {
                        binding.editTextTitle.setText(it.title)
                        binding.editTextDescription.setText(it.description)
                        binding.editTextDate.setText(it.date)
                        
                        // Load existing images
                        val images = withContext(Dispatchers.IO) {
                            database.tripImageDao().getImagesByTripIdSync(id)
                        }
                        selectedImagePaths.clear()
                        selectedImagePaths.addAll(images.map { img -> img.imagePath })
                        selectedImageAdapter.submitList(selectedImagePaths.toList())
                        
                        // Załaduj lokalizację
                        if (it.latitude != null && it.longitude != null) {
                            currentLatitude = it.latitude
                            currentLongitude = it.longitude
                            currentLocationName = it.locationName
                            updateLocationUI()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AddTripActivity, "Błąd ładowania: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveTrip() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val date = binding.editTextDate.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val savedTripId = if (tripId != null) {
                    // Update existing trip
                    val existingTrip = withContext(Dispatchers.IO) {
                        database.tripDao().getTripById(tripId!!)
                    }
                    val updatedTrip = existingTrip?.copy(
                        title = title,
                        description = description,
                        date = date,
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        locationName = currentLocationName
                    ) ?: return@launch
                    
                    withContext(Dispatchers.IO) {
                        database.tripDao().updateTrip(updatedTrip)
                        // Usuń stare pliki zdjęć, które nie są już wybrane
                        val oldImages = database.tripImageDao().getImagesByTripIdSync(tripId!!)
                        oldImages.forEach { image ->
                            if (!selectedImagePaths.contains(image.imagePath)) {
                                val file = java.io.File(image.imagePath)
                                if (file.exists()) {
                                    file.delete()
                                }
                            }
                        }
                        // Delete old images from DB and insert new ones
                        database.tripImageDao().deleteImagesByTripId(tripId!!)
                    }
                    tripId!!
                } else {
                    // Create new trip
                    val newTrip = TripEntity(
                        userEmail = currentUserEmail!!,
                        title = title,
                        description = description,
                        date = date,
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        locationName = currentLocationName
                    )
                    
                    withContext(Dispatchers.IO) {
                        database.tripDao().insertTrip(newTrip)
                    }
                }

                // Save images
                if (selectedImagePaths.isNotEmpty()) {
                    val imageEntities = selectedImagePaths.mapIndexed { index, path ->
                        TripImageEntity(
                            tripId = savedTripId,
                            imagePath = path,
                            orderIndex = index
                        )
                    }
                    withContext(Dispatchers.IO) {
                        database.tripImageDao().insertImages(imageEntities)
                    }
                }

                Toast.makeText(this@AddTripActivity, if (tripId != null) "Podróż zaktualizowana" else "Podróż zapisana", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddTripActivity, "Błąd zapisu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}