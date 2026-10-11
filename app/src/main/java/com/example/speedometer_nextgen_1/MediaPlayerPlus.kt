package com.example.speedometer_nextgen_1

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.io.File

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

// ... AudioAttributes und SoundPool Initialisierung bleibt gleich ...
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsToLoadCount++
                if (soundsToLoadCount == totalSoundsToLoad) {
                    soundsLoaded = true
                    Log.i("MediaPlayerPlus", "All 4 sounds loaded successfully.")
                }
            }
        }

// Dynamisches Laden der 4 Kategorien
        loadCategorySound(SpeedCategory.SPEEDING_UP, R.raw.speedupmaestro3)
        loadCategorySound(SpeedCategory.CRUISING, R.raw.maintainmaestro3)
        loadCategorySound(SpeedCategory.SLOWING_DOWN, R.raw.downmaestro3)
        loadCategorySound(SpeedCategory.UNKNOWN, R.raw.hintergrundemaestro2)
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

    /**
     * Definiert die Dateinamen für die benutzerdefinierten Sounds im internen Speicher
     */
    private fun getCustomSoundFile(category: SpeedCategory): File {
        val fileName = when (category) {
            SpeedCategory.SPEEDING_UP -> "custom_speed_up.mp3"
            SpeedCategory.CRUISING -> "custom_cruising.mp3"
            SpeedCategory.SLOWING_DOWN -> "custom_slowing_down.mp3"
            SpeedCategory.UNKNOWN -> "custom_background.mp3"
        }
        return File(context.filesDir, fileName)
    }



    /**
     * Prüft ob eine lokale Datei existiert, ansonsten wird das Raw-Fallback genutzt
     */
    private fun loadCategorySound(category: SpeedCategory, fallbackRawId: Int) {
        val customFile = getCustomSoundFile(category)

        val soundId = if (customFile.exists()) {
            Log.d(
                "MediaPlayerPlus",
                "Loading custom sound for $category from ${customFile.absolutePath}"
            )
            soundPool?.load(customFile.absolutePath, 1) ?: 0
        } else {
            Log.d("MediaPlayerPlus", "Loading default sound for $category from resources")
            soundPool?.load(context, fallbackRawId, 1) ?: 0
        }

        soundMap[category] = soundId
    }

    /**
     * Ermöglicht es, die Sounds neu zu laden, nachdem eine Datei getauscht wurde
     */
    fun reloadSounds() {
        soundsLoaded = false
        soundsToLoadCount = 0
        soundMap.clear()
        soundPool?.release()
        setupSoundPoolAndLoadSounds()
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