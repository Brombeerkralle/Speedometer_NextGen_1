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
    private var soundMap: MutableMap<String, Int> = mutableMapOf()

    private var backgroundPlayer: MediaPlayer? = null
    private var backgroundVolume: Float = initialBackgroundVolume
    private var indicatorVolume: Float = initialIndicatorVolume

    var soundsLoaded = false
    private var soundsToLoadCount = 0
    private var totalSoundsToLoad = 0

    private val audioPrefs: SharedPreferences = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
    private val idealCruiseSpeeds = listOf(30, 50, 70, 80)

    init {
        setupSoundPoolAndLoadSounds()
        playSilentAudio()
    }

    private fun getSoundKey(category: SpeedCategory, targetSpeed: Int?): String {
        return if (targetSpeed == null || category == SpeedCategory.UNKNOWN) {
            category.name
        } else {
            "${category.name}_$targetSpeed"
        }
    }

    private fun setupSoundPoolAndLoadSounds() {
        Log.d("MediaPlayerPlus", "Setting up SoundPool and loading sounds...")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val categories = listOf(SpeedCategory.SPEEDING_UP, SpeedCategory.CRUISING, SpeedCategory.SLOWING_DOWN)
        totalSoundsToLoad = (categories.size * idealCruiseSpeeds.size) + 1 // +1 for background

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsToLoadCount++
                if (soundsToLoadCount >= totalSoundsToLoad) {
                    soundsLoaded = true
                    Log.i("MediaPlayerPlus", "All sounds loaded successfully ($soundsToLoadCount).")
                }
            }
        }

        // Load background sound
        loadSound(SpeedCategory.UNKNOWN, null, R.raw.hintergrundemaestro2)

        // Load sounds for each category and ideal speed
        for (speed in idealCruiseSpeeds) {
            loadSound(SpeedCategory.SPEEDING_UP, speed, R.raw.speedupmaestro3)
            loadSound(SpeedCategory.CRUISING, speed, R.raw.maintainmaestro3)
            loadSound(SpeedCategory.SLOWING_DOWN, speed, R.raw.downmaestro3)
        }
    }

    fun playMusic(category: SpeedCategory, targetSpeed: Int? = null) {
        if (!soundsLoaded || !isSoundEnabled(category, targetSpeed)) {
            return
        }

        val key = getSoundKey(category, targetSpeed)
        val soundId = soundMap[key]
        if (soundId != null && soundId != 0) {
            soundPool?.play(soundId, indicatorVolume, indicatorVolume, 0, 0, 1.0f)
            Log.i("MediaPlayerPlus", "Audio played for: $key")
        }
    }

    fun playSilentAudio() {
        if (backgroundPlayer != null) return
        if (!isSoundEnabled(SpeedCategory.UNKNOWN, null)) return

        val customFile = getCustomSoundFile(SpeedCategory.UNKNOWN, null)
        
        try {
            backgroundPlayer = if (customFile.exists()) {
                MediaPlayer().apply {
                    setDataSource(customFile.absolutePath)
                    prepare()
                }
            } else {
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
            Log.e("MediaPlayerPlus", "Error background player", e)
        }
    }

    fun isSoundEnabled(category: SpeedCategory, targetSpeed: Int?): Boolean {
        val key = getSoundKey(category, targetSpeed)
        return audioPrefs.getBoolean("enabled_$key", true)
    }

    fun setSoundEnabled(category: SpeedCategory, targetSpeed: Int?, enabled: Boolean) {
        val key = getSoundKey(category, targetSpeed)
        audioPrefs.edit().putBoolean("enabled_$key", enabled).apply()
        if (category == SpeedCategory.UNKNOWN) {
            if (enabled) playSilentAudio() else releaseBackgroundPlayer()
        }
    }

    fun getCustomSoundFile(category: SpeedCategory, targetSpeed: Int?): File {
        val key = getSoundKey(category, targetSpeed)
        return File(context.filesDir, "custom_$key.mp3")
    }

    private fun loadSound(category: SpeedCategory, targetSpeed: Int?, fallbackRawId: Int) {
        val customFile = getCustomSoundFile(category, targetSpeed)
        val key = getSoundKey(category, targetSpeed)

        val soundId = if (customFile.exists()) {
            soundPool?.load(customFile.absolutePath, 1) ?: 0
        } else {
            soundPool?.load(context, fallbackRawId, 1) ?: 0
        }

        soundMap[key] = soundId
    }

    fun reloadSounds() {
        soundsLoaded = false
        soundsToLoadCount = 0
        soundMap.clear()
        releaseBackgroundPlayer()
        soundPool?.release()
        setupSoundPoolAndLoadSounds()
        if (isSoundEnabled(SpeedCategory.UNKNOWN, null)) {
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
