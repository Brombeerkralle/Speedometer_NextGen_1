package com.example.speedometer_nextgen_1

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsManager {
    private const val PREFS_NAME = "my_prefs"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getPreferences(): SharedPreferences {
        return sharedPreferences ?: throw IllegalStateException("SharedPrefsManager not initialized")
    }
}

