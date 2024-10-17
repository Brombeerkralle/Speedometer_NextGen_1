package com.example.speedometer_nextgen_1

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

class MediaPlayerPlus(private val context: Context, var audioManager: AudioManager) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    /*// Function to request audio focus
    fun requestAudioFocus(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        mediaPlayer?.pause()
                    }
                }
                .build()
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            // For API levels below 26, use the deprecated method
            val result = audioManager.requestAudioFocus(
                null,  // No listener for older versions
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    // Function to release audio focus
    fun releaseAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }
    Request / Release audio focus causes error 8 -> no audfio is playing
    */


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
