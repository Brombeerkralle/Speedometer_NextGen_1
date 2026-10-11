package com.example.speedometer_nextgen_1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.koin.android.ext.android.inject

class IndicatorAudioForegroundservice : Service(), LocationService.LocationUpdateListener {

    private val mediaPlayerPlus: MediaPlayerPlus by inject()
    private val speedManagement: SpeedManagement by inject()

    private var locationService: LocationService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            isBound = true
            locationService?.addListener(this@IndicatorAudioForegroundservice)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isBound = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        mediaPlayerPlus.playSilentAudio()

        val locationServiceIntent = Intent(this, LocationService::class.java)
        bindService(locationServiceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun startForegroundService() {
        val channelId = "audio_foreground_service_channel"
        val channelName = "Indicator sound chanel"
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Indicator sound")
            .setContentText("Play Indicator tunes")
            .setSmallIcon(R.drawable.ic_music_note)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }

    override fun onLocationUpdate(speed: Int, speedAsDecimal: String, accelerationMagnitude: Float, gpsLocationAccuracy: Number) {
        val categoryHasChanged = speedManagement.hasCategoryChanged(speed)
        Log.w("IndicatorAudioService", "speed = $speed")
        if (categoryHasChanged) {
            val status = speedManagement.getSpeedStatus(speed)
            if (status.category != SpeedCategory.UNKNOWN) {
                if (mediaPlayerPlus.soundsLoaded) {
                    mediaPlayerPlus.playMusic(status.category, status.targetSpeed)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            locationService?.removeListener(this)
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}