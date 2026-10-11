package com.example.speedometer_nextgen_1

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.WindowManager
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.speedometer_nextgen_1.databinding.ActivityMainBinding
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.util.Locale

private const val ACTION_EXTERNAL_SHUTDOWN = "com.example.speedometer_nextgen_1.FORCE_SHUTDOWN"

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private lateinit var binding: ActivityMainBinding
    private val speedManagement: SpeedManagement by inject()
    private val mediaPlayerPlus: MediaPlayerPlus by inject()
    private val volumeControlManager: VolumeControlManager by inject { parametersOf(this) }

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    private var locationService: LocationService? = null
    private var isBound = false

    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedAudio(it) }
    }

    private var pendingCategory: SpeedCategory? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            isBound = true
            volumeControlManager.locationService = locationService
            observeLocationData()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filter = android.content.IntentFilter(ACTION_EXTERNAL_SHUTDOWN)
        ContextCompat.registerReceiver(
            this,
            shutdownReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        initializeLayout()
        otherInits()

        findViewById<Button>(R.id.openVolumeControlButton).setOnClickListener {
            volumeControlManager.showVolumeControlDialog()
        }

        findViewById<Button>(R.id.destroyApp).setOnClickListener {
            finishAndRemoveTask()
        }

        binding.menuButton.setOnClickListener { view ->
            showAudioPopupMenu(view)
        }
    }

    private fun showAudioPopupMenu(view: android.view.View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.audio_selection_menu, popup.menu)

        val menu = popup.menu
        val categoryMap = mapOf(
            R.id.menu_speed_up to SpeedCategory.SPEEDING_UP,
            R.id.menu_cruising to SpeedCategory.CRUISING,
            R.id.menu_slow_down to SpeedCategory.SLOWING_DOWN,
            R.id.menu_background to SpeedCategory.UNKNOWN
        )

        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val category = categoryMap[item.itemId] ?: continue
            
            val isEnabled = mediaPlayerPlus.isCategoryEnabled(category)
            val hasCustom = isCustomSoundPresent(category)
            
            val statusIcon = if (isEnabled) "●" else "○"
            val customIcon = if (hasCustom) " ✓" else ""
            
            val newTitle = "$statusIcon ${item.title}$customIcon"
            val spanString = SpannableString(newTitle)
            
            if (isEnabled) {
                spanString.setSpan(ForegroundColorSpan(Color.GREEN), 0, 1, 0)
            } else {
                spanString.setSpan(ForegroundColorSpan(Color.RED), 0, 1, 0)
            }
            
            item.title = spanString
        }

        popup.setOnMenuItemClickListener { item ->
            val category = categoryMap[item.itemId]
            if (category != null) {
                showCategoryOptions(category)
            }
            true
        }
        popup.show()
    }

    private fun showCategoryOptions(category: SpeedCategory) {
        val isEnabled = mediaPlayerPlus.isCategoryEnabled(category)
        val hasCustom = isCustomSoundPresent(category)
        
        val options = mutableListOf<String>()
        options.add(if (isEnabled) "Disable Sound" else "Enable Sound")
        options.add("Select New Sound File")
        if (hasCustom) {
            options.add("Reset to Default Sound")
        }

        val categoryName = category.name.replace("_", " ").lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        AlertDialog.Builder(this)
            .setTitle("$categoryName Settings")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        mediaPlayerPlus.setCategoryEnabled(category, !isEnabled)
                        Toast.makeText(this, "$categoryName ${if (!isEnabled) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                    }
                    1 -> openAudioPicker(category)
                    2 -> {
                        val file = File(filesDir, getFileNameForCategory(category))
                        if (file.exists()) {
                            file.delete()
                            mediaPlayerPlus.reloadSounds()
                            Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun openAudioPicker(category: SpeedCategory) {
        pendingCategory = category
        audioPickerLauncher.launch("audio/*")
    }

    private fun handleSelectedAudio(uri: Uri) {
        val category = pendingCategory ?: return
        val fileName = getFileNameForCategory(category)
        val destinationFile = File(filesDir, fileName)

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("AudioSetup", "Saved sound for ${category.name}")
            Toast.makeText(this, "Sound for ${category.name} updated! ✓", Toast.LENGTH_SHORT).show()
            mediaPlayerPlus.reloadSounds()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving audio for $category", e)
            Toast.makeText(this, "Error saving audio!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isCustomSoundPresent(category: SpeedCategory): Boolean {
        return File(filesDir, getFileNameForCategory(category)).exists()
    }

    private fun getFileNameForCategory(category: SpeedCategory): String {
        return when (category) {
            SpeedCategory.SPEEDING_UP -> "custom_speed_up.mp3"
            SpeedCategory.CRUISING -> "custom_cruising.mp3"
            SpeedCategory.SLOWING_DOWN -> "custom_slowing_down.mp3"
            SpeedCategory.UNKNOWN -> "custom_background.mp3"
        }
    }

    private fun initializeLayout() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, systemBarsInsets.bottom)
            insets
        }
        updateSystemBarsColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun otherInits() {
        checkAndRequestLocationPermission()
        val indicatorAudioIntent = Intent(this, IndicatorAudioForegroundservice::class.java)
        ContextCompat.startForegroundService(this, indicatorAudioIntent)
        mediaPlayerPlus.playSilentAudio()
    }

    private fun observeLocationData() {
        locationService?.speedData?.observe(this) { speed ->
            binding.currentSpeedId.text = speed.toString()
            handleSpeedIndicators(speed)
        }
        locationService?.speedDecimalData?.observe(this) { speedDecimal ->
            binding.currentSpeedDecimalId.text = speedDecimal
        }
        locationService?.gpsLocationAccuracyData?.observe(this) { accuracy ->
            binding.infotainmentIDright.text = accuracy.toString().take(4)
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            startLocationService()
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun handleSpeedIndicators(speed: Int) {
        val speedHasChanged = speedManagement.hasSpeedChanged(speed)
        val categoryHasChanged = speedManagement.hasCategoryChangedFlag()
        
        if (categoryHasChanged) {
            binding.infotainmentIDleft.text = getString(R.string.music_indicator_text)
        } else {
            binding.infotainmentIDleft.text = " "
        }

        if (speedHasChanged) {
            speedManagement.updateBackgroundColor(speed) { color ->
                binding.root.setBackgroundColor(color)
                updateSystemBarsColor(color)
            }
        }
    }

    private fun updateSystemBarsColor(color: Int) {
        window.statusBarColor = color
        window.navigationBarColor = color
        val insetsController = window.insetsController
        if (insetsController != null) {
            val appearance = if (ColorUtils.calculateLuminance(color) > 0.5) {
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            } else 0
            insetsController.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {}
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}
    override fun onRationaleAccepted(requestCode: Int) {}
    override fun onRationaleDenied(requestCode: Int) {}

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, LocationService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private val shutdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_EXTERNAL_SHUTDOWN) {
                finishAndRemoveTask()
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(shutdownReceiver) } catch (e: Exception) {}
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopService(Intent(this, LocationService::class.java))
        stopService(Intent(this, IndicatorAudioForegroundservice::class.java))
        mediaPlayerPlus.release()
        super.onDestroy()
    }
}
