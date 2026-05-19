package skelterjohn.stylonotifier

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private val deviceItems = mutableListOf<DeviceItem>()

    private lateinit var rescanProgress: com.google.android.material.progressindicator.CircularProgressIndicator
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var progressAnimator: android.animation.ValueAnimator? = null
    
    private val rescanRunnable = object : Runnable {
        override fun run() {
            setupDeviceList()
            startProgressAnimation()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        rescanProgress = findViewById(R.id.rescanProgress)

        setupDeviceList()
        checkNotificationPermission()
        scheduleWork()
    }

    private fun startProgressAnimation() {
        progressAnimator?.cancel()
        progressAnimator = android.animation.ValueAnimator.ofInt(0, 100).apply {
            duration = 5000
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { animator ->
                rescanProgress.progress = animator.animatedValue as Int
            }
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(rescanRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(rescanRunnable)
        progressAnimator?.cancel()
    }

    private fun setupDeviceList() {
        val sharedPrefs = getSharedPreferences("stylotifierPrefs", Context.MODE_PRIVATE)
        val monitoredDevices = sharedPrefs.getStringSet("monitored_devices", emptySet()) ?: emptySet()

        val deviceIds = InputDevice.getDeviceIds()
        val connectedDevices = mutableMapOf<String, String>()
        for (id in deviceIds) {
            val device = InputDevice.getDevice(id)
            if (device != null) {
                val isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    device.isExternal
                } else {
                    !device.isVirtual
                }
                val source = if (isExternal) "External (Bluetooth/USB)" else "System/Internal"
                connectedDevices[device.name] = source
            }
        }

        deviceItems.clear()

        // Add all currently connected devices
        for ((name, source) in connectedDevices) {
            deviceItems.add(DeviceItem(name, source, monitoredDevices.contains(name)))
        }

        // Add monitored devices that are not currently connected
        monitoredDevices.forEach { name ->
            if (deviceItems.none { it.name == name }) {
                deviceItems.add(DeviceItem(name, "Saved (Disconnected)", true))
            }
        }

        if (!::adapter.isInitialized) {
            adapter = DeviceAdapter(deviceItems) {
                updateMonitoredDevices()
            }
            deviceRecyclerView.adapter = adapter
        } else {
            adapter.updateDevices(deviceItems)
        }
    }

    private fun updateMonitoredDevices() {
        val sharedPrefs = getSharedPreferences("stylotifierPrefs", Context.MODE_PRIVATE)
        val monitoredSet = deviceItems.filter { it.isMonitored }.map { it.name }.toSet()
        sharedPrefs.edit().putStringSet("monitored_devices", monitoredSet).apply()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    private fun scheduleWork() {
        val workRequest = PeriodicWorkRequestBuilder<StyloWorker>(30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "StyloCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
