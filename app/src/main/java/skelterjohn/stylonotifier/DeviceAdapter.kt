package skelterjohn.stylonotifier

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DeviceItem(val name: String, val source: String, var isMonitored: Boolean)

class DeviceAdapter(
    private val devices: List<DeviceItem>,
    private val onToggle: (DeviceItem) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.deviceCheckBox)
        val nameTextView: TextView = view.findViewById(R.id.deviceNameTextView)
        val sourceTextView: TextView = view.findViewById(R.id.deviceSourceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.nameTextView.text = device.name
        holder.sourceTextView.text = device.source
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = device.isMonitored
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            device.isMonitored = isChecked
            onToggle(device)
        }
    }

    override fun getItemCount() = devices.size
}
