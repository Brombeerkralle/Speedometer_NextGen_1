package com.example.speedometer_nextgen_1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import java.util.Arrays
import java.util.Locale
import kotlin.math.sqrt

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var speedFilter: SpeedFilter
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private var accelerationMagnitude: Float = 0f

    // LiveData für die MainActivity
    val speedData = MutableLiveData<Int>()
    val speedDecimalData = MutableLiveData<String>()
    val accelerationMagnitudeData = MutableLiveData<Float>()
    val gpsLocationAccuracyData = MutableLiveData<Number>()

    // Liste von Listeners für den Audioservice
    private val listeners = mutableListOf<LocationUpdateListener>()

    // Schnittstelle für die direkte Kommunikation mit dem Audioservice
    interface LocationUpdateListener {
        fun onLocationUpdate(speed: Int, speedAsDecimal: String, accelerationMagnitude: Float, gpsLocationAccuracy: Number)
    }

    // Methode zum Hinzufügen eines Listeners
    fun addListener(listener: LocationUpdateListener) {
        listeners.add(listener)
    }

    // Methode zum Entfernen eines Listeners
    fun removeListener(listener: LocationUpdateListener) {
        listeners.remove(listener)
    }

    // Binder für die Kommunikation mit Clients (z.B. MainActivity)
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }


    override fun onCreate() {
        super.onCreate()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "Keine Standort-Berechtigung, Service wird gestoppt!")
            stopSelf()
            return
        }


        startForegroundService()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        speedFilter = SpeedFilter(3) // Initialize speedFilter here!
        createLocationCallback()
        startLocationUpdates()
        initAccelerometer()
    }

    private fun initAccelerometer() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        val accelerometerListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                accelerationMagnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                //Log.d("LocationService", "ACCELEROMETER: $accelerationMagnitude")
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        val channelName = "Location Service"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Tracking location in the background")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let {
                    Log.d("LocationService", "------------START LOCATION CALLBACK-------------")
                    //Log.d("LocationService", "ACCELEROMETER: $accelerationMagnitude")

                    var speedKmH = location.speed * 3.6f // Get the raw speed
                    speedKmH = speedFilter.addSpeed(speedKmH)


                    //Log.d("LocationService", "Realtime Speed: $speedKmH")                    // Use accelerometer to adjust filtering
                    if (accelerationMagnitude > 13) { // Adjust threshold as needed
                        // If accelerating, trust the raw speed more
                        // speedKmH is already set to the raw speed.
                    } else {
                        // If not accelerating, apply the median filter
                        speedKmH = speedFilter.addSpeed(speedKmH)
                    }



                    val speedInt = speedKmH.toInt()
                    val speedDecimal = "%.1f".format(Locale.US, speedKmH - speedInt).substringAfter('.')

                    val gpsLocationAccuracy = location.accuracy


                    // 1. Updates für LiveData (an die MainActivity)
                    speedData.postValue(speedInt)
                    speedDecimalData.postValue(speedDecimal)
                    accelerationMagnitudeData.postValue(accelerationMagnitude)
                    gpsLocationAccuracyData.postValue(gpsLocationAccuracy)

                    // 2. Updates für Listener (an den Audioservice)
                    listeners.forEach {
                        it.onLocationUpdate(speedInt, speedDecimal, accelerationMagnitude, gpsLocationAccuracy)
                    }

                    //Log.d("LocationService", "-Updates sent to LiveData and Listeners.")
                   // Log.d("LocationService", "LiveData sent: $speedInt,$speedDecimal")
                   // Log.d("LocationService", "Accuracy: $gpsLocationAccuracy m")


/*
                    // Broadcast the speed update
                    val intent = Intent("com.example.speedometer_nextgen_1.LOCATION_UPDATE").apply {
                        putExtra("speed", speedInt)
                        putExtra("speedDecimal", speedDecimal)
                        putExtra("accelerationMagnitude", accelerationMagnitude)
                        putExtra("gpsLocationAccuracy", gpsLocationAccuracy)
                    }
                    sendBroadcast(intent)  // Replacing LocalBroadcastManager
                    Log.d("LocationService", "Broadcast sent: $speedInt,$speedDecimal")
                    Log.d("LocationService", "Accuracy: $gpsLocationAccuracy m")

 */

                    //Log.d("LocationService", "------------END-------------")
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
            .setMinUpdateIntervalMillis(80)
            .setMaxUpdateDelayMillis(40)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {}

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        })
    }

    // SpeedFilter class
    class SpeedFilter(private val historySize: Int) {
        private val speedHistory = FloatArray(historySize)
        private var currentIndex = 0

        fun addSpeed(speed: Float): Float {
            speedHistory[currentIndex] = speed
            currentIndex = (currentIndex + 1) % historySize
            val sortedSpeeds = speedHistory.clone()
            Arrays.sort(sortedSpeeds)
            return sortedSpeeds[historySize / 2]
        }
    }

    fun triggerTestUpdate() {
        Log.d("LocationService", "------------START TEST -------------")

        val testSpeeds = listOf(20, 26, 30, 34)
        Thread {
            testSpeeds.forEach { testSpeed ->
                val testSpeedInt = testSpeed
                val testSpeedDecimal = "0"
                val testAcceleration = 10f
                val testAccuracy = 3f
                speedData.postValue(testSpeedInt)
                speedDecimalData.postValue(testSpeedDecimal)
                accelerationMagnitudeData.postValue(testAcceleration)
                gpsLocationAccuracyData.postValue(testAccuracy)
                listeners.forEach {
                    it.onLocationUpdate(testSpeedInt, testSpeedDecimal, testAcceleration, testAccuracy)
                }
                Log.d("LocationService", "Test update: Speed $testSpeedInt")
                Thread.sleep(2000)
            }
        }.start()

        Log.d("LocationService", "------------END-------------")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop the service and remove the notification
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
