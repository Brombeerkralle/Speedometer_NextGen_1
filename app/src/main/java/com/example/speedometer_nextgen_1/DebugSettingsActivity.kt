package com.example.speedometer_nextgen_1

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.speedometer_nextgen_1.databinding.ActivityDebugSettingsBinding

class DebugSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugSettingsBinding
    private lateinit var mediaPlayerPlus: MediaPlayerPlus  // Initialize with volume settings as needed
    private lateinit var volumeControlManager: VolumeControlManager
    private lateinit var mainActivity: MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMenuControls()

        // Use the shared instance of SharedPreferences
        val sharedPreferences = SharedPrefsManager.getPreferences()
        val initialVolume = sharedPreferences.getFloat("backgroundVolume", 0.01f)
        mediaPlayerPlus = MediaPlayerPlus(this, initialVolume)

        // Then initialize volumeControlManager with the initialized mediaPlayerPlus
        volumeControlManager = VolumeControlManager(this, mediaPlayerPlus, sharedPreferences, initialVolume)

        mainActivity = MainActivity()

        setupMenuControls()

        // Handle speed input and play music
        binding.mockGPSinsertButton.setOnClickListener {
            val speedText = binding.speedInput.text.toString()
            if (speedText.isNotEmpty()) {
                val speedValue = speedText.toIntOrNull()
                if (speedValue != null) {
                    // Call the method for speed management
                    mainActivity.callSpeedIndicators(speedValue, "*")
                } else {
                    Toast.makeText(this, "Please enter a valid speed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Speed input cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Volume control setup
        binding.volumeControlButton.setOnClickListener {
            // Call the volume control dialog from VolumeControlManager
             volumeControlManager.showVolumeControlDialog()
        }

        // Update indicator light based on media player state
        updateIndicatorLight()
    }



    private fun setupMenuControls() {
        val menuControl = findViewById<Button?>(R.id.menuControlButton)
        menuControl?.setOnClickListener {
            // Ensure this is correctly integrated with the menu page updates
        }
    }

    private fun updateIndicatorLight() {
        // Logic to update indicatorLight color based on mediaPlayer status
        audioPlayerActive()
    }

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


}