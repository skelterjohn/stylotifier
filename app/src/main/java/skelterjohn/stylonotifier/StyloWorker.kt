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
        val sharedPrefs = applicationContext.getSharedPreferences("StyloNotifierPrefs", Context.MODE_PRIVATE)
        val monitoredDevices = sharedPrefs.getStringSet("monitored_devices", emptySet()) ?: emptySet()

        if (monitoredDevices.isEmpty()) {
            return Result.success()
        }

        val connectedDevices = InputDevice.getDeviceIds().map { id ->
            InputDevice.getDevice(id)?.name
        }.filterNotNull().toSet()

        val missingDevices = monitoredDevices.filter { it !in connectedDevices }

        if (missingDevices.isNotEmpty()) {
            showNotification(missingDevices)
        }

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
