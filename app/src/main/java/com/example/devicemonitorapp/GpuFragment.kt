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
import androidx.fragment.app.Fragment
import com.example.devicemonitorapp.databinding.FragmentGpuBinding
import com.example.devicemonitorapp.utils.Utils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.io.RandomAccessFile


class GpuFragment : Fragment() {

    //Declaratoo pentru binding di handler
    private var _binding: FragmentGpuBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L // 1 second
    private val gpuUsageEntries = ArrayList<Entry>()
    private val gpuMemoryEntries = ArrayList<Entry>()
    private var gpuUsageEntryCount = 0
    private var gpuMemoryEntryCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGpuBinding.inflate(inflater, container, false)


        // Verifica si seteaza fundalul in functie de modul intunecat
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night2) // Inlocuieste cu drawable-ul pentru modul noapte
        } else {
            binding.root.setBackgroundResource(R.drawable.bg2) // Inlocuieste cu drawable-ul pentru modul zi
        }

        // Porneste actualizarea informatiilor despre GPU
        updateGpuInfo()
        return binding.root
    }

    //Functia actualizare a informatiilor despre GPU
    private fun updateGpuInfo() {
        handler.postDelayed({
            GlobalScope.launch(Dispatchers.IO) {
                val gpuModel = getGpuName()
                val gpuTemperature = getGpuTemperature()
                val gpuFrequency = getGpuFrequency()
                val gpuUsage = getGpuUsage()
                val gpuMemoryUsage = getGpuMemoryUsage()
                val gpuGovernor = getGpuGovernor()

                withContext(Dispatchers.Main) {
                    // Verifica daca fragmentul este inca atasat la activitate
                    if (isAdded) {
                        binding.gpuModel.text = "GPU Model: $gpuModel"
                        binding.temperatureValue.text = "$gpuTemperature°C"
                        binding.frequencyValue.text = "$gpuFrequency MHz"

                        updateGpuUsageChart(binding.gpuUsageChart, gpuUsage)
                        updateGpuMemoryChart(binding.gpuMemoryChart, gpuMemoryUsage)
                    }
                }
            }

            // Continua actualizarea
            if (isAdded) {
                updateGpuInfo()
            }
        }, updateInterval)
    }

    //Obtine numele GPU-ului
    private suspend fun getGpuName(): String = withContext(Dispatchers.Default) {
        val gpuInfoHardware = Utils.runCommand("cat /sys/class/kgsl/kgsl-3d0/gpu_model", "").trim()
        return@withContext if (gpuInfoHardware.isEmpty()) Build.HARDWARE else gpuInfoHardware
    }

    //Obtine modelul GPU-ului
    private suspend fun getGpuModel(): String = withContext(Dispatchers.Default) {
        // Implement a way to get the GPU model
        return@withContext "Qualcomm"
    }

    //Obtine temperatura GPU-ului
    private fun getGpuTemperature(): Float {
        return try {
            // Implementeaza o metoda pentru a obtine temperatura GPU
            // De exemplu, citeste dintr-un fisier precum /sys/class/thermal/thermal_zone1/temp
            val temp = RandomAccessFile("/sys/class/thermal/thermal_zone1/temp", "r").use { it.readLine().toFloat() }
            temp / 1000.0f
        } catch (e: Exception) {
            Log.e("GpuFragment", "Error reading GPU temperature", e)
            0f
        }
    }

    //Obtine frecventa GPU-ului
    private fun getGpuFrequency(): Float {
        return try {
            // Implementeaza o metoda pentru a obtine frecventa GPU
            // De exemplu, citeste dintr-un fisier precum /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq
            val freq = RandomAccessFile("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq", "r").use { it.readLine().toFloat() }
            freq / 1000.0f
        } catch (e: Exception) {
            Log.e("GpuFragment", "Error reading GPU frequency", e)
            0f
        }
    }

    //Obtine utilizarea GPU-ului
    private fun getGpuUsage(): Float {
        return try {
            // Citeste din /sys/class/kgsl/kgsl-3d0/gpubusy which returns two values: busy time and total time
            RandomAccessFile("/sys/class/kgsl/kgsl-3d0/gpubusy", "r").use {
                val line = it.readLine().trim() // elimina spatiile de la inceput si sfarsit
                Log.d("GpuFragment", "gpubusy content: $line") // Afiseaza linia pentru debugging
                val parts = line.split("\\s+".toRegex())
                Log.d("GpuFragment", "gpubusy parts: ${parts.joinToString()}") // Afiseaza partile pentru debugging
                if (parts.size == 2) {
                    val busyTime = parts[0].toLongOrNull() ?: 0L
                    val totalTime = parts[1].toLongOrNull() ?: 0L
                    Log.d("GpuFragment", "busyTime: $busyTime, totalTime: $totalTime") // Afiseaza valorile parsate
                    if (totalTime > 0) {
                        (busyTime.toFloat() / totalTime) * 100
                    } else {
                        0f
                    }
                } else {
                    0f
                }
            }
        } catch (e: Exception) {
            Log.e("GpuFragment", "Error reading GPU usage", e)
            0f
        }
    }

    //Obtine utilizarea memoriei GPU-ului
    private fun getGpuMemoryUsage(): Float {
        return try {
            // Citeste informatiile despre memorie din  /proc/meminfo
            val file = File("/proc/meminfo")
            val reader = BufferedReader(FileReader(file))
            var gpuMemoryUsage: Float? = null

            reader.forEachLine { line ->
                if (line.contains("KgslShmemUsage:")) {
                    val parts = line.split("\\s+".toRegex())
                    gpuMemoryUsage = parts.getOrNull(1)?.toFloatOrNull()
                }
            }

            reader.close()
            gpuMemoryUsage ?: 0f
        } catch (e: Exception) {
            Log.e("GpuFragment", "Error reading GPU memory usage", e)
            0f
        }
    }

    //Obtine governator-ul GPU-ului
    private fun getGpuGovernor(): String {
        return try {
            // Implementeaza o metoda pentru a obtine governator-ul GPU
            // De exemplu, cisteste dintr-un fisier precum  /sys/class/kgsl/kgsl-3d0/pwrscale
            RandomAccessFile("/sys/class/kgsl/kgsl-3d0/pwrscale", "r").use { it.readLine() }
        } catch (e: Exception) {
            Log.e("GpuFragment", "Error reading GPU governor", e)
            "Unknown"
        }
    }

    //Actualizaeaza graficul utilizarii GPU-ului
    private fun updateGpuUsageChart(gpuUsageChart: LineChart, gpuUsage: Float) {
        // Adauga datele despre utilizarea GPU-ului in grafic daca sunt in limite rezonabile
        if (gpuUsage in 0f..100f) {
            gpuUsageEntries.add(Entry(gpuUsageEntryCount.toFloat(), gpuUsage))
            gpuUsageEntryCount++

            val dataSet = LineDataSet(gpuUsageEntries, "GPU Usage")
            val lineData = LineData(dataSet)
            gpuUsageChart.data = lineData
            gpuUsageChart.description.text = "Percentage of usage"
            gpuUsageChart.invalidate() // Reimprospateaza graficul
        } else {
            Log.e("GpuFragment", "Invalid GPU usage value: $gpuUsage")
        }
    }

    private fun updateGpuMemoryChart(gpuMemoryChart: LineChart, gpuMemoryUsage: Float) {
        gpuMemoryEntries.add(Entry(gpuMemoryEntryCount.toFloat(), gpuMemoryUsage))
        gpuMemoryEntryCount++

        val dataSet = LineDataSet(gpuMemoryEntries, "GPU Memory Usage")
        val lineData = LineData(dataSet)
        gpuMemoryChart.data = lineData
        gpuMemoryChart.description.text = "KB"
        gpuMemoryChart.invalidate() // Reimprospateaza graficul
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

//    private fun fetchGfxInfo() {
//        GlobalScope.launch(Dispatchers.IO) {
//            try {
//                val processBuilder = ProcessBuilder("sh", "-c", "dumpsys -l")
//                processBuilder.redirectErrorStream(true)
//                val process = processBuilder.start()
//
//                val reader = BufferedReader(InputStreamReader(process.inputStream))
//                val output = StringBuilder()
//                var line: String?
//
//                while (reader.readLine().also { line = it } != null) {
//                    output.append(line).append("\n")
//                    Log.d("GpuFragment", "gfxinfo line: $line") // Print each line as it is read
//                }
//                reader.close()
//                process.waitFor()
//
//                withContext(Dispatchers.Main) {
//                    Log.d("GpuFragment", "gfxinfo complete output: $output")
//                }
//            } catch (e: Exception) {
//                Log.e("GpuFragment", "Error fetching gfxinfo", e)
//            }
//        }
//    }
}
