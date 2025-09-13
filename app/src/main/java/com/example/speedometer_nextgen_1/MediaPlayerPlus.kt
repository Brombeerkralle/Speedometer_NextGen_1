package com.example.speedometer_nextgen_1

import android.content.Context
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


    private var soundsLoaded = false
    private var soundsToLoad = 0
    private var soundsLoadedCount = 0

    var onSoundsLoadedListener: (() -> Unit)? = null

    init {
        loadSounds()
    }

    fun loadSounds() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        // SoundPool neu erstellen
        soundPool?.release()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4) // You can play up to 3 sounds at the same time
            .setAudioAttributes(audioAttributes)
            .build()

        soundsLoaded = false
        soundsLoadedCount = 0


        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                soundsLoadedCount++
                Log.d("MediaPlayerPlus", "Sound loaded: $sampleId ($soundsLoadedCount/$soundsToLoad)")
                if (soundsLoadedCount >= soundsToLoad) {
                    soundsLoaded = true
                    Log.d("MediaPlayerPlus", "\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\nAll sounds loaded\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n")
                    onSoundsLoadedListener?.invoke()  // Callback auslösen
                }
            } else {
                Log.w("MediaPlayerPlus", "Failed to load sound ID: $sampleId")
            }
        }

        // Vorherige Sound Map leeren
        soundMap.clear()

        // Lade Sounds und zähle nur erfolgreich geladene
        val loadedIds = listOf(
            soundPool?.load(context, R.raw.speedupmaestro3, 1) ?: 0,
            soundPool?.load(context, R.raw.maintainmaestro3, 1) ?: 0,
            soundPool?.load(context, R.raw.downmaestro3, 1) ?: 0,
            soundPool?.load(context, R.raw.hintergrundemaestro2, 1) ?: 0
        )

        soundsToLoad = 0

        SpeedCategory.entries.forEachIndexed { index, category ->
            val soundId = loadedIds.getOrElse(index) { 0 }
            if (soundId != 0) {
                soundMap[category] = soundId
                soundsToLoad++
            } else {
                Log.w("MediaPlayerPlus", "Failed to load sound for category $category")
            }
        }
        val testsound = soundMap[SpeedCategory.UNKNOWN]
        if (testsound != null && testsound != 0) {
            soundPool?.play(testsound, indicatorVolume, indicatorVolume, 0, 0, 1.0f)
            Log.w("MediaPlayerPlus", "Audio Played")
            /*TODO warum spielt dieses AUdio nicht ab?*/
        }
    }
    // Play nur, wenn SoundPool existiert und alle Sounds geladen sind
    fun playMusic(speedCategory: SpeedCategory) {
        if (soundPool == null) {
            Log.w("MediaPlayerPlus", "SoundPool is null; cannot play sound")
            // Optionally reinitialize:
            loadSounds()
            return
        }
        if (!soundsLoaded) {
            Log.w("MediaPlayerPlus", "Sounds not loaded yet")
            return
        }
        val soundId = soundMap[speedCategory]
        if (soundId != null && soundId != 0) {
            soundPool?.play(soundId, indicatorVolume, indicatorVolume, 0, 0, 1.0f)
            Log.w("MediaPlayerPlus", "Audio Played")
        } else {
            Log.w("MediaPlayerPlus", "Could not play sound for $speedCategory (ID: $soundId)")
        }
    }


    // Optional: Silent audio im Hintergrund abspielen, um Audio-Fokus zu behalten
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
