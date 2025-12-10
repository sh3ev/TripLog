package com.example.triplog.ui.trips

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.databinding.ItemCalendarMonthBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class CalendarMonth(
    val yearMonth: YearMonth,
    val days: List<LocalDate?>
)

class CalendarMonthAdapter(
    private val months: List<CalendarMonth>,
    private var startDate: LocalDate?,
    private var endDate: LocalDate?,
    private val today: LocalDate,
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarMonthAdapter.ViewHolder>() {

    fun updateSelection(start: LocalDate?, end: LocalDate?) {
        startDate = start
        endDate = end
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarMonthBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(months[position])
    }

    override fun getItemCount() = months.size

    inner class ViewHolder(
        private val binding: ItemCalendarMonthBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(month: CalendarMonth) {
            // Nazwa miesiąca po polsku
            val monthName = month.yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))
                .replaceFirstChar { it.uppercase() }
            binding.textViewMonth.text = "$monthName ${month.yearMonth.year}"

            // Siatka dni
            binding.recyclerViewDays.layoutManager = GridLayoutManager(binding.root.context, 7)
            binding.recyclerViewDays.adapter = CalendarDayAdapter(
                days = month.days,
                startDate = startDate,
                endDate = endDate,
                today = today,
                onDayClick = onDayClick
            )
        }
    }
}
