package com.example.metersapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MeterListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OBJECT_NAME = "extra_object_name"
        const val EXTRA_OBJECT_ID = "extra_object_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meter_list)

        val objectName = intent.getStringExtra(EXTRA_OBJECT_NAME) ?: "Объект"
        findViewById<TextView>(R.id.objectNameText).text = objectName
    }
}