package com.example.speedometer_nextgen_1

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

class MediaPlayerPlus(private val context: Context, var audioManager: AudioManager) {

    private var mediaPlayer: MediaPlayer? = null

    // Function to play music based on speed category
    fun playMusic(speedCategory: SpeedCategory) {
        // Release any existing MediaPlayer to avoid multiple instances
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }


            mediaPlayer?.release()

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val soundResId = when (speedCategory) {
                SpeedCategory.SPEEDING_UP -> R.raw.speedup
                SpeedCategory.CRUISING -> R.raw.maintainspeed
                SpeedCategory.SLOWING_DOWN -> R.raw.speeddown
                SpeedCategory.UNKNOWN -> R.raw.backgroundstrings
            }

            mediaPlayer = MediaPlayer.create(context, soundResId).apply {
                setAudioAttributes(audioAttributes)
                setVolume(1.0f, 1.0f)
                start()
            }
    }

    // Function to play silent audio (optional, if the issue persists)
    fun playSilentAudio() {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(context, R.raw.backgroundstrings).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setVolume(1.00f, 1.00f) // Play at a very low volume
            isLooping = true  // Keep looping to prevent audio hardware from turning off
            start()
        }
    }

    // Release resources
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
