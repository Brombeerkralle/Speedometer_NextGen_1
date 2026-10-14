package com.example.speedometer_nextgen_1

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

/**
 * Koin Module: Central dependency injection configuration for the application.
 */
val appModule = module {

    // ================= ================= =================
    // SharedPreferences
    // ================= ================= =================
    single { androidContext().getSharedPreferences("default_prefs", Context.MODE_PRIVATE) }

    // ================= ================= =================
    // Audio & MediaPlayer
    // ================= ================= =================
    single { 
        val prefs = get<SharedPreferences>()
        val bgVol = getSafeVolume(prefs, "backgroundVolume", 0.005f)
        val indVol = getSafeVolume(prefs, "indicatorVolume", 1f)
        
        MediaPlayerPlus(
            androidContext(),
            bgVol,
            indVol
        ) 
    }

    // ================= ================= =================
    // UI Managers & Navigation Services
    // ================= ================= =================
    factory { (activity: AppCompatActivity) ->
        val prefs = get<SharedPreferences>()
        val bgVol = getSafeVolume(prefs, "backgroundVolume", 0.005f)
        val indVol = getSafeVolume(prefs, "indicatorVolume", 1f)

        VolumeControlManager(
            activity, get(), prefs, bgVol, indVol, null
        )
    }

    // ================= ================= =================
    // Speed Logic & Calculations
    // ================= ================= =================
    single { SpeedManagement(get()) }
}

/**
 * Safely reads volume settings from SharedPreferences, handling legacy formats and enforcing safe quiet defaults.
 */
private fun getSafeVolume(prefs: SharedPreferences, key: String, default: Float): Float {
    return try {
        val value = prefs.getFloat(key, default)
        if (key == "backgroundVolume" && value > 0.05f) {
            0.005f
        } else {
            value
        }
    } catch (e: ClassCastException) {
        prefs.getString(key, null)?.toFloatOrNull() ?: default
    }
}
