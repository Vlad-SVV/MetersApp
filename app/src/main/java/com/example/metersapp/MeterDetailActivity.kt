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

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import java.io.OutputStreamWriter


class MeterDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_METER_ID = "extra_meter_id"
    }

    private val viewModel: MeterDetailViewModel by viewModels()
    private lateinit var adapter: ReadingAdapter
    private var meterId: Long = 0
    private var currentMeter: Meter? = null

    private val createCsvLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            writeCsvToUri(uri)
        }
    }

    private val openCsvLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.importReadings(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meter_detail)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            insets
        }


        meterId = intent.getLongExtra(EXTRA_METER_ID, 0)

        // Кнопка «Назад»
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Кнопка «Изменить тариф»
        findViewById<Button>(R.id.tariffButton).setOnClickListener {
            showTariffDialog()
        }

        // Кнопка «Выгрузить данные»
        findViewById<Button>(R.id.exportButton).setOnClickListener {
            val meter = currentMeter
            if (meter != null) {
                // Предлагаем имя файла: Meter_Кухня_2026-11-13.csv
                val fileName = "Meter_${meter.name}_${System.currentTimeMillis()}.csv"
                createCsvLauncher.launch(fileName)
            }
        }

        findViewById<Button>(R.id.importButton).setOnClickListener {
            // Открываем выбор файла, разрешая любые текстовые форматы
            openCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain", "*/*"))
        }

        lifecycleScope.launch {
            viewModel.importStatus.collect { message ->
                if (message != null) {
                    android.widget.Toast.makeText(this@MeterDetailActivity, message, android.widget.Toast.LENGTH_LONG).show()
                    viewModel.clearImportStatus()
                }
            }
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
                    adapter.submitList(list, meter.type, meter.isTwoTariff, meter.tariff, meter.tariffNight)
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

        // Настраиваем цвет и текст для поверки
        val verificationTextView = findViewById<TextView>(R.id.meterVerificationText)
        val verificationEndDate = Date(meter.verificationEndDate)
        val verificationStartDate = Date(meter.verificationDate)

        val currentTime = System.currentTimeMillis()
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000

        val text = "Поверка: ${dateFormat.format(verificationStartDate)} — ${dateFormat.format(verificationEndDate)}"

        when {
            meter.verificationEndDate < currentTime -> {
                verificationTextView.text = "$text\n⚠️ ПОВЕРКА ПРОСРОЧЕНА!"
                verificationTextView.setTextColor(android.graphics.Color.RED)
            }
            meter.verificationEndDate - currentTime < thirtyDaysInMillis -> {
                verificationTextView.text = "$text\n⚠️ Скоро заканчивается!"
                verificationTextView.setTextColor(android.graphics.Color.parseColor("#FF9800")) // Оранжевый
            }
            else -> {
                verificationTextView.text = text
                verificationTextView.setTextColor(android.graphics.Color.parseColor("#666666")) // Обычный серый
            }
        }
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
    private fun showTariffDialog() {
        val meter = currentMeter
        if (meter == null) {
            android.widget.Toast.makeText(this, "Счётчик ещё загружается, подождите", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        android.util.Log.d("TARIFF", "Открываю тариф для ${meter.name}, tariff=${meter.tariff}")

        val dialogView = layoutInflater.inflate(R.layout.dialog_tariff, null)
        val tariffInput = dialogView.findViewById<EditText>(R.id.tariffInput)
        val tariffNightInput = dialogView.findViewById<EditText>(R.id.tariffNightInput)

        tariffInput.setText(meter.tariff?.toString() ?: "")
        if (meter.isTwoTariff) {
            tariffNightInput.visibility = View.VISIBLE
            tariffNightInput.setText(meter.tariffNight?.toString() ?: "")
        } else {
            tariffNightInput.visibility = View.GONE
        }

        AlertDialog.Builder(this)
            .setTitle("Тариф")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val tariff = tariffInput.text.toString().replace(',', '.').toDoubleOrNull()
                val tariffNight = if (meter.isTwoTariff) {
                    tariffNightInput.text.toString().replace(',', '.').toDoubleOrNull()
                } else null

                viewModel.updateMeter(meter.copy(tariff = tariff, tariffNight = tariffNight))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun writeCsvToUri(uri: Uri) {
        val meter = currentMeter ?: return
        val readings = adapter.getItems() // Нам понадобится метод в адаптере
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream)
                // Заголовки
                writer.write("Дата;Показание;Показание день;Показание ночь;Расход;Стоимость\n")

                // Строки
                for (i in readings.indices) {
                    val r = readings[i]
                    val prev = readings.getOrNull(i + 1) // Список отсортирован по убыванию даты
                    val dateStr = dateFormat.format(Date(r.date))

                    var consumption = ""
                    var cost = ""

                    if (r.value != null && prev?.value != null) {
                        val diff = r.value - prev.value
                        consumption = "%.2f".format(diff).replace(',', '.')
                        if (meter.tariff != null) {
                            cost = "%.2f".format(diff * meter.tariff).replace(',', '.')
                        }
                    } else if (r.valueDay != null && r.valueNight != null && prev?.valueDay != null && prev.valueNight != null) {
                        val diffDay = r.valueDay - prev.valueDay
                        val diffNight = r.valueNight - prev.valueNight
                        consumption = "День: %.2f, Ночь: %.2f".format(diffDay, diffNight).replace(',', '.')
                        if (meter.tariff != null && meter.tariffNight != null) {
                            cost = "%.2f".format(diffDay * meter.tariff + diffNight * meter.tariffNight).replace(',', '.')
                        }
                    }

                    writer.write("$dateStr;${r.value ?: ""};${r.valueDay ?: ""};${r.valueNight ?: ""};$consumption;$cost\n")
                }
                writer.flush()
            }
            android.widget.Toast.makeText(this, "Файл сохранён!", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("CSV", "Ошибка экспорта", e)
            android.widget.Toast.makeText(this, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
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