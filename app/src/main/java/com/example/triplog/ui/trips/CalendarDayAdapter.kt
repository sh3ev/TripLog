package com.example.triplog.ui.trips

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.triplog.R
import com.example.triplog.databinding.ItemCalendarDayBinding
import java.time.LocalDate

class CalendarDayAdapter(
    private val days: List<LocalDate?>,
    private val startDate: LocalDate?,
    private val endDate: LocalDate?,
    private val today: LocalDate,
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount() = days.size

    inner class ViewHolder(
        private val binding: ItemCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(date: LocalDate?) {
            if (date == null) {
                // Puste pole (dni przed początkiem miesiąca)
                binding.textViewDay.text = ""
                binding.viewSelectedBackground.visibility = View.GONE
                binding.viewRangeBackground.visibility = View.GONE
                binding.root.isClickable = false
                return
            }

            binding.textViewDay.text = date.dayOfMonth.toString()

            val isPast = date.isBefore(today)
            val isSelected = date == startDate || date == endDate
            val isInRange = startDate != null && endDate != null && 
                           date.isAfter(startDate) && date.isBefore(endDate)
            val isStart = date == startDate
            val isEnd = date == endDate

            // Reset
            binding.viewSelectedBackground.visibility = View.GONE
            binding.viewRangeBackground.visibility = View.GONE
            binding.textViewDay.setTextColor(
                if (isPast) 0xFF666666.toInt() else 0xFFFFFFFF.toInt()
            )

            when {
                isSelected -> {
                    binding.viewSelectedBackground.visibility = View.VISIBLE
                    binding.textViewDay.setTextColor(0xFFFFFFFF.toInt())
                    
                    // Pokaż tło zakresu po odpowiedniej stronie
                    if (startDate != null && endDate != null) {
                        binding.viewRangeBackground.visibility = View.VISIBLE
                        val lp = binding.viewRangeBackground.layoutParams as ViewGroup.MarginLayoutParams
                        if (isStart) {
                            lp.marginStart = binding.root.width / 2
                            lp.marginEnd = 0
                        } else if (isEnd) {
                            lp.marginStart = 0
                            lp.marginEnd = binding.root.width / 2
                        }
                        binding.viewRangeBackground.layoutParams = lp
                    }
                }
                isInRange -> {
                    binding.viewRangeBackground.visibility = View.VISIBLE
                    val lp = binding.viewRangeBackground.layoutParams as ViewGroup.MarginLayoutParams
                    lp.marginStart = 0
                    lp.marginEnd = 0
                    binding.viewRangeBackground.layoutParams = lp
                }
            }

            // Przekreślenie dat z przeszłości
            binding.textViewDay.paintFlags = if (isPast) {
                binding.textViewDay.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewDay.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.root.isClickable = !isPast
            binding.root.setOnClickListener {
                if (!isPast) {
                    onDayClick(date)
                }
            }
        }
    }
}
