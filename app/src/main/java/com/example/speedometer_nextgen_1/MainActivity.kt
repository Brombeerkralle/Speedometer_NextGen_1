package com.example.speedometer_nextgen_1

/**
 * Latest Running Version
 * Fully operational
 * Latest one on Win11 Narwahl
 */

import android.content.BroadcastReceiver
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.speedometer_nextgen_1.databinding.ActivityMainBinding
import pub.devrel.easypermissions.EasyPermissions
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import android.Manifest


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private lateinit var binding: ActivityMainBinding
    private val speedManagement: SpeedManagement by inject()
    private val mediaPlayerPlus: MediaPlayerPlus by inject()
    private val volumeControlManager: VolumeControlManager by inject() { parametersOf(this) }
    private val debugSettingsActivity: DebugSettingsActivity by inject() // if DebugSettingsActivity needs injection
    private val sharedPreferences: SharedPreferences by inject()

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationService()
        }
    }

    private fun startLocationService() {
        val locationServiceIntent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, locationServiceIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Berechtigung erteilt", Toast.LENGTH_SHORT).show()
                startLocationService()
            } else {
                Toast.makeText(
                    this,
                    "Standort-Berechtigung erforderlich für GPS-Funktion.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize layout components
        initializeLayout()
        otherInits()


        // Button to open the menu
        val menuButton = findViewById<Button>(R.id.menuButton)
        menuButton.setOnClickListener {
            val popupMenu = PopupMenu(this, menuButton)
            popupMenu.menuInflater.inflate(R.menu.main_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                onOptionsItemSelected(item)
            }
            popupMenu.show()
        }

        // Setup button to open volume control
        findViewById<Button>(R.id.openVolumeControlButton).setOnClickListener {
            volumeControlManager.showVolumeControlDialog()
        }

    }



    // Function to initialize layout components, colors, and window insets
    private fun initializeLayout() {
        // Prevent the screen from turning off
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Make the status bar and navigation bar transparent and enable edge-to-edge content
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set edge-to-edge UI and window insets handling
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Retrieve the insets for system bars (status bar, navigation bar, etc.)
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding to the view based on the insets
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom
            )
            insets
        }
        // Set initial status and navigation bar colors to your default background color
        updateSystemBarsColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun otherInits() {
        checkAndRequestLocationPermission()

        val indicatorAudioForegroundserviceIntent = Intent(this, IndicatorAudioForegroundservice::class.java)
        ContextCompat.startForegroundService(this, indicatorAudioForegroundserviceIntent)

        mediaPlayerPlus.playSilentAudio()
    }

    private fun updateSystemBarsColor(color: Int) {
        window.statusBarColor = color
        window.navigationBarColor = color

        //Change Status Bar Text to White
        val insetsController = window.insetsController
        if (insetsController != null) {
            if (isLightBackground(color)) {
                // Text/Icons dunkel (für helle Hintergründe)
                insetsController.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                // Text/Icons hell (für dunkle Hintergründe)
                insetsController.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }
    }
    private fun isLightBackground(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) > 0.5
    }



    private val locationUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val speed = intent?.getIntExtra("speed", 0) ?: 0
            val speedDecimal = intent?.getStringExtra("speedDecimal") ?: "*"
            val accelerationMagnitude = intent?.getFloatExtra("accelerationMagnitude", 99f) ?: 0
            val gpsLocationAccuracy = intent?.getFloatExtra("gpsLocationAccuracy", 99f) ?: 0
 //Log.d("MainActivity", "Broadcast received: $speed,$speedDecimal | Gyro: $accelerationMagnitude")
            // Update the UI on the main thread
            runOnUiThread {
                callSpeedIndicators(speed, speedDecimal, accelerationMagnitude.toString(), gpsLocationAccuracy)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_debug_settings -> {
                // Start DebugSettingsActivity when the menu item is clicked
                val intent = Intent(this, DebugSettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Function that handles speed changes and calls background color change or music playback
    fun callSpeedIndicators(speed: Int, speedAsDecimal: String, accelerationMagnitude: String, gpsLocationAccuracy: Number) {
        binding.currentSpeedId.text = speed.toString()
        binding.currentSpeedDecimalId.text = speedAsDecimal
        binding.infotainmentIDleft.text = " "
        binding.infotainmentIDright.text = gpsLocationAccuracy.toString().take(4)

        val speedHasChanged = speedManagement.hasSpeedChanged(speed)
        val categoryHasChanged = speedManagement.hasCategoryChanged(speed)

        if (categoryHasChanged) {
            mediaPlayerPlus.playMusic(speedManagement.getSpeedCategory(speed))
            //Visual Indicator that Music should be played now
            binding.infotainmentIDleft.text = "Music"
        }
        if (speedHasChanged) {
            speedManagement.updateBackgroundColor(speed) { color ->
                binding.root.setBackgroundColor(color)
                updateSystemBarsColor(color)
            }
            speedManagement.previousSpeed = speed
        }
    }



    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        //gpsGetSpeed.onPermissionsDenied(perms)
        Toast.makeText(this, "onPermissionsDenied", Toast.LENGTH_SHORT).show()
    }
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        //gpsGetSpeed.startLocationUpdates()
        Toast.makeText(this, "onPermissionsGranted", Toast.LENGTH_SHORT).show()
    }
    override fun onResume() {
        Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show()
        super.onResume()
        val filter = IntentFilter("com.example.speedometer_nextgen_1.LOCATION_UPDATE")
        ContextCompat.registerReceiver(
            this,
            locationUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        Log.d("MainActivity", "Receiver registered in onResume")
    }

    override fun onPause() {
        Toast.makeText(this, "onPause", Toast.LENGTH_SHORT).show()
        super.onPause()
        Log.d("MainActivity", "Receiver unregistered in onPause")
    }

    override fun onStop() {
        Toast.makeText(this, "onStop", Toast.LENGTH_SHORT).show()
        super.onStop()
        unregisterReceiver(locationUpdateReceiver)
        Log.d("MainActivity", "Receiver unregistered in onStop")
    }

    override fun onDestroy() {
        Toast.makeText(this, "onDestroy", Toast.LENGTH_SHORT).show()
        super.onDestroy()
        // Stop LocationService
        val locationServiceIntent = Intent(this, LocationService::class.java)
        stopService(locationServiceIntent)

        mediaPlayerPlus.release()
        mediaPlayerPlus.releaseBackgroundPlayer()
    }
    override fun onRationaleAccepted(requestCode: Int) {}
    override fun onRationaleDenied(requestCode: Int) {}
}
