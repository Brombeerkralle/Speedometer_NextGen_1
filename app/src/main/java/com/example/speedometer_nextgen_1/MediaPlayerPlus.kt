package com.example.speedometer_nextgen_1

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer

class MediaPlayerPlus(private val context: Context, var audioManager: AudioManager) {

    private var mediaPlayer: MediaPlayer? = null
    private var backgroundPlayer: MediaPlayer? = null
    private var backgroundVolume: Float = 0.5f

    // Function to play music based on speed category
    fun playMusic(speedCategory: SpeedCategory) {
        // Release any existing MediaPlayer to avoid multiple instances


            mediaPlayer?.release()
            mediaPlayer = null


        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val soundResId = when (speedCategory) {
            SpeedCategory.SPEEDING_UP -> R.raw.speedup
            SpeedCategory.CRUISING -> R.raw.maintainspeed
            SpeedCategory.SLOWING_DOWN -> R.raw.speeddown
            SpeedCategory.UNKNOWN -> null
        }

        if (soundResId != null) {
            mediaPlayer = MediaPlayer.create(context, soundResId).apply {
                setAudioAttributes(audioAttributes)
                setVolume(1.0f, 1.0f)
                start()
            }
        }
    }

    // Function to play silent audio (optional, if the issue persists)
    fun playSilentAudio() {
        if (backgroundPlayer == null) {
            backgroundPlayer = MediaPlayer.create(context, R.raw.backgroundpiano).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(backgroundVolume, backgroundVolume) // Set appropriate volume
                isLooping = true  // Loop the background sound
                start()  // Play the background sound
            }
        }
    }

    fun updateBackgroundVolume(volume: Float) {
        backgroundVolume = volume
        backgroundPlayer?.setVolume(volume, volume)
    }

    fun releaseBackgroundPlayer() {
        backgroundPlayer?.release()
        backgroundPlayer = null
    }

    // Release resources
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
