package com.example.triplog.ui.trips

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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddTripActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTripBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val selectedImagePaths = mutableListOf<String>()
    private var tripId: Long? = null
    private var currentUserEmail: String? = null
    private lateinit var selectedImageAdapter: SelectedImageAdapter
    
    // Lokalizacja i daty
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentLocationName: String? = null
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            saveImageFromUri(uri)
        }
    }
    
    // Launcher dla wyboru lokalizacji - po wyborze otwiera wybór dat
    private val locationSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val locationName = data.getStringExtra(LocationSearchActivity.EXTRA_LOCATION_NAME)
                val latitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LATITUDE, 0.0)
                val longitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LONGITUDE, 0.0)
                
                // Otwórz wybór dat z danymi lokalizacji
                val intent = Intent(this, DateRangePickerActivity::class.java).apply {
                    putExtra(DateRangePickerActivity.EXTRA_LOCATION_NAME, locationName)
                    putExtra(DateRangePickerActivity.EXTRA_LATITUDE, latitude)
                    putExtra(DateRangePickerActivity.EXTRA_LONGITUDE, longitude)
                }
                dateRangePickerLauncher.launch(intent)
            }
        }
    }
    
    // Launcher dla wyboru dat
    private val dateRangePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                currentLocationName = data.getStringExtra(DateRangePickerActivity.EXTRA_LOCATION_NAME)
                currentLatitude = data.getDoubleExtra(DateRangePickerActivity.EXTRA_LATITUDE, 0.0)
                currentLongitude = data.getDoubleExtra(DateRangePickerActivity.EXTRA_LONGITUDE, 0.0)
                
                val startDateStr = data.getStringExtra(DateRangePickerActivity.EXTRA_START_DATE)
                val endDateStr = data.getStringExtra(DateRangePickerActivity.EXTRA_END_DATE)
                
                startDate = startDateStr?.let { LocalDate.parse(it) }
                endDate = endDateStr?.let { LocalDate.parse(it) }
                
                updateLocationAndDateUI()
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

        binding.buttonSave.setOnClickListener {
            saveTrip()
        }
    }

    private fun setupLocationSelection() {
        binding.layoutSelectLocation.setOnClickListener {
            val intent = Intent(this, LocationSearchActivity::class.java)
            locationSearchLauncher.launch(intent)
        }
        
        binding.imageViewClearLocation.setOnClickListener {
            clearLocationAndDates()
        }
    }
    
    private fun updateLocationAndDateUI() {
        if (currentLocationName != null && startDate != null && endDate != null) {
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("pl"))
            val dateRange = "${startDate!!.format(formatter)} - ${endDate!!.format(formatter)}"
            
            binding.textViewSelectedLocation.text = "$currentLocationName\n$dateRange"
            binding.layoutSelectedLocation.visibility = View.VISIBLE
            binding.textViewLocationHint.text = "Zmień cel podróży"
        } else {
            binding.layoutSelectedLocation.visibility = View.GONE
            binding.textViewLocationHint.text = "Dokąd chcesz jechać?"
        }
    }
    
    private fun clearLocationAndDates() {
        currentLocationName = null
        currentLatitude = null
        currentLongitude = null
        startDate = null
        endDate = null
        updateLocationAndDateUI()
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
                        
                        val images = withContext(Dispatchers.IO) {
                            database.tripImageDao().getImagesByTripIdSync(id)
                        }
                        selectedImagePaths.clear()
                        selectedImagePaths.addAll(images.map { img -> img.imagePath })
                        selectedImageAdapter.submitList(selectedImagePaths.toList())
                        
                        if (it.latitude != null && it.longitude != null) {
                            currentLatitude = it.latitude
                            currentLongitude = it.longitude
                            currentLocationName = it.locationName
                            startDate = try { LocalDate.parse(it.date) } catch (e: Exception) { null }
                            endDate = try { it.endDate?.let { d -> LocalDate.parse(d) } } catch (e: Exception) { null }
                            updateLocationAndDateUI()
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

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Wypełnij tytuł i opis", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Wybierz cel podróży i daty", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val savedTripId = if (tripId != null) {
                    val existingTrip = withContext(Dispatchers.IO) {
                        database.tripDao().getTripById(tripId!!)
                    }
                    val updatedTrip = existingTrip?.copy(
                        title = title,
                        description = description,
                        date = startDate.toString(),
                        endDate = endDate.toString(),
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        locationName = currentLocationName
                    ) ?: return@launch
                    
                    withContext(Dispatchers.IO) {
                        database.tripDao().updateTrip(updatedTrip)
                        val oldImages = database.tripImageDao().getImagesByTripIdSync(tripId!!)
                        oldImages.forEach { image ->
                            if (!selectedImagePaths.contains(image.imagePath)) {
                                val file = java.io.File(image.imagePath)
                                if (file.exists()) file.delete()
                            }
                        }
                        database.tripImageDao().deleteImagesByTripId(tripId!!)
                    }
                    tripId!!
                } else {
                    val newTrip = TripEntity(
                        userEmail = currentUserEmail!!,
                        title = title,
                        description = description,
                        date = startDate.toString(),
                        endDate = endDate.toString(),
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        locationName = currentLocationName
                    )
                    withContext(Dispatchers.IO) {
                        database.tripDao().insertTrip(newTrip)
                    }
                }

                if (selectedImagePaths.isNotEmpty()) {
                    val imageEntities = selectedImagePaths.mapIndexed { index, path ->
                        TripImageEntity(tripId = savedTripId, imagePath = path, orderIndex = index)
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