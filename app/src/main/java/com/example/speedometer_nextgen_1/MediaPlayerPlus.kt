package com.example.speedometer_nextgen_1

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log

class MediaPlayerPlus(
    private val context: Context,
    initialBackgroundVolume: Float,
    initialIndicatorVolume: Float
) {


    private var soundPool: SoundPool? = null
    private var soundMap: MutableMap<SpeedCategory, Int> = mutableMapOf()

    private var backgroundPlayer: MediaPlayer? = null
    private var backgroundVolume: Float = initialBackgroundVolume // Set initial background volume
    private var indicatorVolume: Float = initialIndicatorVolume // Set initial background volume

    init {
        loadSounds()
    }

    private fun loadSounds() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3) // You can play up to 3 sounds at the same time
            .setAudioAttributes(audioAttributes)
            .build()

        soundMap[SpeedCategory.SPEEDING_UP] = soundPool?.load(context, R.raw.speedupmaestro3, 1) ?: 0
        soundMap[SpeedCategory.CRUISING] = soundPool?.load(context, R.raw.maintainmaestro3, 1) ?: 0
        soundMap[SpeedCategory.SLOWING_DOWN] = soundPool?.load(context, R.raw.downmaestro3, 1) ?: 0
    }
    // Function to play music based on speed category
    fun playMusic(speedCategory: SpeedCategory) {
        val soundId = soundMap[speedCategory]
        if (soundId != null && soundId != 0 && soundPool != null) {
            soundPool?.play(soundId, indicatorVolume, indicatorVolume, 0, 0, 1.0f)
            // Log.d("MediaPlayerPlus", "indicatorVolume $indicatorVolume")
        } else {
            //Log.w("MediaPlayerPlus", "Could not play sound for $speedCategory (ID: $soundId, SoundPool: $soundPool)")
        }
    }

    // Function to play silent audio (optional, if the issue persists)
    fun playSilentAudio() {
        if (backgroundPlayer == null) {
            backgroundPlayer = MediaPlayer.create(context, R.raw.hintergrundemaestro2).apply {
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

    fun updateIndicatorVolume(volume: Float) {
        indicatorVolume = volume
        //Log.d("MediaPlayerPlus", "indicatorVolume $indicatorVolume")
    }

    fun releaseBackgroundPlayer() {
        backgroundPlayer?.release()
        backgroundPlayer = null
    }

    // Release resources
    fun release() {
        soundPool?.release()
        soundPool = null
        releaseBackgroundPlayer()
    }
}
