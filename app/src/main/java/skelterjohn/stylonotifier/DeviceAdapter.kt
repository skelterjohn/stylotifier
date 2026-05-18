package skelterjohn.stylonotifier

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DeviceItem(val name: String, val source: String, var isMonitored: Boolean)

sealed class DisplayItem {
    data class Header(val title: String, val source: String, var isExpanded: Boolean) : DisplayItem()
    data class Device(val device: DeviceItem) : DisplayItem()
}

class DeviceAdapter(
    private var allDevices: List<DeviceItem>,
    private val onToggle: (DeviceItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val expandedGroups = mutableSetOf<String>()
    private var displayItems: List<DisplayItem> = emptyList()

    init {
        updateDisplayItems()
    }

    fun updateDevices(newDevices: List<DeviceItem>) {
        allDevices = newDevices
        updateDisplayItems()
        notifyDataSetChanged()
    }

    private fun updateDisplayItems() {
        val items = mutableListOf<DisplayItem>()
        
        // Monitored devices at the top
        val monitored = allDevices.filter { it.isMonitored }
        monitored.forEach { items.add(DisplayItem.Device(it)) }

        // Non-monitored grouped by source
        val nonMonitored = allDevices.filter { !it.isMonitored }
        val grouped = nonMonitored.groupBy { it.source }

        grouped.keys.sorted().forEach { source ->
            val isExpanded = expandedGroups.contains(source)
            items.add(DisplayItem.Header(source, source, isExpanded))
            if (isExpanded) {
                grouped[source]?.forEach { items.add(DisplayItem.Device(it)) }
            }
        }
        
        displayItems = items
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is DisplayItem.Header -> 1
            is DisplayItem.Device -> 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 1) {
            val view = inflater.inflate(R.layout.item_group_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_device, parent, false)
            DeviceViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is DisplayItem.Header -> {
                val vh = holder as HeaderViewHolder
                vh.title.text = item.title
                vh.indicator.rotation = if (item.isExpanded) 0f else -90f
                vh.itemView.setOnClickListener {
                    if (expandedGroups.contains(item.source)) {
                        expandedGroups.remove(item.source)
                    } else {
                        expandedGroups.add(item.source)
                    }
                    updateDisplayItems()
                    notifyDataSetChanged()
                }
            }
            is DisplayItem.Device -> {
                val vh = holder as DeviceViewHolder
                val device = item.device
                vh.name.text = device.name
                vh.source.text = device.source
                vh.checkBox.setOnCheckedChangeListener(null)
                vh.checkBox.isChecked = device.isMonitored
                vh.checkBox.setOnCheckedChangeListener { _, isChecked ->
                    device.isMonitored = isChecked
                    updateDisplayItems()
                    notifyDataSetChanged()
                    onToggle(device)
                }
            }
        }
    }

    override fun getItemCount() = displayItems.size

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.deviceCheckBox)
        val name: TextView = view.findViewById(R.id.deviceNameTextView)
        val source: TextView = view.findViewById(R.id.deviceSourceTextView)
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.groupTitleTextView)
        val indicator: ImageView = view.findViewById(R.id.expandIndicator)
    }
}
