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
import com.example.metersapp.data.entity.MeterObject
import com.example.metersapp.ui.adapter.ObjectAdapter
import com.example.metersapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

import com.example.metersapp.utils.AppSettings

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = AppSettings(this)

// СЕКРЕТНЫЙ ПЕРЕКЛЮЧАТЕЛЬ (зажать на 3 секунды)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val proRunnable = Runnable {
            // Этот код сработает, только если продержать палец 3 секунды
            settings.isProVersion = !settings.isProVersion
            val status = if (settings.isProVersion) "ВКЛЮЧЕН (Безлимит)" else "ВЫКЛЮЧЕН (Лимиты)"
            android.widget.Toast.makeText(this, "Pro-режим: $status", android.widget.Toast.LENGTH_LONG).show()

        }

        findViewById<android.widget.TextView>(R.id.titleText).setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Палец коснулся экрана — запускаем таймер на 3000 мс (3 секунды)
                    handler.postDelayed(proRunnable, 3000)
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    // Палец отпущен раньше времени — отменяем таймер
                    handler.removeCallbacks(proRunnable)
                    true
                }
                else -> false
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.objectsRecyclerView)
        val addButton = findViewById<Button>(R.id.addObjectButton)
        val emptyStateLayout = findViewById<android.view.View>(R.id.emptyStateLayout)

        val adapter = ObjectAdapter(
            onClick = { obj ->
                // Открываем экран счётчиков (как и раньше)
                val intent = android.content.Intent(this, MeterListActivity::class.java)
                intent.putExtra(MeterListActivity.EXTRA_OBJECT_NAME, obj.name)
                intent.putExtra(MeterListActivity.EXTRA_OBJECT_ID, obj.id)
                startActivity(intent)
            },
            onLongClick = { obj ->
                // Показываем меню действий
                showObjectActionsDialog(obj)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.objects.collect { list ->
                adapter.submitList(list)
                emptyStateLayout.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        addButton.setOnClickListener {
            showAddEditObjectDialog(null)
        }
    }

    /** Диалог «Что сделать?» при долгом нажатии */
    private fun showObjectActionsDialog(obj: MeterObject) {
        val options = arrayOf("Редактировать", "Удалить")
        AlertDialog.Builder(this)
            .setTitle(obj.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddEditObjectDialog(obj)
                    1 -> confirmDeleteObject(obj)
                }
            }
            .show()
    }

    /** Подтверждение удаления */
    private fun confirmDeleteObject(obj: MeterObject) {
        AlertDialog.Builder(this)
            .setTitle("Удалить объект?")
            .setMessage("Внимание! Все счётчики и показания, привязанные к этому объекту, также будут удалены. Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteObject(obj)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /** Диалог добавления или редактирования объекта */
    private fun showAddEditObjectDialog(existing: MeterObject?) {
        val input = EditText(this)

        // Проверяем лимит: если создаем новый, объектов >= 1 и это НЕ Pro-версия
        if (existing == null && !settings.isProVersion && viewModel.objects.value.size >= 1) {
            AlertDialog.Builder(this)
                .setTitle("Ограничение бесплатной версии")
                .setMessage("В бесплатной версии можно создать только 1 объект.\n\nВ будущем здесь появится кнопка «Купить Pro».")
                .setPositiveButton("Понятно", null)
                .show()
            return
        }

        // Если редактируем — подставляем текущее имя
        if (existing != null) {
            input.setText(existing.name)
        }

        val title = if (existing == null) "Новый объект" else "Редактировать объект"
        val message = if (existing == null) "Введите название (например, «Квартира» или «Дом»)" else "Измените название объекта"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (existing == null) {
                        viewModel.addObject(name)
                    } else {
                        viewModel.updateObject(existing.copy(name = name))
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}