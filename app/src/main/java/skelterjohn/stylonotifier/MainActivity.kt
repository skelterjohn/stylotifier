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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)

        setupDeviceList()
        checkNotificationPermission()
        scheduleWork()
    }

    private fun setupDeviceList() {
        val sharedPrefs = getSharedPreferences("StyloNotifierPrefs", Context.MODE_PRIVATE)
        val monitoredDevices = sharedPrefs.getStringSet("monitored_devices", emptySet()) ?: emptySet()

        val deviceIds = InputDevice.getDeviceIds()
        val connectedDeviceNames = deviceIds.map { id ->
            InputDevice.getDevice(id)?.name
        }.filterNotNull().toSet()

        deviceItems.clear()

        // Add all currently connected devices
        connectedDeviceNames.forEach { name ->
            deviceItems.add(DeviceItem(name, monitoredDevices.contains(name)))
        }

        // Add monitored devices that are not currently connected
        monitoredDevices.forEach { name ->
            if (deviceItems.none { it.name == name }) {
                deviceItems.add(DeviceItem(name, true))
            }
        }

        adapter = DeviceAdapter(deviceItems) {
            updateMonitoredDevices()
        }
        deviceRecyclerView.adapter = adapter
    }

    private fun updateMonitoredDevices() {
        val sharedPrefs = getSharedPreferences("StyloNotifierPrefs", Context.MODE_PRIVATE)
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
