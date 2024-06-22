package com.example.devicemonitorapp

import android.Manifest
import android.app.UiModeManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.devicemonitorapp.databinding.FragmentBatteryBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.roundToInt

class BatteryFragment : Fragment() {

    //Variabilele pentru legarea elementeolor de UI și manager-ul de baterie
    private var _binding: FragmentBatteryBinding? = null
    private val binding get() = _binding!!
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val batteryManager: BatteryManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        } else {
            null
        }
    }

    private lateinit var switchDarkMode: Switch

    companion object {
        private const val REQUEST_CODE_CHANGE_WIFI_STATE = 1
        private const val REQUEST_CODE_BLUETOOTH = 2
    }

    //Receiver pentru monitorizarea schimbarilor in starea bateriei
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            if (action != Intent.ACTION_BATTERY_CHANGED) return

            //Extrage și afiseaza temperatura bateriei
            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0).toFloat() / 10
            binding.cardTemperatureValue.text = "$temp ºC"

            //Extrage și afiseaza nivelul bateriei ca procentaj
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val percentage = ((level / scale.toFloat()) * 100).roundToInt()

            binding.cardStatusValue.text = "$percentage%"
            binding.cardStatusValue.setTextColor(
                when {
                    percentage >= 90 -> ContextCompat.getColor(requireContext(), R.color.colorSuccess)
                    percentage in 16..30 -> ContextCompat.getColor(requireContext(), R.color.colorWarning)
                    percentage <= 15 -> ContextCompat.getColor(requireContext(), R.color.colorError)
                    else -> ContextCompat.getColor(requireContext(), R.color.colorOnSurface)
                }
            )

            //Determina și afiseaza tipul de conexiune al bateriei
            binding.cardStatusSubText.text = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                BatteryManager.BATTERY_PLUGGED_USB -> getString(R.string.battery_connection_usb)
                BatteryManager.BATTERY_PLUGGED_AC -> getString(R.string.battery_connection_ac)
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> getString(R.string.battery_connection_wireless)
                else -> getString(R.string.battery_connection_unplugged)
            }

            //Determina și afiseaza starea de sanatate a bateriei
            binding.cardHealthValue.text = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> getString(R.string.battery_health_healthy)
                BatteryManager.BATTERY_HEALTH_COLD -> getString(R.string.battery_health_cold)
                BatteryManager.BATTERY_HEALTH_DEAD -> getString(R.string.battery_health_dead)
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> getString(R.string.battery_health_overheat)
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> getString(R.string.battery_health_overvoltage)
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> getString(R.string.battery_health_failure)
                else -> getString(android.R.string.unknownName)
            }

            //Afiseaza tehnologia bateriei și tensiunea curenta
            binding.cardHealthSubText.text = intent.extras?.getString(BatteryManager.EXTRA_TECHNOLOGY) ?: ""

            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            binding.cardCurrentSubText.text = "$voltage mV"
        }
    }

    //Runnable pentru actualizarea curentului bateriei la fiecare 2 secunde
    private val getCurrentRunnable = object : Runnable {
        override fun run() {
            lifecycleScope.launch {
                getCurrent()
            }
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Initializarea binding-ului si seteaza actiunile pentr butoanele de informare
        _binding = FragmentBatteryBinding.inflate(inflater, container, false)
        binding.infoImproveBattery.setOnClickListener {
            showInfoPopup(getString(R.string.improve_battery_title),
                getString(R.string.improve_battery_content)
            )
        }
        binding.infoLowRamFlag.setOnClickListener {
            showInfoPopup(getString(R.string.low_ram_title),
                getString(R.string.low_ram_content)
            )
        }
        binding.infoDozeInterval.setOnClickListener {
            showInfoPopup(getString(R.string.doze_interval_title),
                getString(R.string.doze_interval_content)
            )
        }
        binding.infoDozeMode.setOnClickListener {
            showInfoPopup(getString(R.string.doze_mode_title),
                getString(R.string.doze_mode_content)
            )
        }
        binding.infoWifiSwitch.setOnClickListener {
            showInfoPopup(getString(R.string.wifi_switch_title),
                getString(R.string.wifi_switch_content)
            )
        }
        binding.infoBluetoothSwitch.setOnClickListener {
            showInfoPopup(getString(R.string.bluetooth_switch_title),
                getString(R.string.bluetooth_switch_content)
            )
        }
        binding.infoSyncSwitch.setOnClickListener {
            showInfoPopup(getString(R.string.sync_switch_title),
                getString(R.string.sync_switch_content)
            )
        }
        binding.infoDarkMode.setOnClickListener {
            showInfoPopup(getString(R.string.dark_mode_title),
                getString(R.string.dark_mode_content)
            )
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Initializarea switch-ului pentru modul intunecat si setarea fundalului
        switchDarkMode = binding.switchDarkMode // Initialize the dark mode switch from your layout

        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night3) // Replace with your night mode drawable
        } else {
            binding.root.setBackgroundResource(R.drawable.bg1) // Replace with your day mode drawable
        }

        setupDarkModeControl()

        //Seteaza actiunile pentru switch-urile de optimizare a bateriei, modul Doze, si alte setari
        binding.improveBattery.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                optimizeBatterySettings()
            } else {
                resetBatterySettings()
            }
        }

        binding.dozeModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableDozeMode()
            } else {
                disableDozeMode()
            }
        }

        binding.lowRamFlag.setOnCheckedChangeListener { _, isChecked ->
            setLowRamDeviceFlag(isChecked)
        }

        binding.wifiSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CHANGE_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CHANGE_WIFI_STATE), REQUEST_CODE_CHANGE_WIFI_STATE)
            } else {
                setWifiEnabled(isChecked)
            }
        }

        binding.bluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE_BLUETOOTH)
                } else {
                    setBluetoothEnabled(isChecked)
                }
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_ADMIN), REQUEST_CODE_BLUETOOTH)
                } else {
                    setBluetoothEnabled(isChecked)
                }
            }
        }

        binding.syncSwitch.setOnCheckedChangeListener { _, isChecked ->
            setSyncEnabled(!isChecked)
        }

    }

    override fun onStart() {
        super.onStart()
        //Inregistreaza receiver-ul pentru monitorizarea starii bateriei și porneste rennable-ul pentru curentul bateriei
        IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            requireContext().registerReceiver(batteryReceiver, this)
        }
        handler.post(getCurrentRunnable)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        //Incetarea inregistrarii receiver-ului și porneste runnable-ul
        try {
            handler.removeCallbacks(getCurrentRunnable)
            requireContext().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //Metoda sesoendata pentru obtinerea curentului bateriei
    private suspend fun getCurrent() {
        if (!isAdded) return

        val current = withContext(Dispatchers.Default) {
            return@withContext when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.toFloat()?.div(1000) ?: 0F
                }
                else -> 0F
            }
        }

        binding.cardCurrentValue.text = "${current.toDouble().roundToInt()} mA"
    }


    //Afiseaza un popup de informatii
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

    //Optimizeaza setarile bateriei
    private fun optimizeBatterySettings() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Optimize Wi-Fi Scan Interval
            try {
                val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.isWifiEnabled = false
                wifiManager.isWifiEnabled = true
                Log.d("BatteryFragment", "Wi-Fi scan interval optimized")
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to optimize Wi-Fi scan interval", e)
            }

            // Activeaza modul de economisire a energiei
            try {
                // Prima incercare fara root
                try {
                    val process = Runtime.getRuntime().exec("settings put global low_power 1")
                    process.waitFor()
                    if (process.exitValue() == 0) {
                        Log.d("BatteryFragment", "Power saving mode enabled without root")
                    } else {
                        throw IOException("Non-zero exit value without root")
                    }
                } catch (e: IOException) {
                    Log.e("BatteryFragment", "Failed to enable power saving mode without root, trying with root", e)
                    // Incercare cu root
                    val suProcess = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(suProcess.outputStream)
                    os.writeBytes("settings put global low_power 1\n")
                    os.flush()
                    os.close()
                    suProcess.waitFor()
                    if (suProcess.exitValue() == 0) {
                        Log.d("BatteryFragment", "Power saving mode enabled with root")
                    } else {
                        Log.e("BatteryFragment", "Failed to enable power saving mode even with root")
                    }
                }
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to enable power saving mode", e)
            }

            // Dezactivează rapoartele de eroare
            try {
                // Prima incercare fara root
                try {
                    val process = Runtime.getRuntime().exec("settings put global bugreport_in_power_menu 0")
                    process.waitFor()
                    if (process.exitValue() == 0) {
                        Log.d("BatteryFragment", "Error reports disabled without root")
                    } else {
                        throw IOException("Non-zero exit value without root")
                    }
                } catch (e: IOException) {
                    Log.e("BatteryFragment", "Failed to disable error reports without root, trying with root", e)
                    // Incercare cu root
                    val suProcess = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(suProcess.outputStream)
                    os.writeBytes("settings put global bugreport_in_power_menu 0\n")
                    os.flush()
                    os.close()
                    suProcess.waitFor()
                    if (suProcess.exitValue() == 0) {
                        Log.d("BatteryFragment", "Error reports disabled with root")
                    } else {
                        Log.e("BatteryFragment", "Failed to disable error reports even with root")
                    }
                }
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to disable error reports", e)
            }
        }
    }

    //Reseteaza setarile bateriei la valorile implicite
    private fun resetBatterySettings() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Reset Wi-Fi Scan Interval
            try {
                val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.isWifiEnabled = false
                wifiManager.isWifiEnabled = true
                Log.d("BatteryFragment", "Wi-Fi scan interval reset")
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to reset Wi-Fi scan interval", e)
            }

            // Dezactiveaza modul de economisire a energiei
            try {
                // Prima incercare fara root
                try {
                    val process = Runtime.getRuntime().exec("settings put global low_power 0")
                    process.waitFor()
                    if (process.exitValue() == 0) {
                        Log.d("BatteryFragment", "Power saving mode disabled without root")
                    } else {
                        throw IOException("Non-zero exit value without root")
                    }
                } catch (e: IOException) {
                    Log.e("BatteryFragment", "Failed to disable power saving mode without root, trying with root", e)
                    // Incercare cu root
                    val suProcess = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(suProcess.outputStream)
                    os.writeBytes("settings put global low_power 0\n")
                    os.flush()
                    os.close()
                    suProcess.waitFor()
                    if (suProcess.exitValue() == 0) {
                        Log.d("BatteryFragment", "Power saving mode disabled with root")
                    } else {
                        Log.e("BatteryFragment", "Failed to disable power saving mode even with root")
                    }
                }
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to disable power saving mode", e)
            }

            // Activeaza rapoartele de eroare
            try {
                // Prima incercare fara root
                try {
                    val process = Runtime.getRuntime().exec("settings put global bugreport_in_power_menu 1")
                    process.waitFor()
                    if (process.exitValue() == 0) {
                        Log.d("BatteryFragment", "Error reports enabled without root")
                    } else {
                        throw IOException("Non-zero exit value without root")
                    }
                } catch (e: IOException) {
                    Log.e("BatteryFragment", "Failed to enable error reports without root, trying with root", e)
                    // Incercare cu root
                    val suProcess = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(suProcess.outputStream)
                    os.writeBytes("settings put global bugreport_in_power_menu 1\n")
                    os.flush()
                    os.close()
                    suProcess.waitFor()
                    if (suProcess.exitValue() == 0) {
                        Log.d("BatteryFragment", "Error reports enabled with root")
                    } else {
                        Log.e("BatteryFragment", "Failed to enable error reports even with root")
                    }
                }
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to enable error reports", e)
            }
        }
    }

    //Activeaza modul Doze
    private fun enableDozeMode() {
        val dozeInterval = binding.dozeInterval.text.toString().toIntOrNull() ?: 0
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val suProcess = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(suProcess.outputStream)
                os.writeBytes("dumpsys deviceidle force-idle\n")
                os.writeBytes("dumpsys deviceidle deep\n")
                os.writeBytes("settings put global device_idle_constants idle_after_inactive_timeout=${dozeInterval * 1000}\n")
                os.flush()
                os.close()
                suProcess.waitFor()
                Log.d("BatteryFragment", "Doze mode enabled with interval $dozeInterval seconds")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Doze mode enabled", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("BatteryFragment", "Failed to enable Doze mode", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to enable Doze mode", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Dezactiveaza modul Doze
    private fun disableDozeMode() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val suProcess = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(suProcess.outputStream)
                os.writeBytes("dumpsys deviceidle unforce\n")
                os.writeBytes("settings delete global device_idle_constants\n")
                os.flush()
                os.close()
                suProcess.waitFor()
                Log.d("BatteryFragment", "Doze mode disabled")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Doze mode disabled", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("BatteryFragment", "Failed to disable Doze mode", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to disable Doze mode", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Seteaza Flag-ul pentru dispozitiv Low RAM
    private fun setLowRamDeviceFlag(isLowRam: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Prima incercare fara root
                try {
                    val process = if (isLowRam) {
                        Runtime.getRuntime().exec("settings put global low_ram true")
                    } else {
                        Runtime.getRuntime().exec("settings delete global low_ram")
                    }
                    process.waitFor()
                    if (process.exitValue() == 0) {
                        Log.d("BatteryFragment", "Low RAM device flag set without root")
                    } else {
                        throw IOException("Non-zero exit value without root")
                    }
                } catch (e: IOException) {
                    Log.e("BatteryFragment", "Failed to set Low RAM device flag without root, trying with root", e)
                    // Incercare cu root
                    val suProcess = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(suProcess.outputStream)
                    if (isLowRam) {
                        os.writeBytes("settings put global low_ram true\n")
                    } else {
                        os.writeBytes("settings delete global low_ram\n")
                    }
                    os.flush()
                    os.close()
                    suProcess.waitFor()
                    if (suProcess.exitValue() == 0) {
                        Log.d("BatteryFragment", "Low RAM device flag set with root")
                    } else {
                        Log.e("BatteryFragment", "Failed to set Low RAM device flag even with root")
                    }
                }
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to set Low RAM device flag", e)
            }
        }
    }

    //Activarea sau dezactivarea WI-FI
    private fun setWifiEnabled(enabled: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val wifiManager = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.isWifiEnabled = enabled
                Log.d("BatteryFragment", "Wi-Fi ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to ${if (enabled) "enable" else "disable"} Wi-Fi", e)
                // Incercare cu root daca metoda standard esueaza
                try {
                    val suProcess = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(suProcess.outputStream)
                    os.writeBytes("svc wifi ${if (enabled) "enable" else "disable"}\n")
                    os.flush()
                    os.close()
                    suProcess.waitFor()
                    Log.d("BatteryFragment", "Wi-Fi ${if (enabled) "enabled" else "disabled"} using root")
                } catch (rootException: Exception) {
                    Log.e("BatteryFragment", "Failed to ${if (enabled) "enable" else "disable"} Wi-Fi using root", rootException)
                }
            }
        }
    }

    //Activeaza sau dezactiveaza Bluetooth
    private fun setBluetoothEnabled(enabled: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE_BLUETOOTH)
                            return@launch
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_ADMIN), REQUEST_CODE_BLUETOOTH)
                            return@launch
                        }
                    }
                    if (enabled && bluetoothAdapter.isEnabled) {
                        Log.d("BatteryFragment", "Bluetooth already enabled")
                    } else if (!enabled && !bluetoothAdapter.isEnabled) {
                        Log.d("BatteryFragment", "Bluetooth already disabled")
                    } else {
                        try {
                            if (enabled) {
                                bluetoothAdapter.enable()
                            } else {
                                bluetoothAdapter.disable()
                            }
                            Log.d("BatteryFragment", "Bluetooth ${if (enabled) "enabled" else "disabled"}")
                        } catch (e: SecurityException) {
                            Log.e("BatteryFragment", "Permission denied for ${if (enabled) "enabling" else "disabling"} Bluetooth", e)
                        }
                    }
                } else {
                    Log.d("BatteryFragment", "Bluetooth adapter not available")
                }
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to ${if (enabled) "enable" else "disable"} Bluetooth", e)
                // Attempt with root if standard method fails
                try {
                    val suProcess = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(suProcess.outputStream)
                    os.writeBytes("service call bluetooth_manager ${if (enabled) "enable" else "disable"}\n")
                    os.flush()
                    os.close()
                    suProcess.waitFor()
                    Log.d("BatteryFragment", "Bluetooth ${if (enabled) "enabled" else "disabled"} using root")
                } catch (rootException: Exception) {
                    Log.e("BatteryFragment", "Failed to ${if (enabled) "enable" else "disable"} Bluetooth using root", rootException)
                }
            }
        }
    }

    //Activeaza sau dezactiveaza sincronizarea
    private fun setSyncEnabled(enabled: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val accounts = android.accounts.AccountManager.get(requireContext()).accounts
                for (account in accounts) {
                    ContentResolver.setSyncAutomatically(account, true.toString(), enabled)
                }
                Log.d("BatteryFragment", "Sync ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                Log.e("BatteryFragment", "Failed to ${if (enabled) "enable" else "disable"} sync", e)
            }
        }
    }
    //Configurarewa controlului pentru modul intunecat
    private fun setupDarkModeControl() {
        // Verifica setarea actuala pentru modul intunecat
        val isDarkModeEnabled = isDarkModeEnabled()
        switchDarkMode.isChecked = isDarkModeEnabled

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            setDarkMode(isChecked)
        }
    }

    //Activeaza sau dezactiveaza modul intunecat
    private fun setDarkMode(enabled: Boolean) {
        val command = if (enabled) "cmd uimode night yes" else "cmd uimode night no"
        try {
            val suProcess = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(suProcess.outputStream)
            os.writeBytes("$command\n")
            os.flush()
            os.close()
            suProcess.waitFor()
            if (suProcess.exitValue() == 0) {
                Toast.makeText(requireContext(), "Dark mode ${if (enabled) "enabled" else "disabled"} successfully", Toast.LENGTH_SHORT).show()
                Log.d("BatteryFragment", "Dark mode set to ${if (enabled) "enabled" else "disabled"}")
            } else {
                Log.e("BatteryFragment", "Failed to set dark mode: ${suProcess.errorStream.bufferedReader().readText()}")
                Toast.makeText(requireContext(), "Failed to set dark mode.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("BatteryFragment", "Error setting dark mode", e)
            Toast.makeText(requireContext(), "Error setting dark mode.", Toast.LENGTH_SHORT).show()
        }
    }

    //Verifica daca modul intunecat este activat
    private fun isDarkModeEnabled(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("cmd", "uimode", "night"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()
            output.contains("night=yes")
        } catch (e: Exception) {
            Log.e("BatteryFragment", "Error checking dark mode", e)
            false
        }
    }

    //Gestionearea permisiunilor pentru schimbarea starii Wi-Fi și Bluetooth
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_CHANGE_WIFI_STATE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setWifiEnabled(binding.wifiSwitch.isChecked)
                } else {
                    Toast.makeText(requireContext(), "Wi-Fi state change permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_BLUETOOTH -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setBluetoothEnabled(binding.bluetoothSwitch.isChecked)
                } else {
                    Toast.makeText(requireContext(), "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
