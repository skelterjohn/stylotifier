package skelterjohn.stylonotifier

import android.os.Build
import android.view.InputDevice

object DeviceUtils {
    fun getConnectionType(device: InputDevice): String {
        val info = device.toString()
        val busMatch = Regex("bus[=:]\\s*0x([0-9a-fA-F]+)").find(info)
        val bus = busMatch?.groupValues?.get(1)?.lowercase()

        return when (bus) {
            "3", "0003" -> "USB"
            "5", "0005" -> "Bluetooth"
            else -> {
                val locationMatch = Regex("(?i)Location:\\s*(.+)").find(info)
                if (locationMatch != null) return locationMatch.groupValues[1].trim()
                "External"
            }
        }
    }

    fun isExternalDevice(device: InputDevice): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            device.isExternal
        } else {
            !device.isVirtual
        }
    }

    fun getDeviceIdentifier(device: InputDevice): String {
        return "${device.name} (${getConnectionType(device)})"
    }
}
