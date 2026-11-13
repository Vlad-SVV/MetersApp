package com.example.metersapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.metersapp.data.entity.Meter
import com.example.metersapp.data.entity.MeterType
import com.example.metersapp.data.entity.Reading
import com.example.metersapp.ui.adapter.ReadingAdapter
import com.example.metersapp.ui.viewmodel.MeterDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MeterDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_METER_ID = "extra_meter_id"
    }

    private val viewModel: MeterDetailViewModel by viewModels()
    private lateinit var adapter: ReadingAdapter
    private var meterId: Long = 0
    private var currentMeter: Meter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meter_detail)

        meterId = intent.getLongExtra(EXTRA_METER_ID, 0)

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.readingsRecyclerView)
        val emptyText = findViewById<TextView>(R.id.emptyText)
        val addReadingButton = findViewById<Button>(R.id.addReadingButton)

        adapter = ReadingAdapter { reading ->
            showReadingActionsDialog(reading)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.setMeterId(meterId)

        lifecycleScope.launch {
            viewModel.meter.collect { meter ->
                meter?.let {
                    currentMeter = it
                    bindMeterInfo(it)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.readings.collect { list ->
                val meter = currentMeter
                if (meter != null) {
                    adapter.submitList(list, meter.type, meter.isTwoTariff)
                }
                emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        addReadingButton.setOnClickListener {
            showReadingDialog(null)
        }
    }

    private fun bindMeterInfo(meter: Meter) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        findViewById<TextView>(R.id.meterNameText).text = meter.name
        findViewById<TextView>(R.id.meterTypeText).text = when (meter.type) {
            MeterType.COLD_WATER -> "Холодная вода"
            MeterType.HOT_WATER -> "Горячая вода"
            MeterType.ELECTRICITY ->
                if (meter.isTwoTariff) "Электро (день/ночь)" else "Электроэнергия"
        }
        findViewById<TextView>(R.id.meterSerialText).text = "Заводской №: ${meter.serialNumber}"
        findViewById<TextView>(R.id.meterVerificationText).text =
            "Поверка: ${dateFormat.format(Date(meter.verificationDate))} — ${dateFormat.format(Date(meter.verificationEndDate))}"
    }

    /** Диалог «Что сделать?» при клике на показание */
    private fun showReadingActionsDialog(reading: Reading) {
        val options = arrayOf("Редактировать", "Удалить")
        AlertDialog.Builder(this)
            .setTitle("Показание")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showReadingDialog(reading)
                    1 -> confirmDelete(reading)
                }
            }
            .show()
    }

    /** Подтверждение удаления */
    private fun confirmDelete(reading: Reading) {
        AlertDialog.Builder(this)
            .setTitle("Удалить показание?")
            .setMessage("Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteReading(reading)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Диалог добавления/редактирования показания.
     * Если [existing] == null — добавление, иначе — редактирование.
     */
    private fun showReadingDialog(existing: Reading?) {
        val meter = currentMeter ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reading, null)
        val dateInput = dialogView.findViewById<EditText>(R.id.dateInput)
        val valueInput = dialogView.findViewById<EditText>(R.id.valueInput)
        val valueDayInput = dialogView.findViewById<EditText>(R.id.valueDayInput)
        val valueNightInput = dialogView.findViewById<EditText>(R.id.valueNightInput)

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // Показываем нужные поля
        if (meter.isTwoTariff) {
            valueInput.visibility = View.GONE
            valueDayInput.visibility = View.VISIBLE
            valueNightInput.visibility = View.VISIBLE
        } else {
            valueInput.visibility = View.VISIBLE
            valueDayInput.visibility = View.GONE
            valueNightInput.visibility = View.GONE
        }

        // Если редактируем — предзаполняем поля
        if (existing != null) {
            dateInput.setText(dateFormat.format(Date(existing.date)))
            if (meter.isTwoTariff) {
                valueDayInput.setText(existing.valueDay?.toString() ?: "")
                valueNightInput.setText(existing.valueNight?.toString() ?: "")
            } else {
                valueInput.setText(existing.value?.toString() ?: "")
            }
        } else {
            dateInput.setText(dateFormat.format(Date()))
        }

        // Календарь по клику на дату
        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (existing != null) {
                calendar.timeInMillis = existing.date
            }
            DatePickerDialog(this, { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                dateInput.setText(dateFormat.format(cal.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        val title = if (existing == null) "Новое показание" else "Редактировать показание"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val date = try {
                    dateFormat.parse(dateInput.text.toString())?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                if (meter.isTwoTariff) {
                    val day = valueDayInput.text.toString().replace(',', '.').toDoubleOrNull()
                    val night = valueNightInput.text.toString().replace(',', '.').toDoubleOrNull()
                    if (day != null && night != null) {
                        if (existing == null) {
                            viewModel.addReading(date, null, day, night)
                        } else {
                            viewModel.updateReading(
                                existing.copy(date = date, value = null, valueDay = day, valueNight = night)
                            )
                        }
                    }
                } else {
                    val value = valueInput.text.toString().replace(',', '.').toDoubleOrNull()
                    if (value != null) {
                        if (existing == null) {
                            viewModel.addReading(date, value, null, null)
                        } else {
                            viewModel.updateReading(
                                existing.copy(date = date, value = value, valueDay = null, valueNight = null)
                            )
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}