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
            // Listener registrieren, um Updates zu erhalten
            locationService?.addListener(this@IndicatorAudioForegroundservice)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isBound = false
        }
    }

    override fun onCreate() {
        super.onCreate()
/*
        val filter = IntentFilter("com.example.speedometer_nextgen_1.LOCATION_UPDATE")
        ContextCompat.registerReceiver(
            this,
            locationUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
*/

        startForegroundService()
        mediaPlayerPlus.playSilentAudio()


        // Binden an den LocationService
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

    /*  private val locationUpdateReceiver = object : BroadcastReceiver() {
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

       val categoryHasChanged = speedManagement.hasCategoryChangedFlag()
       Log.w("IAFS", "----------\nforeground Audio Service Active\n        $categoryHasChanged\n --------")
       if (categoryHasChanged) {
           val speedCategory = speedManagement.getSpeedCategory(speed)
           Log.w("IAFS", "         $speed\n        $speedCategory\n --------")
           if (speedCategory != SpeedCategory.UNKNOWN) {
               Log.w("IAFS", "----------\nAudio Requested\n--------")
               mediaPlayerPlus.playMusic(speedCategory)
               //Visual Indicator that Music should be played now
               //binding.infotainmentIDleft.text = "Music"
           }
       }
   }*/

    // Diese Methode wird jetzt direkt vom LocationService aufgerufen
     override fun onLocationUpdate(speed: Int, speedAsDecimal: String, accelerationMagnitude: Float, gpsLocationAccuracy: Number) {
        val categoryHasChanged = speedManagement.hasCategoryChangedFlag()
        if (categoryHasChanged) {
            val speedCategory = speedManagement.getSpeedCategory(speed)
            if (speedCategory != SpeedCategory.UNKNOWN) {
                if (mediaPlayerPlus.soundsLoaded) {
                    mediaPlayerPlus.playMusic(speedCategory)
                } else {
                    Log.w("IndicatorAudioService", "MediaPlayerPlus not ready to play sound.")
                }
            }
        }
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregisterReceiver(locationUpdateReceiver)
        if (isBound) {
            // Wichtig: Listener entfernen und Service entbinden
            locationService?.removeListener(this)
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf() // This will call onDestroy()
        super.onTaskRemoved(rootIntent)
    }
}