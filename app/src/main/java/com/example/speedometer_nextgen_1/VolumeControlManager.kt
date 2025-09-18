// File: VolumeControlManager.kt
package com.example.speedometer_nextgen_1

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.SeekBar
import androidx.core.content.edit
import com.example.speedometer_nextgen_1.databinding.DialogVolumeControlBinding
import kotlin.math.pow

class VolumeControlManager(
    private val context: Context,
    private val mediaPlayerPlus: MediaPlayerPlus,
    private val sharedPreferences: SharedPreferences,
    initialBackgroundVolume: Float,
    initialIndicatorVolume: Float,
    var locationService: LocationService? = null // Add this parameter
) {
    private var backgroundVolume: Float = initialBackgroundVolume
    private var indicatorVolume: Float = initialIndicatorVolume
    private var logarythmicVolume = backgroundVolume
    private var isDialogActive = false
    private var isVolumeMaxUnlocked = false  // Renamed from isBackgroundSoundMaxUnlocked



    // Show the volume control dialog
    fun showVolumeControlDialog() {
        val dialogBinding = DialogVolumeControlBinding.inflate(LayoutInflater.from(context))  // Updated to use LayoutInflater.from(context)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Volume Control")
            .setView(dialogBinding.root)
            .setNegativeButton("Confirm", null)
            .create()

        // Set initial SeekBar progress based on current volume
        dialogBinding.backgroundVolumeSeekBar.progress = (backgroundVolume*100).toInt()
        dialogBinding.indicatorVolumeSeekBar.progress = (indicatorVolume*100).toInt()

        // Setup SeekBar listener
        dialogBinding.backgroundVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress > 50 && !isVolumeMaxUnlocked && !isDialogActive) {
                    isDialogActive = true
                    showVolumeWarningDialog(dialogBinding, progress)
                }
                // volume=minVolume+(maxVolume−minVolume)×(progress/maxProgress)^p
                val normalizedProgress = progress / 100.0f
                backgroundVolume = normalizedProgress
                logarythmicVolume = 0.01f + (100 - 1) * normalizedProgress.toDouble().pow(2.toDouble()).toFloat()
                val scaledVolume = logarythmicVolume / 100

                mediaPlayerPlus.updateBackgroundVolume(scaledVolume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveVolume()
            }
        })

        dialogBinding.indicatorVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                // volume=minVolume+(maxVolume−minVolume)×(progress/maxProgress)
                indicatorVolume = progress / 100.0f
                mediaPlayerPlus.updateIndicatorVolume(indicatorVolume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveVolume()
                /*val intent = Intent("com.example.speedometer_nextgen_1.LOCATION_UPDATE").apply {
                    putExtra("speed",50)
                    putExtra("speedDecimal", "55")
                    putExtra("accelerationMagnitude", 55f)
                    putExtra("gpsLocationAccuracy", 55f)
                }
                context.sendBroadcast(intent)  // Replacing LocalBroadcastManager*/

                locationService?.triggerTestUpdate()

            }
        })

        dialog.show()

        // Set custom dimensions for the dialog
        dialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            (context.resources.displayMetrics.heightPixels * 0.6).toInt()
        )
    }

    private fun saveVolume() {
        sharedPreferences.edit {
            putFloat("backgroundVolume", backgroundVolume)
            putFloat("indicatorVolume", indicatorVolume)
        }
    }

    // Show warning dialog for high volume
    private fun showVolumeWarningDialog(dialogBinding: DialogVolumeControlBinding, progress: Int) {
        AlertDialog.Builder(context)
            .setTitle("Volume Warning")
            .setMessage("Listening at high volumes may damage hearing. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                isDialogActive = false  // Reset dialog state
                isVolumeMaxUnlocked = true
                dialogBinding.backgroundVolumeSeekBar.progress = progress  // Unlock to chosen volume
            }
            .setNegativeButton("No") { _, _ ->
                isDialogActive = false  // Reset dialog state
                dialogBinding.backgroundVolumeSeekBar.progress = 50  // Limit volume to 50%
            }
            .setCancelable(false)
            .show()
    }


}
