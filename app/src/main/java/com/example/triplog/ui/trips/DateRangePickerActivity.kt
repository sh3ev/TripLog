package com.example.triplog.ui.trips

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.triplog.databinding.ActivityDateRangePickerBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class DateRangePickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOCATION_NAME = "location_name"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_START_DATE = "start_date"
        const val EXTRA_END_DATE = "end_date"
    }

    private lateinit var binding: ActivityDateRangePickerBinding
    private lateinit var calendarAdapter: CalendarMonthAdapter
    
    private var locationName: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null
    private val today = LocalDate.now()
    
    // Launcher dla podglądu pogody
    private val weatherPreviewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Przekaż wynik z powrotem do AddTripActivity
            setResult(RESULT_OK, result.data)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateRangePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pobierz dane lokalizacji
        locationName = intent.getStringExtra(EXTRA_LOCATION_NAME) ?: ""
        latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)

        binding.textViewLocation.text = locationName.split(",").firstOrNull() ?: locationName

        setupCalendar()
        setupClickListeners()
        updateUI()
    }

    private fun setupCalendar() {
        val months = generateMonths(12) // 12 miesięcy do przodu
        
        calendarAdapter = CalendarMonthAdapter(
            months = months,
            startDate = startDate,
            endDate = endDate,
            today = today,
            onDayClick = { date -> onDaySelected(date) }
        )

        binding.recyclerViewCalendar.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCalendar.adapter = calendarAdapter
    }

    private fun generateMonths(count: Int): List<CalendarMonth> {
        val months = mutableListOf<CalendarMonth>()
        // Zacznij od obecnego miesiąca (nie poprzedniego)
        var currentMonth = YearMonth.now()

        repeat(count) {
            val days = mutableListOf<LocalDate?>()
            val firstDayOfMonth = currentMonth.atDay(1)
            val lastDayOfMonth = currentMonth.atEndOfMonth()

            // Dodaj puste dni na początku (niedziela = 0, poniedziałek = 1, ...)
            // Java: SUNDAY=7, MONDAY=1, ...
            // Chcemy: SUNDAY=0, MONDAY=1, ...
            val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value % 7)
            repeat(firstDayOfWeek) { days.add(null) }

            // Dodaj dni miesiąca
            var day = firstDayOfMonth
            while (!day.isAfter(lastDayOfMonth)) {
                days.add(day)
                day = day.plusDays(1)
            }

            months.add(CalendarMonth(currentMonth, days))
            currentMonth = currentMonth.plusMonths(1)
        }

        return months
    }

    private fun onDaySelected(date: LocalDate) {
        when {
            startDate == null -> {
                // Pierwszy wybór - ustaw datę początkową
                startDate = date
                endDate = null
            }
            endDate == null -> {
                // Drugi wybór
                if (date.isBefore(startDate)) {
                    // Jeśli wybrano wcześniejszą datę, zamień
                    endDate = startDate
                    startDate = date
                } else if (date == startDate) {
                    // Kliknięto tę samą datę - ustaw jako pojedynczy dzień
                    endDate = date
                } else {
                    endDate = date
                }
            }
            else -> {
                // Reset - zacznij od nowa
                startDate = date
                endDate = null
            }
        }

        calendarAdapter.updateSelection(startDate, endDate)
        updateUI()
    }

    private fun updateUI() {
        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("pl"))
        
        binding.textViewSelectedRange.text = when {
            startDate != null && endDate != null && startDate == endDate -> {
                // Pojedynczy dzień
                startDate!!.format(formatter)
            }
            startDate != null && endDate != null -> {
                val start = startDate!!.format(formatter)
                val end = endDate!!.format(formatter)
                "$start - $end"
            }
            startDate != null -> {
                "Od ${startDate!!.format(formatter)}"
            }
            else -> "Wybierz daty podróży"
        }

        // Włącz przycisk gdy wybrano datę/zakres
        binding.buttonNext.isEnabled = startDate != null && endDate != null
        binding.buttonNext.alpha = if (binding.buttonNext.isEnabled) 1f else 0.5f
    }

    private fun setupClickListeners() {
        binding.imageViewClose.setOnClickListener {
            finish()
        }

        binding.textViewReset.setOnClickListener {
            startDate = null
            endDate = null
            calendarAdapter.updateSelection(null, null)
            updateUI()
        }

        binding.buttonNext.setOnClickListener {
            if (startDate != null && endDate != null) {
                // Otwórz podgląd pogody
                val intent = Intent(this, WeatherPreviewActivity::class.java).apply {
                    putExtra(WeatherPreviewActivity.EXTRA_LOCATION_NAME, locationName)
                    putExtra(WeatherPreviewActivity.EXTRA_LATITUDE, latitude)
                    putExtra(WeatherPreviewActivity.EXTRA_LONGITUDE, longitude)
                    putExtra(WeatherPreviewActivity.EXTRA_START_DATE, startDate.toString())
                    putExtra(WeatherPreviewActivity.EXTRA_END_DATE, endDate.toString())
                }
                weatherPreviewLauncher.launch(intent)
            }
        }
    }
}
