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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle speed input and play music
        binding.playMusicButton.setOnClickListener {
            val speedText = binding.speedInput.text.toString()
            if (speedText.isNotEmpty()) {
                val speedValue = speedText.toIntOrNull()
                if (speedValue != null) {
                    // Call the method for speed management
                    callSpeedIndicators(speedValue, "*")
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
            val sharedPreferences = getPreferences(Context.MODE_PRIVATE)

            // Initialize mediaPlayerPlus first with default volume from SharedPreferences
            val initialVolume = sharedPreferences.getFloat("backgroundVolume", 0.01f)
            val volumeControlManager = VolumeControlManager(this, mediaPlayerPlus,sharedPreferences,initialVolume)
            volumeControlManager.showVolumeControlDialog()
        }

        // Update indicator light based on media player state
        updateIndicatorLight()
    }

    fun setupVolumeControlButton() {
        val volumeButton = findViewById<Button>(R.id.volumeControlButton)

        volumeButton.setOnClickListener {
            volumeControlManager.showVolumeControlDialog()
        }
    }

    fun audioPlayerActive() {
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

    private fun callSpeedIndicators(speed: Int, speedAsDecimal: String) {
        // Implement your speed indication logic here
    }

    private fun updateIndicatorLight() {
        // Logic to update indicatorLight color based on mediaPlayer status
    }

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
}