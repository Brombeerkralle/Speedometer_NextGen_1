package com.example.speedometer_nextgen_1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.koin.android.ext.android.inject

class IndicatorAudioForegroundservice : Service()  {

    private val mediaPlayerPlus: MediaPlayerPlus by inject()
    private val speedManagement: SpeedManagement by inject()

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        mediaPlayerPlus.playSilentAudio()
    }

    private fun startForegroundService() {
        val channelId = "audio_foreground_service_channel"
        val channelName = "Indicator sound chanel"
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Indicator sound")
            .setContentText("Play Indicator tunes")
            .setSmallIcon(R.drawable.ic_music_note)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }

    private val locationUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val speed = intent?.getIntExtra("speed", 0) ?: 0
            val speedDecimal = intent?.getStringExtra("speedDecimal") ?: "*"
            val accelerationMagnitude = intent?.getFloatExtra("accelerationMagnitude", 99f) ?: 0
            val gpsLocationAccuracy = intent?.getFloatExtra("gpsLocationAccuracy", 99f) ?: 0
            callSpeedIndicators(speed, speedDecimal, accelerationMagnitude.toString(), gpsLocationAccuracy)
        }
    }

    // Function that handles speed changes and calls background color change or music playback
    private fun callSpeedIndicators(speed: Int, speedAsDecimal: String, accelerationMagnitude: String, gpsLocationAccuracy: Number) {

        val speedHasChanged = speedManagement.hasSpeedChanged(speed)
        val categoryHasChanged = speedManagement.hasCategoryChangedFlag()

        if (categoryHasChanged) {
            mediaPlayerPlus.playMusic(speedManagement.getSpeedCategory(speed))
            //Visual Indicator that Music should be played now
            //binding.infotainmentIDleft.text = "Music"
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}