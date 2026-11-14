package com.example.metersapp.utils

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("meters_settings", Context.MODE_PRIVATE)

    // Храним статус Pro-версии. По умолчанию — false (бесплатная).
    var isProVersion: Boolean
        get() = prefs.getBoolean("is_pro_version", false)
        set(value) = prefs.edit().putBoolean("is_pro_version", value).apply()
}