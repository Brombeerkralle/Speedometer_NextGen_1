package com.example.speedometer_nextgen_1

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

// Define the Koin module for your dependencies
val appModule = module {
    single { androidContext().getSharedPreferences("default_prefs", Context.MODE_PRIVATE) }
    single { MediaPlayerPlus(androidContext(), get<SharedPreferences>().getFloat("backgroundVolume", 0.01f)) }
    factory { (activity: AppCompatActivity) ->
        VolumeControlManager(activity, get(), get(), get<SharedPreferences>().getFloat("backgroundVolume", 0.01f))
    }
    single { SpeedManagement(get()) }
    single { SharedPrefsManager.init(androidContext()) }
}

