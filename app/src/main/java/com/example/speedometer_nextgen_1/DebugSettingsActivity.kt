package com.example.speedometer_nextgen_1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.speedometer_nextgen_1.databinding.ActivityDebugSettingsBinding

class DebugSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugSettingsBinding
    private lateinit var mediaPlayerPlus: MediaPlayerPlus  // Initialize with volume settings as needed

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
            val volumeControlManager = VolumeControlManager(this, mediaPlayerPlus)
            volumeControlManager.showVolumeControlDialog()
        }

        // Update indicator light based on media player state
        updateIndicatorLight()
    }

    private fun callSpeedIndicators(speed: Int, speedAsDecimal: String) {
        // Implement your speed indication logic here
    }

    private fun updateIndicatorLight() {
        // Logic to update indicatorLight color based on mediaPlayer status
    }
}