package com.example.metersapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.metersapp.data.entity.MeterType
import com.example.metersapp.ui.adapter.MeterAdapter
import com.example.metersapp.ui.viewmodel.MeterListViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MeterListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OBJECT_NAME = "extra_object_name"
        const val EXTRA_OBJECT_ID = "extra_object_id"
    }

    private val viewModel: MeterListViewModel by viewModels()
    private lateinit var adapter: MeterAdapter
    private var objectId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meter_list)

        val objectName = intent.getStringExtra(EXTRA_OBJECT_NAME) ?: "Объект"
        objectId = intent.getLongExtra(EXTRA_OBJECT_ID, 0)

        // Название объекта
        findViewById<TextView>(R.id.objectNameText).text = objectName

        // Кнопка «Назад»
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.metersRecyclerView)
        val emptyText = findViewById<TextView>(R.id.emptyText)
        val addButton = findViewById<Button>(R.id.addMeterButton)

        adapter = MeterAdapter { _ ->
            // TODO: открыть экран показаний (следующий этап)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.setObjectId(objectId)

        lifecycleScope.launch {
            viewModel.meters.collect { list ->
                adapter.submitList(list)
                emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        addButton.setOnClickListener {
            showAddMeterDialog()
        }
    }

    private fun showAddMeterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_meter, null)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.typeSpinner)
        val nameInput = dialogView.findViewById<EditText>(R.id.nameInput)
        val serialInput = dialogView.findViewById<EditText>(R.id.serialInput)
        val verificationDateInput = dialogView.findViewById<EditText>(R.id.verificationDateInput)
        val verificationEndDateInput = dialogView.findViewById<EditText>(R.id.verificationEndDateInput)
        val twoTariffCheckbox = dialogView.findViewById<CheckBox>(R.id.twoTariffCheckbox)

        val types = listOf("Холодная вода", "Горячая вода", "Электроэнергия")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = spinnerAdapter

        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                twoTariffCheckbox.visibility = if (position == 2) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                twoTariffCheckbox.visibility = View.GONE
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Новый счётчик")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val type = when (typeSpinner.selectedItemPosition) {
                    0 -> MeterType.COLD_WATER
                    1 -> MeterType.HOT_WATER
                    else -> MeterType.ELECTRICITY
                }
                val name = nameInput.text.toString().trim()
                val serial = serialInput.text.toString().trim()

                if (name.isEmpty() || serial.isEmpty()) {
                    return@setPositiveButton
                }

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val verificationDate = try {
                    dateFormat.parse(verificationDateInput.text.toString())?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
                val verificationEndDate = try {
                    dateFormat.parse(verificationEndDateInput.text.toString())?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                viewModel.addMeter(
                    objectId = objectId,
                    type = type,
                    name = name,
                    serialNumber = serial,
                    verificationDate = verificationDate,
                    verificationEndDate = verificationEndDate,
                    isTwoTariff = twoTariffCheckbox.isChecked
                )
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}