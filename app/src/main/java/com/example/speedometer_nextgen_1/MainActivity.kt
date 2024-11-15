package com.example.speedometer_nextgen_1


import android.content.BroadcastReceiver
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.speedometer_nextgen_1.databinding.ActivityMainBinding
import pub.devrel.easypermissions.EasyPermissions
import android.media.AudioManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private lateinit var binding: ActivityMainBinding
    private val speedManagement: SpeedManagement by inject()
    private val mediaPlayerPlus: MediaPlayerPlus by inject()
    private val volumeControlManager: VolumeControlManager by inject() { parametersOf(this) }
    private val debugSettingsActivity: DebugSettingsActivity by inject() // if DebugSettingsActivity needs injection
    private val sharedPreferences: SharedPreferences by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize layout components
        initializeLayout()
        //initializeClasses()
        val locationServiceIntent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, locationServiceIntent)

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

    private fun updateSystemBarsColor(color: Int) {
        window.statusBarColor = color
        window.navigationBarColor = color
    }



    private val locationUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val speed = intent?.getIntExtra("speed", 0) ?: 0
            val speedDecimal = intent?.getStringExtra("speedDecimal") ?: "*"
            Log.d("MainActivity", "Broadcast received: speed=$speed, decimal=$speedDecimal")

            // Update the UI on the main thread
            runOnUiThread {
                callSpeedIndicators(speed, speedDecimal)
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
    fun callSpeedIndicators(speed: Int, speedAsDecimal: String) {
        binding.currentSpeedId.text = speed.toString()
        binding.currentSpeedDecimalId.text = speedAsDecimal

        val speedHasChanged = speedManagement.hasSpeedChanged(speed)
        val categoryHasChanged = speedManagement.hasCategoryChanged(speed)

        if (categoryHasChanged) {
            mediaPlayerPlus.playMusic(speedManagement.getSpeedCategory(speed))
        }

        if (speedHasChanged) {
            speedManagement.updateBackgroundColor(speed) { color ->
                binding.root.setBackgroundColor(color)
            }
            speedManagement.previousSpeed = speed
        }
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
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
