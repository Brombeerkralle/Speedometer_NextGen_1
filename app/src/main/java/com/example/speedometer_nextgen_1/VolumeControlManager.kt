// File: VolumeControlManager.kt
package com.example.speedometer_nextgen_1

import android.app.AlertDialog
import android.content.Context
import android.view.WindowManager
import android.widget.SeekBar
import com.example.speedometer_nextgen_1.databinding.DialogVolumeControlBinding

class VolumeControlManager(
    private val context: Context,
    private val mediaPlayerPlus: MediaPlayerPlus
) {
    var volume = 35.5f  // Initial volume
    private var isDialogActive = false
    private var isVolumeMaxUnlocked = false  // Renamed from isBackgroundSoundMaxUnlocked

    // Show the volume control dialog
    fun showVolumeControlDialog() {
        val dialogBinding = DialogVolumeControlBinding.inflate((context as MainActivity).layoutInflater)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Volume Control")
            .setView(dialogBinding.root)
            .setNegativeButton("Confirm", null)
            .create()

        // Set initial SeekBar progress based on current volume
        dialogBinding.volumeSeekBar.progress = (volume * 100).toInt()

        // Setup SeekBar listener
        dialogBinding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress > 50 && !isVolumeMaxUnlocked && !isDialogActive) {
                    isDialogActive = true
                    showVolumeWarningDialog(dialogBinding, progress)
                }

                // Update volume based on SeekBar's progress (scale 0-100)
                volume = dialogBinding.volumeSeekBar.progress / 100.0f
                mediaPlayerPlus.updateBackgroundVolume(volume)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialog.show()

        // Set custom dimensions for the dialog
        dialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            (context.resources.displayMetrics.heightPixels * 0.6).toInt()
        )
    }

    // Show warning dialog for high volume
    private fun showVolumeWarningDialog(dialogBinding: DialogVolumeControlBinding, progress: Int) {
        AlertDialog.Builder(context)
            .setTitle("Volume Warning")
            .setMessage("Listening at high volumes may damage hearing. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                isDialogActive = false  // Reset dialog state
                isVolumeMaxUnlocked = true
                dialogBinding.volumeSeekBar.progress = progress  // Unlock to chosen volume
            }
            .setNegativeButton("No") { _, _ ->
                isDialogActive = false  // Reset dialog state
                dialogBinding.volumeSeekBar.progress = 50  // Limit volume to 50%
            }
            .setCancelable(false)
            .show()
    }
}