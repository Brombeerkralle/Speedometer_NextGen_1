package com.example.speedometer_nextgen_1

import android.content.Context
import android.content.SharedPreferences
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
    private val totalSoundsToLoad = 4

    private val audioPrefs: SharedPreferences = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)

    init {
        setupSoundPoolAndLoadSounds()
        playSilentAudio()
    }

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

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsToLoadCount++
                if (soundsToLoadCount == totalSoundsToLoad) {
                    soundsLoaded = true
                    Log.i("MediaPlayerPlus", "All 4 sounds loaded successfully.")
                }
            }
        }

        loadCategorySound(SpeedCategory.SPEEDING_UP, R.raw.speedupmaestro3)
        loadCategorySound(SpeedCategory.CRUISING, R.raw.maintainmaestro3)
        loadCategorySound(SpeedCategory.SLOWING_DOWN, R.raw.downmaestro3)
        loadCategorySound(SpeedCategory.UNKNOWN, R.raw.hintergrundemaestro2)
    }

    fun playMusic(speedCategory: SpeedCategory) {
        if (!soundsLoaded || !isCategoryEnabled(speedCategory)) {
            Log.w("MediaPlayerPlus", "Sound for $speedCategory skipped (Loaded: $soundsLoaded, Enabled: ${isCategoryEnabled(speedCategory)})")
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

    fun playSilentAudio() {
        if (backgroundPlayer != null) return
        if (!isCategoryEnabled(SpeedCategory.UNKNOWN)) {
            Log.d("MediaPlayerPlus", "Background audio disabled, not playing.")
            return
        }

        val customFile = getCustomSoundFile(SpeedCategory.UNKNOWN)
        
        try {
            backgroundPlayer = if (customFile.exists()) {
                Log.d("MediaPlayerPlus", "Playing custom background sound from ${customFile.absolutePath}")
                MediaPlayer().apply {
                    setDataSource(customFile.absolutePath)
                    prepare()
                }
            } else {
                Log.d("MediaPlayerPlus", "Playing default background sound")
                MediaPlayer.create(context, R.raw.hintergrundemaestro2)
            }

            backgroundPlayer?.apply {
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
        } catch (e: Exception) {
            Log.e("MediaPlayerPlus", "Error initializing background player", e)
            // Fallback to default if custom fails
            if (customFile.exists()) {
                 backgroundPlayer = MediaPlayer.create(context, R.raw.hintergrundemaestro2).apply {
                    setVolume(backgroundVolume, backgroundVolume)
                    isLooping = true
                    start()
                }
            }
        }
    }

    fun isCategoryEnabled(category: SpeedCategory): Boolean {
        return audioPrefs.getBoolean("enabled_${category.name}", true)
    }

    fun setCategoryEnabled(category: SpeedCategory, enabled: Boolean) {
        audioPrefs.edit().putBoolean("enabled_${category.name}", enabled).apply()
        Log.d("MediaPlayerPlus", "Category ${category.name} set to enabled=$enabled")
        if (category == SpeedCategory.UNKNOWN) {
            if (enabled) {
                playSilentAudio()
            } else {
                releaseBackgroundPlayer()
            }
        }
    }

    private fun getCustomSoundFile(category: SpeedCategory): File {
        val fileName = when (category) {
            SpeedCategory.SPEEDING_UP -> "custom_speed_up.mp3"
            SpeedCategory.CRUISING -> "custom_cruising.mp3"
            SpeedCategory.SLOWING_DOWN -> "custom_slowing_down.mp3"
            SpeedCategory.UNKNOWN -> "custom_background.mp3"
        }
        return File(context.filesDir, fileName)
    }

    private fun loadCategorySound(category: SpeedCategory, fallbackRawId: Int) {
        val customFile = getCustomSoundFile(category)

        val soundId = if (customFile.exists()) {
            Log.d("MediaPlayerPlus", "Loading custom sound for $category")
            soundPool?.load(customFile.absolutePath, 1) ?: 0
        } else {
            Log.d("MediaPlayerPlus", "Loading default sound for $category")
            soundPool?.load(context, fallbackRawId, 1) ?: 0
        }

        soundMap[category] = soundId
    }

    fun reloadSounds() {
        Log.d("MediaPlayerPlus", "Reloading all sounds...")
        soundsLoaded = false
        soundsToLoadCount = 0
        soundMap.clear()
        
        // Release background player if it exists to allow reloading the file
        releaseBackgroundPlayer()
        
        soundPool?.release()
        setupSoundPoolAndLoadSounds()
        
        // Restart background audio if enabled
        if (isCategoryEnabled(SpeedCategory.UNKNOWN)) {
            playSilentAudio()
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
        backgroundPlayer?.stop()
        backgroundPlayer?.release()
        backgroundPlayer = null
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        releaseBackgroundPlayer()
    }
}
