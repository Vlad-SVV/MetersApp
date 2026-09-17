package com.example.metersapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.metersapp.ui.adapter.ObjectAdapter
import com.example.metersapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.objectsRecyclerView)
        val addButton = findViewById<Button>(R.id.addObjectButton)

        val adapter = ObjectAdapter { obj ->
            val intent = android.content.Intent(this, MeterListActivity::class.java)
            intent.putExtra(MeterListActivity.EXTRA_OBJECT_NAME, obj.name)
            intent.putExtra(MeterListActivity.EXTRA_OBJECT_ID, obj.id)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.objects.collect { list ->
                adapter.submitList(list)
            }
        }

        addButton.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Новый объект")
            .setMessage("Введите название (например, «Квартира» или «Дом»)")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addObject(name)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}