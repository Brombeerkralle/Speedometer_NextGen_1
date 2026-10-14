package com.example.speedometer_nextgen_1

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.io.File

/**
 * MediaPlayerPlus: Manages all audio operations for the Speedometer.
 * - Silent background audio loop (Bluetooth disconnect blocker) at ultra-low inaudible volume.
 * - SoundPool indicator sounds with immediate priority interruption for the latest speed value.
 */
class MediaPlayerPlus(
    private val context: Context,
    private val initialBackgroundVolume: Float,
    private val initialIndicatorVolume: Float
) {

    // ================= ================= =================
    // Properties & State
    // ================= ================= =================

    private var soundPool: SoundPool? = null
    private val soundMap: MutableMap<String, Int> = mutableMapOf()

    private var backgroundPlayer: MediaPlayer? = null
    // Ensure background volume is safe and ultra-low (inaudible) on startup to prevent loud noise
    private var backgroundVolume: Float = if (initialBackgroundVolume > 0.05f) 0.005f else initialBackgroundVolume
    private var indicatorVolume: Float = initialIndicatorVolume

    var soundsLoaded = false
        private set
    private var soundsToLoadCount = 0
    private var totalSoundsToLoad = 0
    private var lastStreamId: Int = 0

    private val audioPrefs: SharedPreferences = context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
    private val idealCruiseSpeeds = listOf(30, 50, 70, 80)

    // ================= ================= =================
    // Initialization & Sound Loading
    // ================= ================= =================

    init {
        try {
            setupSoundPoolAndLoadSounds()
            playSilentAudio()
        } catch (e: Exception) {
            Log.e("MediaPlayerPlus", "Error during initialization", e)
        }
    }

    private fun setupSoundPoolAndLoadSounds() {
        Log.d("MediaPlayerPlus", "Setting up SoundPool and loading audio resources...")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val categories = listOf(SpeedCategory.SPEEDING_UP, SpeedCategory.CRUISING, SpeedCategory.SLOWING_DOWN)
        totalSoundsToLoad = (categories.size * idealCruiseSpeeds.size) + 1 // +1 for background track

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundsToLoadCount++
                if (soundsToLoadCount >= totalSoundsToLoad) {
                    soundsLoaded = true
                    Log.i("MediaPlayerPlus", "All audio resources loaded successfully ($soundsToLoadCount).")
                }
            }
        }

        // Load background BT disconnect blocker sound
        loadSound(SpeedCategory.UNKNOWN, null, R.raw.hintergrundemaestro2)

        // Load speed indicator sounds for each cruise level
        for (speed in idealCruiseSpeeds) {
            loadSound(SpeedCategory.SPEEDING_UP, speed, R.raw.speedupmaestro3)
            loadSound(SpeedCategory.CRUISING, speed, R.raw.maintainmaestro3)
            loadSound(SpeedCategory.SLOWING_DOWN, speed, R.raw.downmaestro3)
        }
    }

    // ================= ================= =================
    // Playback Control (Indicator & Background)
    // ================= ================= =================

    /**
     * Plays speed indicator sound immediately with priority.
     * Stops any currently playing indicator sound so the latest speed value is announced instantly without delay.
     */
    fun playMusic(category: SpeedCategory, targetSpeed: Int? = null) {
        if (!soundsLoaded || !isSoundEnabled(category, targetSpeed)) {
            return
        }

        val key = getSoundKey(category, targetSpeed)
        val soundId = soundMap[key]
        if (soundId != null && soundId != 0) {
            try {
                if (lastStreamId != 0) {
                    soundPool?.stop(lastStreamId)
                }
                lastStreamId = soundPool?.play(soundId, indicatorVolume, indicatorVolume, 0, 0, 1.0f) ?: 0
                Log.i("MediaPlayerPlus", "Audio played immediately for priority: $key (streamId: $lastStreamId)")
            } catch (e: Exception) {
                Log.e("MediaPlayerPlus", "Error playing music for key $key", e)
            }
        }
    }

    /**
     * Stops active indicator sound (called when standing still at a traffic light).
     */
    fun stopIndicatorSound() {
        try {
            if (lastStreamId != 0) {
                soundPool?.stop(lastStreamId)
                lastStreamId = 0
            }
        } catch (e: Exception) {
            Log.e("MediaPlayerPlus", "Error stopping indicator sound", e)
        }
    }

    /**
     * Plays silent/subtle background audio loop at ultra-low volume to act as a Bluetooth disconnect blocker.
     */
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
                // Force safe ultra-low volume on startup so it is inaudible but keeps Bluetooth active
                val effectiveBgVol = if (backgroundVolume > 0.05f) 0.005f else backgroundVolume
                setVolume(effectiveBgVol, effectiveBgVol)
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            Log.e("MediaPlayerPlus", "Error starting background player", e)
        }
    }

    // ================= ================= =================
    // Settings, Preferences & Resource Management
    // ================= ================= =================

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

    private fun getSoundKey(category: SpeedCategory, targetSpeed: Int?): String {
        return if (targetSpeed == null || category == SpeedCategory.UNKNOWN) {
            category.name
        } else {
            "${category.name}_$targetSpeed"
        }
    }

    fun reloadSounds() {
        try {
            soundsLoaded = false
            soundsToLoadCount = 0
            soundMap.clear()
            releaseBackgroundPlayer()
            soundPool?.release()
            setupSoundPoolAndLoadSounds()
            if (isSoundEnabled(SpeedCategory.UNKNOWN, null)) {
                playSilentAudio()
            }
        } catch (e: Exception) {
            Log.e("MediaPlayerPlus", "Error reloading sounds", e)
        }
    }

    fun updateBackgroundVolume(volume: Float) {
        backgroundVolume = if (volume > 0.05f) 0.05f else volume
        backgroundPlayer?.setVolume(backgroundVolume, backgroundVolume)
    }

    fun updateIndicatorVolume(volume: Float) {
        indicatorVolume = volume
    }

    fun releaseBackgroundPlayer() {
        try {
            backgroundPlayer?.stop()
            backgroundPlayer?.release()
            backgroundPlayer = null
        } catch (e: Exception) {
            Log.e("MediaPlayerPlus", "Error releasing background player", e)
        }
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            releaseBackgroundPlayer()
        } catch (e: Exception) {
            Log.e("MediaPlayerPlus", "Error releasing MediaPlayerPlus", e)
        }
    }
}
