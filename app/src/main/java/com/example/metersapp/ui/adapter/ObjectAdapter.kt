package com.example.metersapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.metersapp.R
import com.example.metersapp.data.entity.MeterObject

class ObjectAdapter(
    private val onClick: (MeterObject) -> Unit
) : RecyclerView.Adapter<ObjectAdapter.ObjectViewHolder>() {

    private var items: List<MeterObject> = emptyList()

    fun submitList(newItems: List<MeterObject>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_object, parent, false)
        return ObjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ObjectViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class ObjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.objectName)

        fun bind(obj: MeterObject) {
            nameText.text = obj.name
        }
    }
}