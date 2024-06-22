package com.example.devicemonitorapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class NetworkFragment : Fragment() {

    private lateinit var textViewSignalStrength: TextView
    private lateinit var textViewConnectionSpeed: TextView
    private lateinit var textViewDataUsage: TextView
    private lateinit var textViewIPAddress: TextView
    private lateinit var textViewFrequency: TextView
    private lateinit var textViewSSID: TextView
    private lateinit var lineChart: LineChart
    private lateinit var editTextIdle: EditText
    private lateinit var editTextSupplicantScanInterval: EditText
    private lateinit var editTextFrameworkScanInterval: EditText
    private lateinit var imageViewIdle: ImageView
    private lateinit var imageViewSupplicant: ImageView
    private lateinit var imageViewFramework: ImageView
    private lateinit var switchScanAlwaysEnabled: Switch

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val dataEntriesRx = mutableListOf<Entry>()
    private val dataEntriesTx = mutableListOf<Entry>()
    private var previousRxBytes = TrafficStats.getTotalRxBytes()
    private var previousTxBytes = TrafficStats.getTotalTxBytes()
    private var startTime = System.currentTimeMillis()

    companion object {
        private const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1
        private const val TAG = "NetworkFragment"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)

        textViewSignalStrength = view.findViewById(R.id.textViewSignalStrength)
        textViewConnectionSpeed = view.findViewById(R.id.textViewConnectionSpeed)
        textViewDataUsage = view.findViewById(R.id.textViewDataUsage)
        textViewIPAddress = view.findViewById(R.id.textViewIPAddress)
        textViewFrequency = view.findViewById(R.id.textViewFrequency)
        textViewSSID = view.findViewById(R.id.textViewSSID)
        lineChart = view.findViewById(R.id.lineChart)
        editTextIdle = view.findViewById(R.id.editTextIdle)
        editTextSupplicantScanInterval = view.findViewById(R.id.editTextSupplicantScanInterval)
        editTextFrameworkScanInterval = view.findViewById(R.id.editTextFrameworkScanInterval)
        imageViewIdle = view.findViewById(R.id.imageViewIdle)
        imageViewSupplicant = view.findViewById(R.id.imageViewSupplicant)
        imageViewFramework = view.findViewById(R.id.imageViewFramework)
        switchScanAlwaysEnabled = view.findViewById(R.id.switchScanAlwaysEnabled)

        // Check and set the background drawable based on the dark mode setting
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        val root = view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.NetworkLayout)

        if (isNightMode) {
            root.setBackgroundResource(R.drawable.night3) // Replace with your night mode drawable
        } else {
            root.setBackgroundResource(R.drawable.bg1) // Replace with your day mode drawable
        }

        setupLineChart()
        checkPermissionsAndStart()

        imageViewIdle.setOnClickListener {
            setWifiSettingTemporary(it as ImageView, "Idle")
            setWifiIdleSetting(editTextIdle.text.toString().toIntOrNull() ?: 0)
        }
        imageViewSupplicant.setOnClickListener {
            setWifiSettingTemporary(it as ImageView, "Supplicant scan interval")
            setWifiSupplicantScanInterval(editTextSupplicantScanInterval.text.toString().toIntOrNull() ?: 0)
        }
        imageViewFramework.setOnClickListener {
            setWifiSettingTemporary(it as ImageView, "Framework scan interval")
            setWifiFrameworkScanInterval(editTextFrameworkScanInterval.text.toString().toIntOrNull() ?: 0)
        }

        switchScanAlwaysEnabled.setOnCheckedChangeListener { _, isChecked ->
            setWifiScanAlwaysEnabled(isChecked)
        }

        view.findViewById<ImageButton>(R.id.infoIdle).setOnClickListener {
            showInfoPopup(getString(R.string.idle_title),
                getString(R.string.idle_content)
            )
        }
        view.findViewById<ImageButton>(R.id.infoSupplicantScanInterval).setOnClickListener {
            showInfoPopup(getString(R.string.supplicant_interval_title),
                getString(R.string.supplicant_interval_content)
            )
        }
        view.findViewById<ImageButton>(R.id.infoFrameworkScanInterval).setOnClickListener {
            showInfoPopup(getString(R.string.framework_interval_title),
                getString(R.string.framework_interval_content)
            )
        }
        view.findViewById<ImageButton>(R.id.infoScanAlwaysEnabled).setOnClickListener {
            showInfoPopup(getString(R.string.switch_scan_title),
                getString(R.string.switch_scan_content)
            )
        }

        return view
    }

    private fun setWifiSettingTemporary(imageView: ImageView, settingName: String) {
        val originalColor = imageView.background
        imageView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary))
        Toast.makeText(requireContext(), "$settingName has been set", Toast.LENGTH_SHORT).show()

        coroutineScope.launch {
            delay(1000) // Se mentine culoarea 1 secunda
            imageView.background = originalColor
        }
    }

    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION
            )
        } else {
            updateNetworkStats()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateNetworkStats()
        }
    }
    private fun showInfoPopup(title: String, content: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun setupLineChart() {
        val lineDataSetRx = LineDataSet(dataEntriesRx, "Received Data")
        lineDataSetRx.lineWidth = 2.5f
        lineDataSetRx.setDrawCircles(false)
        lineDataSetRx.color = resources.getColor(R.color.colorPrimary, null)

        val lineDataSetTx = LineDataSet(dataEntriesTx, "Sent Data")
        lineDataSetTx.lineWidth = 2.5f
        lineDataSetTx.setDrawCircles(false)
        lineDataSetTx.color = resources.getColor(R.color.colorSecondary, null)

        lineChart.data = LineData(lineDataSetRx, lineDataSetTx)
        lineChart.description.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.granularity = 1f
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.invalidate() // Refresh the chart
    }

    private fun updateLineChart() {
        val currentTime = (System.currentTimeMillis() - startTime) / 1000f
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()

        val rxBytesDiff = (currentRxBytes - previousRxBytes) / 1024f // Convert to KB
        val txBytesDiff = (currentTxBytes - previousTxBytes) / 1024f // Convert to KB

        dataEntriesRx.add(Entry(currentTime, rxBytesDiff))
        dataEntriesTx.add(Entry(currentTime, txBytesDiff))

        val lineDataSetRx = LineDataSet(dataEntriesRx, "Received Data")
        lineDataSetRx.lineWidth = 2.5f
        lineDataSetRx.setDrawCircles(false)
        lineDataSetRx.color = resources.getColor(R.color.colorPrimary, null)

        val lineDataSetTx = LineDataSet(dataEntriesTx, "Sent Data")
        lineDataSetTx.lineWidth = 2.5f
        lineDataSetTx.setDrawCircles(false)
        lineDataSetTx.color = resources.getColor(R.color.colorSecondary, null)

        lineChart.data = LineData(lineDataSetRx, lineDataSetTx)
        lineChart.notifyDataSetChanged()
        lineChart.invalidate() // Refresh the chart

        previousRxBytes = currentRxBytes
        previousTxBytes = currentTxBytes
    }

    private fun updateNetworkStats() {
        coroutineScope.launch {
            while (isActive) {
                updateSignalStrength()
                updateConnectionSpeed()
                updateDataUsage()
                updateIPAddress()
                updateFrequency()
                updateSSID()
                updateLineChart()
                delay(5000) // Se actualizeaza la fiecare 5 secunde
            }
        }
    }

    private fun updateSignalStrength() {
        val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val level = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
        val newLevel = level + 1
        textViewSignalStrength.text = "$newLevel/5"
    }

    private fun updateConnectionSpeed() {
        val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val speedMbps = wifiInfo.linkSpeed // measured in Mbps
        textViewConnectionSpeed.text = "$speedMbps Mbps"
    }

    private fun updateDataUsage() {
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        val totalBytes = rxBytes + txBytes
        textViewDataUsage.text = convertBytesToReadableFormat(totalBytes)
    }

    private fun updateIPAddress() {
        val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = Formatter.formatIpAddress(wifiInfo.ipAddress)
        textViewIPAddress.text = ipAddress
    }

    private fun updateFrequency() {
        val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val frequency = wifiInfo.frequency // measured in MHz
        textViewFrequency.text = "$frequency MHz"
    }

    private fun updateSSID() {
        val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid
        textViewSSID.text = ssid.removePrefix("\"").removeSuffix("\"")
    }

    private fun setWifiIdleSetting(value: Int) {
        runCommandWithFeedback("settings put global wifi_idle_ms $value", "Idle setting applied", "Error applying Idle setting")
    }

    private fun setWifiSupplicantScanInterval(value: Int) {
        runCommandWithFeedback("settings put global wifi_supplicant_scan_interval_ms $value", "Supplicant scan interval applied", "Error applying Supplicant scan interval")
    }

    private fun setWifiFrameworkScanInterval(value: Int) {
        runCommandWithFeedback("settings put global wifi_framework_scan_interval_ms $value", "Framework scan interval applied", "Error applying Framework scan interval")
    }

    private fun setWifiScanAlwaysEnabled(enabled: Boolean) {
        val settingValue = if (enabled) "1" else "0"
        runCommandWithFeedback("settings put global wifi_scan_always_enabled $settingValue", "Scan always enabled setting applied", "Error applying Scan always enabled setting")
    }

    private fun runCommandWithFeedback(command: String, successMessage: String, errorMessage: String) {
        coroutineScope.launch(Dispatchers.IO) {
            val result = runCommand(command)
            withContext(Dispatchers.Main) {
                if (result.isEmpty()) {
                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, errorMessage)
                }
            }
        }
    }

    private fun runCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su -c $command")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line)
            }

            reader.close()
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: $command", e)
            "null"
        }
    }

    private fun convertBytesToReadableFormat(bytes: Long): String {
        val kb = bytes / 1024
        val mb = kb / 1024
        val gb = mb / 1024
        return when {
            gb > 0 -> "$gb GB"
            mb > 0 -> "$mb MB"
            kb > 0 -> "$kb KB"
            else -> "$bytes Bytes"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel() // Cancel any ongoing coroutines
    }
}
