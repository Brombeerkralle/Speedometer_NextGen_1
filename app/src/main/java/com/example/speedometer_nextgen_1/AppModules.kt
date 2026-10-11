package com.example.speedometer_nextgen_1

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

// Define the Koin module for your dependencies
val appModule = module {
    single { androidContext().getSharedPreferences("default_prefs", Context.MODE_PRIVATE) }
    
    single { 
        val prefs = get<SharedPreferences>()
        val bgVol = getSafeVolume(prefs, "backgroundVolume", 0.01f)
        val indVol = getSafeVolume(prefs, "indicatorVolume", 1f)
        
        MediaPlayerPlus(
            androidContext(),
            bgVol,
            indVol
        ) 
    }
    
    factory { (activity: AppCompatActivity) ->
        val prefs = get<SharedPreferences>()
        val bgVol = getSafeVolume(prefs, "backgroundVolume", 0.01f)
        val indVol = getSafeVolume(prefs, "indicatorVolume", 1f)

        VolumeControlManager(
            activity, get(), prefs, bgVol, indVol, null
        )
    }
    
    single { SpeedManagement(get()) }
    single { SharedPrefsManager.init(androidContext()) }
}

private fun getSafeVolume(prefs: SharedPreferences, key: String, default: Float): Float {
    return try {
        prefs.getFloat(key, default)
    } catch (e: ClassCastException) {
        prefs.getString(key, null)?.toFloatOrNull() ?: default
    }
}