package com.example.speedometer_nextgen_1

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.speedometer_nextgen_1.databinding.ActivityDebugSettingsBinding
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class DebugSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugSettingsBinding

    private val mediaPlayerPlus: MediaPlayerPlus by inject()
    private val volumeControlManager: VolumeControlManager by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMenuControls()


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