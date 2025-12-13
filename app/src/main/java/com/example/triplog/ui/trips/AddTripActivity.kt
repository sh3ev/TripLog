package com.example.triplog.ui.trips

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            saveImagesFromUris(uris)
        }
    }
    
    // Launcher dla uprawnień do zdjęć
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Brak dostępu do zdjęć", Toast.LENGTH_SHORT).show()
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

        // Przycisk wstecz
        binding.buttonBack.setOnClickListener { finish() }

        setupImageRecyclerView()
        setupLocationSelection()

        if (tripId != null) {
            binding.buttonSave.text = "Zaktualizuj"
            loadTripData()
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
        
        // Kliknięcie na wybraną lokalizację otwiera edycję
        binding.layoutSelectedLocation.setOnClickListener {
            val intent = Intent(this, LocationSearchActivity::class.java)
            locationSearchLauncher.launch(intent)
        }
    }
    
    private fun updateLocationAndDateUI() {
        if (currentLocationName != null && startDate != null && endDate != null) {
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("pl"))
            val dateRange = "${startDate!!.format(formatter)} - ${endDate!!.format(formatter)}"
            
            binding.textViewSelectedLocation.text = "$currentLocationName\n$dateRange"
            binding.layoutSelectedLocation.visibility = View.VISIBLE
            binding.layoutSelectLocation.visibility = View.GONE
        } else {
            binding.layoutSelectedLocation.visibility = View.GONE
            binding.layoutSelectLocation.visibility = View.VISIBLE
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
        selectedImageAdapter = SelectedImageAdapter(
            onRemoveClick = { imagePath -> removeImage(imagePath) },
            onAddClick = { checkPermissionAndOpenPicker() }
        )
        binding.recyclerViewSelectedImages.apply {
            adapter = selectedImageAdapter
            layoutManager = LinearLayoutManager(this@AddTripActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }
    
    private fun checkPermissionAndOpenPicker() {
        // Photo Picker (Android 11+) nie wymaga uprawnień
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - sprawdź READ_MEDIA_IMAGES
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openImagePicker()
                }
                else -> {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 - Photo Picker działa bez uprawnień
            openImagePicker()
        } else {
            // Android 10 i niżej - sprawdź READ_EXTERNAL_STORAGE
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openImagePicker()
                }
                else -> {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        }
    }
    
    private fun openImagePicker() {
        imagePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun removeImage(imagePath: String) {
        selectedImagePaths.remove(imagePath)
        selectedImageAdapter.submitList(selectedImagePaths.toList())
    }

    private fun saveImagesFromUris(uris: List<Uri>) {
        var savedCount = 0
        val imageDir = File(getExternalFilesDir(null), "trip_images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        
        uris.forEach { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val imageFile = File(imageDir, "trip_${System.currentTimeMillis()}_${savedCount}.jpg")
                val outputStream = FileOutputStream(imageFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                selectedImagePaths.add(imageFile.absolutePath)
                savedCount++
            } catch (e: Exception) {
                // Kontynuuj z pozostałymi zdjęciami
            }
        }
        
        selectedImageAdapter.submitList(selectedImagePaths.toList())
        
        if (savedCount > 0) {
            val message = if (savedCount == 1) "Dodano zdjęcie" else "Dodano $savedCount zdjęć"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                        // Zmień nagłówek na nazwę podróży
                        binding.textViewHeader.text = it.title
                        
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