package com.example.metersapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.metersapp.R
import com.example.metersapp.data.entity.Meter
import com.example.metersapp.data.entity.MeterType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeterAdapter(
    private val onClick: (Meter) -> Unit
) : RecyclerView.Adapter<MeterAdapter.MeterViewHolder>() {

    private var items: List<Meter> = emptyList()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun submitList(newItems: List<Meter>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meter, parent, false)
        return MeterViewHolder(view)
    }

    override fun onBindViewHolder(holder: MeterViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    inner class MeterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.meterName)
        private val typeText: TextView = itemView.findViewById(R.id.meterType)
        private val serialText: TextView = itemView.findViewById(R.id.meterSerial)
        private val verificationText: TextView = itemView.findViewById(R.id.meterVerification)


        fun bind(meter: Meter) {
            nameText.text = meter.name

            val (iconRes, colorHex) = when (meter.type) {
                MeterType.COLD_WATER -> Pair(R.drawable.ic_cold_water, "#2196F3") // Синий
                MeterType.HOT_WATER -> Pair(R.drawable.ic_hot_water, "#F44336")  // Красный
                MeterType.ELECTRICITY -> Pair(R.drawable.ic_electricity, "#FF9800") // Оранжевый
            }

            itemView.findViewById<android.widget.ImageView>(R.id.meterIcon).setImageResource(iconRes)
            itemView.findViewById<android.widget.ImageView>(R.id.meterIcon).setColorFilter(android.graphics.Color.parseColor(colorHex))

            typeText.text = when (meter.type) {
                MeterType.COLD_WATER -> "Холодная вода"
                MeterType.HOT_WATER -> "Горячая вода"
                MeterType.ELECTRICITY -> if (meter.isTwoTariff) "Электро (день/ночь)" else "Электроэнергия"
            }
            serialText.text = "Заводской №: ${meter.serialNumber}"
            val endDate = dateFormat.format(Date(meter.verificationEndDate))
            verificationText.text = "Поверка до: $endDate"
        }
    }
}