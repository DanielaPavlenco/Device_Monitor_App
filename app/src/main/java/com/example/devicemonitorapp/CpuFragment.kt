package com.example.devicemonitorapp

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.devicemonitorapp.databinding.FragmentCpuBinding
import com.example.devicemonitorapp.utils.RootUtils
import com.example.devicemonitorapp.utils.Utils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile

class CpuFragment : Fragment() {

    private var _binding: FragmentCpuBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L // 1 second
    private val cpuUsageEntries = ArrayList<Entry>()
    private var cpuUsageEntryCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCpuBinding.inflate(inflater, container, false)


        // Seteaza optiunile pentru spinner-ul governator
        val governorOptions = resources.getStringArray(R.array.governor_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, governorOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.governorSpinner.adapter = adapter

        binding.infoCpuGovernor.setOnClickListener {
            showInfoPopup(getString(R.string.cpu_governor_title),
                getString(R.string.cpu_governor_content)
            )
        }

        // Verificarea și seteaza drawable-ul de fundal in functie de setarea modului intunecat
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night2) // Inlocuieste cu drawable-ul pentru modul noapte
        } else {
            binding.root.setBackgroundResource(R.drawable.bg2) // Inlocuieste cu drawable-ul pentru modul zi
        }

        // Porneste actualizarea informatiilor CPU
        updateCpuInfo()
        return binding.root
    }

    //Actualizeaza informatiile despre CPU
    private fun updateCpuInfo() {
        handler.postDelayed({
            GlobalScope.launch(Dispatchers.IO) {
                val cpuName = getCpuName()
                val cpuModel = getCpuModel()
                val cpuTemperature = getCpuTemperature()
                val cpuFrequency = getCpuFrequency()
                val coreUsages = getCoreUsages()

                withContext(Dispatchers.Main) {
                    _binding?.let { binding ->
                        binding.cpuName.text = "CPU Name: $cpuName"
                        binding.cpuModel.text = "Model: $cpuModel"
                        binding.temperatureValue.text = "$cpuTemperature°C"
                        binding.frequencyValue.text = "$cpuFrequency GHz"

                        updateCpuUsageChart(binding.cpuUsageChart, coreUsages)
                        updateCoresInfo(coreUsages)
                    }
                }
            }

            // Continua actualizarea doar daca fragmentul este inca in vizualizare
            if (_binding != null) {
                updateCpuInfo()
            }
        }, updateInterval)
    }

    //Actualizeaza informatiile despre utilizarea nucleelor CPU
    private fun updateCoresInfo(coreUsages: List<Int>) {
        _binding?.let { binding ->
            binding.coresContainer.removeAllViews()
            coreUsages.forEachIndexed { index, usage ->
                val coreView = createCoreUsageView(index + 1, usage)
                binding.coresContainer.addView(coreView)
            }
        }
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

    //Creeaza o vizualizare pentru utilizarea unui nucleu CPU
    private fun createCoreUsageView(coreNumber: Int, usage: Int): View {
        val coreView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = params
        }

        val coreLabel = TextView(context).apply {
            text = "Core $coreNumber"
            textSize = 16f
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            layoutParams = params
        }

        val coreProgress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 6000
            progress = usage
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                3f
            )
            layoutParams = params
        }

        coreView.addView(coreLabel)
        coreView.addView(coreProgress)

        return coreView
    }

    //Obtine numele CPU
    private suspend fun getCpuName(): String = withContext(Dispatchers.Default) {
        val rootCpuInfoModel = RootUtils.executeWithOutput("cat /proc/cpuinfo | grep 'model name' | head -n 1 | cut -d: -f2").trim()
        if (rootCpuInfoModel.isNotEmpty()) {
            return@withContext rootCpuInfoModel
        }
        val cpuInfoModel = Utils.runCommand("cat /proc/cpuinfo | grep Hardware | cut -d: -f2", "").trim()
        return@withContext if (cpuInfoModel.isNotEmpty()) cpuInfoModel else Build.HARDWARE
    }

    //Obtine modelul CPU
    private suspend fun getCpuModel(): String = withContext(Dispatchers.Default) {
        val cpuInfoModel = Utils.runCommand("getprop ro.product.model\n", "")
        return@withContext if (cpuInfoModel.isEmpty()) "Unknown" else cpuInfoModel.trim()
    }

    //Obtine temperatura CPU
    private fun getCpuTemperature(): Float {
        return try {
            val temp = RandomAccessFile("/sys/class/thermal/thermal_zone0/temp", "r").use { it.readLine().toFloat() }
            temp / 1000.0f
        } catch (e: Exception) {
            Log.e("CpuFragment", "Error reading CPU temperature", e)
            0f
        }
    }

    //Obtine frecventa CPU
    private fun getCpuFrequency(): String {
        return try {
            val freqStr = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r").use { it.readLine() }
            if (freqStr.contains(",")) {
                freqStr
            } else {
                val freq = freqStr.toFloat()
                val formattedFreq = String.format("%.2f", freq / 1000000.0f)
                formattedFreq
            }
        } catch (e: Exception) {
            Log.e("CpuFragment", "Error reading CPU frequency", e)
            "0"
        }
    }

    //Obtine utilizarea nucleelor CPU
    private fun getCoreUsages(): List<Int> {
        return try {
            val coreUsages = mutableListOf<Int>()
            val coreCount = getNumberOfCores()
            Log.d("CpuFragment", "Number of cores: $coreCount")

            for (i in 0 until coreCount) {
                val currentFreq = getCurrentFreq(i)
                Log.d("CpuFragment", "Frequency of core $i: $currentFreq")
                coreUsages.add(currentFreq)
            }

            coreUsages
        } catch (e: Exception) {
            Log.e("CpuFragment", "Error reading core usages", e)
            List(Runtime.getRuntime().availableProcessors()) { 0 }
        }
    }

    //Obtine numarul de nuclee CPU
    private fun getNumberOfCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    //Obtine frecventa curenta a unui nucleu CPU
    private fun getCurrentFreq(coreNumber: Int): Int {
        val currentFreqPath = "/sys/devices/system/cpu/cpu$coreNumber/cpufreq/scaling_cur_freq"
        return try {
            RandomAccessFile(currentFreqPath, "r").use { it.readLine().toInt() / 1000 }
        } catch (e: Exception) {
            Log.e("CpuFragment", "Error reading frequency for core $coreNumber", e)
            -1
        }
    }

    //Actualizarea graficului utilizarii CPU
    private fun updateCpuUsageChart(cpuUsageChart: LineChart, coreUsages: List<Int>) {
        _binding?.let {
            cpuUsageEntries.add(Entry(cpuUsageEntryCount.toFloat(), coreUsages.average().toFloat()))
            cpuUsageEntryCount++

            val dataSet = LineDataSet(cpuUsageEntries, "CPU Usage")
            val lineData = LineData(dataSet)
            cpuUsageChart.data = lineData
            cpuUsageChart.description.text = "Hz"
            cpuUsageChart.invalidate() // Reimprospatarea graficului
        }
    }

    //Seteaza governator-ul CPU
    private fun setCpuGovernor(governor: String) {
        GlobalScope.launch(Dispatchers.IO) {
            RootUtils.executeAsync("chmod +w /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor && echo '$governor' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "CPU governor set to $governor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Curata legaturile la UI cand fragmentul este distrus
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
