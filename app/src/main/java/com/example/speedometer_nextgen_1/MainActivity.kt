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
    private var pendingTargetSpeed: Int? = null

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
        val menu = popup.menu

        // Background Sound Option
        val bgItem = menu.add("Hintergrund Sound")
        updateMenuItemStyle(bgItem, SpeedCategory.UNKNOWN, null)
        bgItem.setOnMenuItemClickListener {
            showCategoryOptions(SpeedCategory.UNKNOWN, null)
            true
        }

        // Dynamic Speed Levels
        speedManagement.idealCruiseSpeeds.forEach { speed ->
            val subMenu = menu.addSubMenu("Level: $speed km/h")
            
            val upItem = subMenu.add("Beschleunigen auf $speed")
            updateMenuItemStyle(upItem, SpeedCategory.SPEEDING_UP, speed)
            upItem.setOnMenuItemClickListener {
                showCategoryOptions(SpeedCategory.SPEEDING_UP, speed)
                true
            }

            val cruiseItem = subMenu.add("Cruising bei $speed")
            updateMenuItemStyle(cruiseItem, SpeedCategory.CRUISING, speed)
            cruiseItem.setOnMenuItemClickListener {
                showCategoryOptions(SpeedCategory.CRUISING, speed)
                true
            }

            val downItem = subMenu.add("Bremsen ab $speed")
            updateMenuItemStyle(downItem, SpeedCategory.SLOWING_DOWN, speed)
            downItem.setOnMenuItemClickListener {
                showCategoryOptions(SpeedCategory.SLOWING_DOWN, speed)
                true
            }
        }

        popup.show()
    }

    private fun updateMenuItemStyle(item: android.view.MenuItem, category: SpeedCategory, targetSpeed: Int?) {
        val isEnabled = mediaPlayerPlus.isSoundEnabled(category, targetSpeed)
        val hasCustom = isCustomSoundPresent(category, targetSpeed)
        
        val statusIcon = if (isEnabled) "●" else "○"
        val customIcon = if (hasCustom) " ✓" else ""
        
        val newTitle = "$statusIcon ${item.title}$customIcon"
        val spanString = SpannableString(newTitle)
        
        spanString.setSpan(ForegroundColorSpan(if (isEnabled) Color.GREEN else Color.RED), 0, 1, 0)
        item.title = spanString
    }

    private fun showCategoryOptions(category: SpeedCategory, targetSpeed: Int?) {
        val isEnabled = mediaPlayerPlus.isSoundEnabled(category, targetSpeed)
        val hasCustom = isCustomSoundPresent(category, targetSpeed)
        
        val options = mutableListOf<String>()
        options.add(if (isEnabled) "Sound deaktivieren" else "Sound aktivieren")
        options.add("Neue Audio-Datei wählen")
        if (hasCustom) {
            options.add("Auf Standard zurücksetzen")
        }

        val title = if (targetSpeed != null) "${category.name} ($targetSpeed km/h)" else "Hintergrund"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        mediaPlayerPlus.setSoundEnabled(category, targetSpeed, !isEnabled)
                        Toast.makeText(this, "$title ${if (!isEnabled) "aktiviert" else "deaktiviert"}", Toast.LENGTH_SHORT).show()
                    }
                    1 -> openAudioPicker(category, targetSpeed)
                    2 -> {
                        val file = mediaPlayerPlus.getCustomSoundFile(category, targetSpeed)
                        if (file.exists()) {
                            file.delete()
                            mediaPlayerPlus.reloadSounds()
                            Toast.makeText(this, "Standard wiederhergestellt", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun openAudioPicker(category: SpeedCategory, targetSpeed: Int?) {
        pendingCategory = category
        pendingTargetSpeed = targetSpeed
        audioPickerLauncher.launch("audio/*")
    }

    private fun handleSelectedAudio(uri: Uri) {
        val category = pendingCategory ?: return
        val targetSpeed = pendingTargetSpeed
        val destinationFile = mediaPlayerPlus.getCustomSoundFile(category, targetSpeed)

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("AudioSetup", "Saved sound for ${category.name} speed $targetSpeed")
            Toast.makeText(this, "Sound aktualisiert! ✓", Toast.LENGTH_SHORT).show()
            mediaPlayerPlus.reloadSounds()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving audio", e)
            Toast.makeText(this, "Fehler beim Speichern!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isCustomSoundPresent(category: SpeedCategory, targetSpeed: Int?): Boolean {
        return mediaPlayerPlus.getCustomSoundFile(category, targetSpeed).exists()
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
