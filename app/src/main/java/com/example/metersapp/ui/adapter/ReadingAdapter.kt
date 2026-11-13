package com.example.metersapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.metersapp.R
import com.example.metersapp.data.entity.MeterType
import com.example.metersapp.data.entity.Reading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReadingAdapter(
    private val onClick: (Reading) -> Unit
) : RecyclerView.Adapter<ReadingAdapter.ReadingViewHolder>() {

    private var items: List<Reading> = emptyList()
    private var meterType: MeterType = MeterType.COLD_WATER
    private var isTwoTariff: Boolean = false
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun submitList(newItems: List<Reading>, type: MeterType, twoTariff: Boolean) {
        items = newItems
        meterType = type
        isTwoTariff = twoTariff
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReadingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reading, parent, false)
        return ReadingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReadingViewHolder, position: Int) {
        val item = items[position]
        // Предыдущее (более старое) показание — следующий элемент в списке (список DESC)
        val previous = items.getOrNull(position + 1)
        holder.bind(item, previous)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    inner class ReadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.readingDate)
        private val valueText: TextView = itemView.findViewById(R.id.readingValue)
        private val consumptionText: TextView = itemView.findViewById(R.id.readingConsumption)

        fun bind(reading: Reading, previous: Reading?) {
            dateText.text = dateFormat.format(Date(reading.date))

            val valueStr = when {
                reading.value != null -> "${reading.value}"
                reading.valueDay != null && reading.valueNight != null ->
                    "День: ${reading.valueDay}, Ночь: ${reading.valueNight}"
                else -> "—"
            }
            valueText.text = valueStr

            // Расчёт расхода
            val consumption = when {
                reading.value != null && previous?.value != null -> {
                    val diff = reading.value - previous.value
                    "Расход: %.2f".format(diff)
                }
                reading.valueDay != null && reading.valueNight != null &&
                        previous?.valueDay != null && previous.valueNight != null -> {
                    val diffDay = reading.valueDay - previous.valueDay
                    val diffNight = reading.valueNight - previous.valueNight
                    "Расход: день %.2f, ночь %.2f".format(diffDay, diffNight)
                }
                else -> null
            }

            consumptionText.text = consumption ?: ""
            consumptionText.visibility = if (consumption != null) View.VISIBLE else View.GONE
        }
    }
}