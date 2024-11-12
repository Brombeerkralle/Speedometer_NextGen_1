package com.example.speedometer_nextgen_1
/*
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@HiltAndroidApp
class MyApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    fun provideMediaPlayerPlus(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences
    ): MediaPlayerPlus {
        val initialVolume = sharedPreferences.getFloat("backgroundVolume", 0.01f)
        return MediaPlayerPlus(context, initialVolume)
    }

    @Provides
    fun provideVolumeControlManager(
        @ApplicationContext context: Context,
        mediaPlayerPlus: MediaPlayerPlus,
        sharedPreferences: SharedPreferences
    ): VolumeControlManager {
        val initialVolume = sharedPreferences.getFloat("backgroundVolume", 0.01f)
        return VolumeControlManager(context, mediaPlayerPlus, sharedPreferences, initialVolume)
    }

    @Provides
    fun provideSpeedManagement(
        @ApplicationContext context: Context,
        view: View
    ): SpeedManagement {
        return SpeedManagement(context, view)
    }
}*/