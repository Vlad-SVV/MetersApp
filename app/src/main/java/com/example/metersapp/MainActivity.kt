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

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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