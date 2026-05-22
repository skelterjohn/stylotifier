package skelterjohn.stylonotifier

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.view.InputDevice
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class StyloWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("stylotifierPrefs", Context.MODE_PRIVATE)
        val monitoredDevices = sharedPrefs.getStringSet("monitored_devices", emptySet()) ?: emptySet()

        if (monitoredDevices.isEmpty()) {
            return Result.success()
        }

        val connectedDevices = InputDevice.getDeviceIds().toList().mapNotNull { id ->
            InputDevice.getDevice(id)
        }.filter { DeviceUtils.isExternalDevice(it) }
            .map { DeviceUtils.getDeviceIdentifier(it) }
            .toSet()

        val currentlyMissing = monitoredDevices.filter { it !in connectedDevices }.toSet()
        val notifiedMissing = sharedPrefs.getStringSet("notified_missing_devices", emptySet()) ?: emptySet()

        // Devices that are now connected should be removed from the notified list
        val newlyConnected = monitoredDevices.filter { it in connectedDevices }.toSet()
        val updatedNotifiedMissing = notifiedMissing.toMutableSet()
        updatedNotifiedMissing.removeAll(newlyConnected)

        // Only notify for devices that are currently missing AND haven't been notified yet
        val newlyMissingToNotify = currentlyMissing.filter { it !in notifiedMissing }

        if (newlyMissingToNotify.isNotEmpty()) {
            showNotification(newlyMissingToNotify)
            updatedNotifiedMissing.addAll(newlyMissingToNotify)
        }

        // Save the updated notified list
        sharedPrefs.edit().putStringSet("notified_missing_devices", updatedNotifiedMissing).apply()

        return Result.success()
    }

    private fun showNotification(missingDevices: List<String>) {
        val channelId = "stylo_notifier_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Device Connection Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val deviceListText = missingDevices.joinToString(", ")
        val contentText = if (missingDevices.size == 1) {
            "Device disconnected: $deviceListText"
        } else {
            "Devices disconnected: $deviceListText"
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Device Disconnected")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
