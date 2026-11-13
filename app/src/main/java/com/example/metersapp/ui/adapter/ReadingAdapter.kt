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
    private var tariff: Double? = null
    private var tariffNight: Double? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun submitList(
        newItems: List<Reading>,
        type: MeterType,
        twoTariff: Boolean,
        tariff: Double?,
        tariffNight: Double?
    ) {
        items = newItems
        meterType = type
        isTwoTariff = twoTariff
        this.tariff = tariff
        this.tariffNight = tariffNight
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReadingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reading, parent, false)
        return ReadingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReadingViewHolder, position: Int) {
        val item = items[position]
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

            // Стоимость
            val cost = when {
                reading.value != null && previous?.value != null && tariff != null -> {
                    val diff = reading.value - previous.value
                    val sum = diff * tariff!!
                    "Стоимость: %.2f руб.".format(sum)
                }
                reading.valueDay != null && reading.valueNight != null &&
                        previous?.valueDay != null && previous.valueNight != null &&
                        tariff != null && tariffNight != null -> {
                    val diffDay = reading.valueDay - previous.valueDay
                    val diffNight = reading.valueNight - previous.valueNight
                    val sum = diffDay * tariff!! + diffNight * tariffNight!!
                    "Стоимость: %.2f руб.".format(sum)
                }
                else -> null
            }

            val fullText = buildString {
                if (consumption != null) append(consumption)
                if (cost != null) {
                    if (isNotEmpty()) append(" • ")
                    append(cost)
                }
            }

            consumptionText.text = fullText
            consumptionText.visibility = if (fullText.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }
}