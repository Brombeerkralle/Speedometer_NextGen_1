package com.example.speedometer_nextgen_1


import android.app.AlertDialog
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
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import com.example.speedometer_nextgen_1.databinding.DialogVolumeControlBinding

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {


    private lateinit var binding: ActivityMainBinding
    private lateinit var speedManagement: SpeedManagement
    private lateinit var mediaPlayerPlus: MediaPlayerPlus
    private lateinit var gpsGetSpeed: GPSgetSpeed
    private lateinit var volumeControlManager: VolumeControlManager  // New manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize layout, location services, and color definitions
        initializeLayout()
        initializeClasses()


        // Set up button and input for manual testing
        setupDebugToggle()

        setupVolumeControlButton()
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
        updateSystemBarsColor(
            ContextCompat.getColor(
                this,
                R.color.black
            )
        ) // default background color
    }

    private fun updateSystemBarsColor(color: Int) {
        window.statusBarColor = color
        window.navigationBarColor = color
    }

    private fun initializeClasses() {
        speedManagement = SpeedManagement(this, binding.root)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaPlayerPlus = MediaPlayerPlus(this, audioManager )


        gpsGetSpeed = GPSgetSpeed(this, this)
        gpsGetSpeed.initializeLocationServices()

        // Initialize VolumeControlManager with mediaPlayerPlus
        volumeControlManager = VolumeControlManager(this, mediaPlayerPlus)
    }

    private fun setupDebugToggle() {
        val debugToggleCheckbox = findViewById<CheckBox>(R.id.debugToggleCheckbox)
        val debugControlsLayout = findViewById<LinearLayout>(R.id.debugControlsLayout)

        debugToggleCheckbox.setOnCheckedChangeListener { _, isChecked ->
            // Show debug controls if the checkbox is checked, hide otherwise
            debugControlsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Retain the existing debug button setup within the debug controls layout
        setupDebugButton()
    }

    // Set up the button for manual debugging and testing
    private fun setupDebugButton() {
        val speedInput = findViewById<EditText>(R.id.speedInput)
        val playMusicButton = findViewById<Button>(R.id.playMusicButton)

        playMusicButton.setOnClickListener {
            val speedText = speedInput.text.toString()
            if (speedText.isNotEmpty()) {
                val speedValue = speedText.toIntOrNull()
                if (speedValue != null) {
                    // Manually call speed indicators for debugging purposes
                    callSpeedIndicators(speedValue, "*")
                } else {
                    Toast.makeText(this, "Please enter a valid speed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Speed input cannot be empty", Toast.LENGTH_SHORT).show()
            }
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
            speedManagement.updateBackgroundColor(speed)
            speedManagement.previousSpeed = speed
        }
        audioPlayerActive()
    }

    // Function to check if music or other audio is playing and update the indicator light accordingly
    private fun audioPlayerActive() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isMusicPlaying = audioManager.isMusicActive

        // Get the indicator view
        val indicatorLight = binding.indicatorLight

        // Get the background as a GradientDrawable (to maintain circle shape)
        val background = indicatorLight.background as GradientDrawable

        // Change the color based on whether music is playing
        if (isMusicPlaying) {
            // Set the indicator light to red when music is playing
            background.setColor(ContextCompat.getColor(this, R.color.red))
        } else {
            // Set the indicator light to green when no music is playing
            background.setColor(ContextCompat.getColor(this, R.color.green))

            // Restart silent audio if no music is playing
            mediaPlayerPlus.playSilentAudio() // Ensure silent audio keeps running
        }
    }

    private fun setupVolumeControlButton() {
        val volumeButton = findViewById<Button>(R.id.volumeControlButton)

        volumeButton.setOnClickListener {
            volumeControlManager.showVolumeControlDialog()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        gpsGetSpeed.onPermissionsDenied(perms)
    }
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        gpsGetSpeed.startLocationUpdates()
    }
    override fun onResume() {
        super.onResume()
        gpsGetSpeed.startLocationUpdates()
        mediaPlayerPlus.playSilentAudio()
        Toast.makeText(this, "Resumeeeeeeeeeeeee", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        gpsGetSpeed.stopLocationUpdates()
        mediaPlayerPlus.release()
        mediaPlayerPlus.releaseBackgroundPlayer()
        Toast.makeText(this, "Pause", Toast.LENGTH_SHORT).show()
    }
    override fun onRationaleAccepted(requestCode: Int) {}
    override fun onRationaleDenied(requestCode: Int) {}
}
