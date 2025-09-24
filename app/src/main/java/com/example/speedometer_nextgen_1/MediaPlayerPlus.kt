package com.example.speedometer_nextgen_1

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log

class MediaPlayerPlus(
    private val context: Context,
    private val initialBackgroundVolume: Float,
    private val initialIndicatorVolume: Float
) {
    private var soundPool: SoundPool? = null
    private var soundMap: MutableMap<SpeedCategory, Int> = mutableMapOf()

    private var backgroundPlayer: MediaPlayer? = null
    private var backgroundVolume: Float = initialBackgroundVolume
    private var indicatorVolume: Float = initialIndicatorVolume

    var soundsLoaded = false
    private var soundsToLoadCount = 0
    private val totalSoundsToLoad = 4 // Explicitly set the number of sounds to load

    init {
        setupSoundPoolAndLoadSounds()
        playSilentAudio()
    }

    /**
     * Sets up the SoundPool and loads all audio files.
     * This method is called once during class initialization.
     */
    private fun setupSoundPoolAndLoadSounds() {
        Log.d("MediaPlayerPlus", "Setting up SoundPool and loading sounds...")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        // Listener to track when all sounds are loaded
        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsToLoadCount++
                Log.d("MediaPlayerPlus", "Sound loaded. ($soundsToLoadCount/$totalSoundsToLoad)")
                if (soundsToLoadCount == totalSoundsToLoad) {
                    soundsLoaded = true
                    Log.d("MediaPlayerPlus", "\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\nAll sounds loaded. Ready for playback.\n.\n.\n.\n.\n.\n.\n.")
                }
            } else {
                Log.w("MediaPlayerPlus", "Failed to load a sound.")
            }
        }

        // Load sounds and map them to their corresponding SpeedCategory
        soundMap[SpeedCategory.SPEEDING_UP] = soundPool?.load(context, R.raw.speedupmaestro3, 1) ?: 0
        soundMap[SpeedCategory.CRUISING] = soundPool?.load(context, R.raw.maintainmaestro3, 1) ?: 0
        soundMap[SpeedCategory.SLOWING_DOWN] = soundPool?.load(context, R.raw.downmaestro3, 1) ?: 0
        soundMap[SpeedCategory.UNKNOWN] = soundPool?.load(context, R.raw.hintergrundemaestro2, 1) ?: 0
    }

    /**
     * Plays a sound effect from the SoundPool for the given SpeedCategory.
     * This function only attempts to play if the SoundPool and sounds are ready.
     */
    fun playMusic(speedCategory: SpeedCategory) {
        if (!soundsLoaded) {
            Log.w("MediaPlayerPlus", "Sounds not loaded yet. Cannot play music.")
            return
        }

        val soundId = soundMap[speedCategory]
        if (soundId != null && soundId != 0) {
            soundPool?.play(soundId, indicatorVolume, indicatorVolume, 0, 0, 1.0f)
            Log.i("MediaPlayerPlus", "Audio played for category: $speedCategory")
        } else {
            Log.e("MediaPlayerPlus", "Could not play sound for $speedCategory (ID: $soundId)")
        }
    }

    /**
     * Plays a silent audio file in the background to maintain audio focus for the app.
     */
    fun playSilentAudio() {
        if (backgroundPlayer == null) {
            backgroundPlayer = MediaPlayer.create(context, R.raw.hintergrundemaestro2).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(backgroundVolume, backgroundVolume)
                isLooping = true
                start()
            }
        }
    }

    fun updateBackgroundVolume(volume: Float) {
        backgroundVolume = volume
        backgroundPlayer?.setVolume(volume, volume)
    }

    fun updateIndicatorVolume(volume: Float) {
        indicatorVolume = volume
    }

    fun releaseBackgroundPlayer() {
        backgroundPlayer?.release()
        backgroundPlayer = null
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        releaseBackgroundPlayer()
    }
}