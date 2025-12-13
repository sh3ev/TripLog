package com.example.triplog.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import com.example.triplog.ui.trips.DateRangePickerActivity
import com.example.triplog.ui.trips.TripAdapter
import com.example.triplog.ui.trips.TripDetailsActivity
import com.example.triplog.utils.SharedPreferencesHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val database by lazy { AppDatabase.getDatabase(this) }
    private var currentUserEmail: String? = null
    private lateinit var tripAdapter: TripAdapter
    private var searchJob: Job? = null
    private var backPressedTime: Long = 0
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    
    private var allTrips: List<TripEntity> = emptyList()
    private var currentTabPosition: Int = 0
    private var isSearchMode: Boolean = false

    private val profileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadUserProfile()
        }
    }
    
    private val dateEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val startDate = result.data?.getStringExtra(DateRangePickerActivity.EXTRA_START_DATE)
            val endDate = result.data?.getStringExtra(DateRangePickerActivity.EXTRA_END_DATE)
            val tripId = result.data?.getLongExtra(DateRangePickerActivity.EXTRA_TRIP_ID, 0L) ?: 0L
            
            if (startDate != null && endDate != null && tripId > 0) {
                updateTripDates(tripId, startDate, endDate)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen - mapa pod status barem
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserEmail = SharedPreferencesHelper.getLoggedInUser(this)
        if (currentUserEmail == null) {
            navigateToLogin()
            return
        }

        setupMap()
        setupBottomSheet()
        setupTabs()
        setupRecyclerView()
        setupSearchBar()
        setupNavigation()
        loadUserProfile()
        loadTrips()
    }
    
    private fun setupTabs() {
        // Dodaj zakładki
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_upcoming))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_current))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_past))
        
        // Domyślnie wybierz "Nadchodzące"
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                filterTripsByCategory()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMap() {
        binding.webViewMap.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            
            val mapHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                    <style>
                        body { margin: 0; padding: 0; }
                        #map { width: 100%; height: 100vh; }
                        .leaflet-control-attribution { display: none; }
                        .leaflet-control-zoom { display: none; }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <script>
                        var map = L.map('map', {
                            zoomControl: false,
                            attributionControl: false
                        }).setView([30, 0], 2);
                        
                        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                            maxZoom: 19
                        }).addTo(map);
                    </script>
                </body>
                </html>
            """.trimIndent()
            
            loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null)
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetContainer)
        
        // Trzy stany: collapsed (schowany na dole), half-expanded (środek), expanded (góra)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        
        // peekHeight = tylko avatar i statystyki nad bottom nav (72dp nav + ~100dp content)
        val bottomNavHeight = (72 * density).toInt()
        val collapsedContentHeight = (110 * density).toInt()
        bottomSheetBehavior.peekHeight = bottomNavHeight + collapsedContentHeight
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.halfExpandedRatio = 0.55f // środkowa pozycja na 55% ekranu
        
        // Stan początkowy: środek (half-expanded)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        
        // Pobierz status bar height
        val statusBarHeight = getStatusBarHeight()
        
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        // W pełni rozwinięty - pokaż wyszukiwarkę, ukryj avatar i statystyki
                        binding.searchSection.visibility = View.VISIBLE
                        binding.profileHeader.visibility = View.GONE
                        binding.userNameSection.visibility = View.GONE
                        binding.imageViewAvatar.visibility = View.GONE
                        binding.tabLayout.visibility = View.GONE
                        // Ustaw padding top dla status bara
                        binding.bottomSheet.setPadding(0, statusBarHeight, 0, 0)
                        (binding.bottomSheet.layoutParams as FrameLayout.LayoutParams).topMargin = 0
                        
                        // Przełącz na listę pionową
                        setRecyclerViewMode(isHorizontal = false)
                        
                        // Tryb wyszukiwania - pokazuj pustą listę dopóki nie wpiszesz zapytania
                        isSearchMode = true
                        binding.editTextSearch.text.clear()
                        tripAdapter.submitList(emptyList())
                        updateEmptyState(true)
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        // Środek - pokaż avatar i statystyki, ukryj wyszukiwarkę
                        binding.searchSection.visibility = View.GONE
                        binding.profileHeader.visibility = View.VISIBLE
                        binding.userNameSection.visibility = View.VISIBLE
                        binding.imageViewAvatar.visibility = View.VISIBLE
                        binding.tabLayout.visibility = View.VISIBLE
                        // Przywróć margin dla avatara
                        binding.bottomSheet.setPadding(0, 0, 0, 0)
                        (binding.bottomSheet.layoutParams as FrameLayout.LayoutParams).topMargin = (32 * density).toInt()
                        
                        // Przełącz na karuzelę horyzontalną
                        setRecyclerViewMode(isHorizontal = true)
                        
                        // Wyjście z trybu wyszukiwania - przywróć filtrowaną listę
                        if (isSearchMode) {
                            isSearchMode = false
                            filterTripsByCategory()
                        }
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // Na dole - pokazuj tylko avatar i statystyki
                        binding.searchSection.visibility = View.GONE
                        binding.profileHeader.visibility = View.VISIBLE
                        binding.userNameSection.visibility = View.GONE
                        binding.imageViewAvatar.visibility = View.VISIBLE
                        binding.tabLayout.visibility = View.GONE
                        // Przywróć margin dla avatara
                        binding.bottomSheet.setPadding(0, 0, 0, 0)
                        (binding.bottomSheet.layoutParams as FrameLayout.LayoutParams).topMargin = (32 * density).toInt()
                        
                        // W trybie collapsed też karuzela horyzontalna
                        setRecyclerViewMode(isHorizontal = true)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // slideOffset: 0 = collapsed, 0.5 = half-expanded, 1 = expanded
                if (slideOffset >= 0.5f) {
                    // Przejście half-expanded -> expanded
                    val expandProgress = (slideOffset - 0.5f) * 2 // 0 to 1
                    binding.searchSection.alpha = expandProgress
                    binding.profileHeader.alpha = 1 - expandProgress
                    binding.userNameSection.alpha = 1 - expandProgress
                    binding.imageViewAvatar.alpha = 1 - expandProgress
                    binding.tabLayout.alpha = 1 - expandProgress
                    
                    if (expandProgress > 0.3f) {
                        binding.searchSection.visibility = View.VISIBLE
                    }
                    
                    // Płynna zmiana marginu i paddingu
                    val margin = ((1 - expandProgress) * 32 * density).toInt()
                    val padding = (expandProgress * statusBarHeight).toInt()
                    binding.bottomSheet.setPadding(0, padding, 0, 0)
                    (binding.bottomSheet.layoutParams as FrameLayout.LayoutParams).topMargin = margin
                    binding.bottomSheet.requestLayout()
                } else {
                    // Przejście collapsed -> half-expanded
                    val halfProgress = slideOffset * 2 // 0 to 1
                    binding.searchSection.visibility = View.GONE
                    binding.profileHeader.alpha = 1f
                    binding.imageViewAvatar.alpha = 1f
                    binding.userNameSection.alpha = halfProgress
                    binding.tabLayout.alpha = halfProgress
                }
            }
        })
    }
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun setupNavigation() {
        // Kliknięcie w avatar - otwiera profil
        binding.imageViewAvatar.setOnClickListener {
            navigateToProfile()
        }

        // Bottom Navigation
        binding.navProfile.setOnClickListener {
            navigateToProfile()
        }

        binding.navAddTrip.setOnClickListener {
            navigateToAddTrip()
        }

        binding.navSettings.setOnClickListener {
            Toast.makeText(this, "Ustawienia - wkrótce", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                currentUserEmail?.let { database.userDao().getUserByEmail(it) }
            }
            user?.let {
                // Ustaw imię użytkownika
                val displayName = when {
                    !it.firstName.isNullOrBlank() -> {
                        if (!it.lastName.isNullOrBlank()) "${it.firstName} ${it.lastName}"
                        else it.firstName
                    }
                    else -> it.name
                }
                binding.textViewUserName.text = displayName
                binding.textViewUserEmail.text = it.email

                // Załaduj zdjęcie profilowe lub placeholder
                if (!it.profileImagePath.isNullOrBlank()) {
                    val file = File(it.profileImagePath!!)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(it.profileImagePath)
                        val rotatedBitmap = rotateBitmapIfRequired(bitmap, it.profileImagePath!!)
                        binding.imageViewAvatar.setImageBitmap(rotatedBitmap)
                    } else {
                        binding.imageViewAvatar.setImageResource(R.drawable.ic_person_placeholder)
                    }
                } else {
                    binding.imageViewAvatar.setImageResource(R.drawable.ic_person_placeholder)
                }
            }
        }
    }

    private fun setupSearchBar() {
        binding.imageViewClearSearch.setOnClickListener {
            binding.editTextSearch.text.clear()
        }

        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.imageViewClearSearch.visibility = 
                    if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
                
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
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
                    // W trybie wyszukiwania - pusta lista gdy brak zapytania
                    if (isSearchMode) {
                        tripAdapter.submitList(emptyList())
                        updateEmptyState(true)
                    } else {
                        database.tripDao().getTripsByUser(email).collect { trips ->
                            allTrips = trips
                            filterTripsByCategory()
                            binding.textViewTripsCount.text = trips.size.toString()
                        }
                    }
                } else {
                    database.tripDao().searchTrips(email, query).collect { trips ->
                        // W trybie wyszukiwania - pokaż wszystkie wyniki bez filtrowania
                        if (isSearchMode) {
                            tripAdapter.submitList(trips)
                            updateEmptyState(trips.isEmpty())
                            updateRecyclerViewPadding(trips.size)
                        } else {
                            allTrips = trips
                            filterTripsByCategory()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
        val currentQuery = binding.editTextSearch.text.toString()
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
            onItemClick = { trip -> navigateToTripDetails(trip) },
            onItemLongClick = { },
            onEditClick = { trip -> navigateToEditTrip(trip) },
            onDeleteClick = { trip -> showDeleteConfirmationDialog(trip) },
            onCalendarClick = { trip -> showEditDatesDialog(trip) }
        )
        binding.recyclerViewTrips.apply {
            adapter = tripAdapter
            // Domyślnie tryb horyzontalny (half-expanded)
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            tripAdapter.isHorizontalMode = true
        }
    }
    
    private fun setRecyclerViewMode(isHorizontal: Boolean) {
        if (tripAdapter.isHorizontalMode == isHorizontal) return
        
        // Zapisz aktualną listę
        val currentList = tripAdapter.currentList.toList()
        
        // Zmień tryb adaptera
        tripAdapter.isHorizontalMode = isHorizontal
        
        // Zmień LayoutManager
        binding.recyclerViewTrips.layoutManager = if (isHorizontal) {
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        } else {
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        }
        
        // Przywróć listę i zaktualizuj padding
        tripAdapter.submitList(currentList)
        updateRecyclerViewPadding(currentList.size)
    }

    private fun loadTrips() {
        currentUserEmail?.let { email ->
            lifecycleScope.launch {
                database.tripDao().getTripsByUser(email).collect { trips ->
                    allTrips = trips
                    filterTripsByCategory()
                    binding.textViewTripsCount.text = trips.size.toString()
                    
                    // Policz unikalne kraje
                    val countries = trips.mapNotNull { it.locationName }
                        .map { it.substringAfterLast(",").trim() }
                        .filter { it.isNotBlank() }
                        .toSet()
                    binding.textViewCountriesCount.text = countries.size.toString()
                }
            }
        }
    }
    
    private fun filterTripsByCategory() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        val filteredTrips = allTrips.filter { trip ->
            try {
                val startDate = LocalDate.parse(trip.date, formatter)
                val endDate = if (!trip.endDate.isNullOrBlank()) {
                    LocalDate.parse(trip.endDate, formatter)
                } else {
                    startDate
                }
                
                when (currentTabPosition) {
                    0 -> startDate.isAfter(today) // Nadchodzące - zaczynają się w przyszłości
                    1 -> !startDate.isAfter(today) && !endDate.isBefore(today) // Aktualne - trwające teraz
                    2 -> endDate.isBefore(today) // Przeszłe - już zakończone
                    else -> true
                }
            } catch (e: Exception) {
                currentTabPosition == 2 // W razie błędu parsowania, traktuj jako przeszłe
            }
        }
        
        tripAdapter.submitList(filteredTrips)
        updateEmptyState(filteredTrips.isEmpty())
        updateRecyclerViewPadding(filteredTrips.size)
    }
    
    private fun updateRecyclerViewPadding(itemCount: Int) {
        // W trybie horyzontalnym, jeśli jest tylko jedna karta, wycentruj ją
        if (tripAdapter.isHorizontalMode && itemCount == 1) {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val cardWidth = (300 * displayMetrics.density).toInt() // 300dp szerokość karty
            val horizontalPadding = (screenWidth - cardWidth) / 2
            binding.recyclerViewTrips.setPadding(
                horizontalPadding.coerceAtLeast(0),
                0,
                horizontalPadding.coerceAtLeast(0),
                (100 * displayMetrics.density).toInt()
            )
        } else {
            val density = resources.displayMetrics.density
            binding.recyclerViewTrips.setPadding(
                (20 * density).toInt(),
                0,
                (20 * density).toInt(),
                (100 * density).toInt()
            )
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewTrips.visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        if (isEmpty) {
            // W trybie wyszukiwania - specjalny komunikat
            if (isSearchMode) {
                val searchQuery = binding.editTextSearch.text.toString()
                val (title, hint, iconRes) = if (searchQuery.isBlank()) {
                    Triple(
                        getString(R.string.search_trips),
                        getString(R.string.search_empty_hint),
                        R.drawable.ic_search
                    )
                } else {
                    Triple(
                        getString(R.string.no_results),
                        getString(R.string.no_results_hint),
                        R.drawable.ic_search
                    )
                }
                binding.textViewEmptyTitle.text = title
                binding.textViewEmptyHint.text = hint
                binding.imageViewEmptyIcon.setImageResource(iconRes)
            } else {
                // Ustaw tekst i ikonę w zależności od wybranej zakładki
                val (title, hint, iconRes) = when (currentTabPosition) {
                    0 -> Triple(
                        getString(R.string.empty_upcoming),
                        getString(R.string.empty_upcoming_hint),
                        R.drawable.ic_empty_trips
                    )
                    1 -> Triple(
                        getString(R.string.empty_current),
                        getString(R.string.empty_current_hint),
                        R.drawable.ic_empty_trips
                    )
                    2 -> Triple(
                        getString(R.string.empty_past),
                        getString(R.string.empty_past_hint),
                        R.drawable.ic_empty_trips
                    )
                    else -> Triple(
                        getString(R.string.all_trips_in_one_place),
                        getString(R.string.press_plus_to_add_trip),
                        R.drawable.ic_empty_trips
                    )
                }
                binding.textViewEmptyTitle.text = title
                binding.textViewEmptyHint.text = hint
                binding.imageViewEmptyIcon.setImageResource(iconRes)
            }
        }
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

    private fun showEditDatesDialog(trip: TripEntity) {
        // Otwórz DateRangePickerActivity w trybie edycji
        val intent = Intent(this, DateRangePickerActivity::class.java).apply {
            putExtra(DateRangePickerActivity.EXTRA_EDIT_MODE, true)
            putExtra(DateRangePickerActivity.EXTRA_TRIP_ID, trip.id)
            putExtra(DateRangePickerActivity.EXTRA_START_DATE, trip.date)
            putExtra(DateRangePickerActivity.EXTRA_END_DATE, trip.endDate ?: trip.date)
            putExtra(DateRangePickerActivity.EXTRA_LOCATION_NAME, trip.title)
        }
        dateEditLauncher.launch(intent)
    }
    
    private fun updateTripDates(tripId: Long, startDate: String, endDate: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val trip = database.tripDao().getTripById(tripId)
                trip?.let {
                    val updatedTrip = it.copy(date = startDate, endDate = endDate)
                    database.tripDao().updateTrip(updatedTrip)
                }
            }
            Toast.makeText(this@MainActivity, "Daty zaktualizowane", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(trip: TripEntity) {
        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle("Usuń podróż")
            .setMessage("Czy na pewno chcesz usunąć podróż \"${trip.title}\"?")
            .setPositiveButton("Usuń") { _, _ -> deleteTrip(trip) }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun deleteTrip(trip: TripEntity) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val images = database.tripImageDao().getImagesByTripIdSync(trip.id)
                    images.forEach { image ->
                        val file = File(image.imagePath)
                        if (file.exists()) file.delete()
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
        // Jeśli bottom sheet jest rozwinięty, wróć do half-expanded
        when (bottomSheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                return
            }
            BottomSheetBehavior.STATE_COLLAPSED -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                return
            }
        }
        
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
