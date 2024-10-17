package com.example.speedometer_nextgen_1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.speedometer_nextgen_1.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.Locale
import kotlin.properties.Delegates

class GPSgetSpeed(private val context: android.app.Activity, private val mainActivity: MainActivity) {
    // Constants
    private val locationPerm = 124

    // GPS related components
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // To check if location updates should stop
    private val isDone: Boolean by Delegates.observable(false) { _, _, newValue ->
        if (newValue) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Ask for location permissions from the user
    fun askForLocationPermission() {
        if (!hasLocationPermission()) {
            EasyPermissions.requestPermissions(
                context,  // Activity context is passed here
                "Location access is required to calculate speed",
                locationPerm,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // Check if location permission is already granted
    private fun hasLocationPermission(): Boolean {
        return EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Initialize location services
    fun initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        createLocationRequest()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isDone) {
                    val lastLocation = locationResult.lastLocation
                    lastLocation?.let { location ->
                        val speedKmH = location.speed * 3.6
                        val speedInt = speedKmH.toInt()
                        val speedDecimal = "%.1f".format(Locale.US, speedKmH - speedInt).substringAfter('.')

                        // Update UI or handle speed changes
                        // E.g., callback to MainActivity
                        // Update UI with speed information
                        //binding.currentSpeedId.text = speedInt.toString()
                        // binding.currentSpeedDecimalId.text = speedDecimal


                        //callSpeedIndicators(speedInt)
                        mainActivity.callSpeedIndicators(speedInt, speedDecimal)
                    }
                }
            }
        }
    }

    // Create location request
    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 300)
            .setMinUpdateIntervalMillis(400)
            .setMaxUpdateDelayMillis(50)
            .build()
    }

    // Start location updates
    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission is required to calculate speed", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // Stop location updates
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Handle permissions denied case
    fun onPermissionsDenied(perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(context, perms)) {
            AppSettingsDialog.Builder(context).build().show()
        }
    }
}
