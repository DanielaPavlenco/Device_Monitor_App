package com.example.devicemonitorapp

import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.devicemonitorapp.databinding.FragmentMemoryBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileReader
import java.io.IOException

class MemoryFragment : Fragment() {
    private var _binding: FragmentMemoryBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L // 1 secunda
    private val memoryUsageEntries = ArrayList<Entry>()
    private var memoryUsageEntryCount = 0
    private var maxMemory = 0L
    private val updateJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + updateJob)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMemoryBinding.inflate(inflater, container, false)

        // Check and set the background drawable based on the dark mode setting
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night3) // Replace with your night mode drawable
        } else {
            binding.root.setBackgroundResource(R.drawable.bg1) // Replace with your day mode drawable
        }

        // Incepe sa se actualizeze memoria
        updateMemoryInfo()

        // Set click listeners for the clear background apps buttons
        binding.forceClearBackgroundAppsButton.setOnClickListener {
            forceClearBackgroundApps()
        }
        binding.politeClearBackgroundAppsButton.setOnClickListener {
            politeClearBackgroundApps()
            requestMemoryTrim()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyGradientToText(binding.forceClearBackgroundAppsButton, R.color.orange, R.color.green)
        applyGradientToText(binding.politeClearBackgroundAppsButton, R.color.green, R.color.orange)
    }

    private fun applyGradientToText(button: Button, startColorResId: Int, endColorResId: Int) {
        val paint = button.paint
        val width = paint.measureText(button.text.toString())
        val textShader: Shader = LinearGradient(0f, 0f, width, button.textSize,
            intArrayOf(
                ContextCompat.getColor(requireContext(), startColorResId),
                ContextCompat.getColor(requireContext(), endColorResId)
            ), null, Shader.TileMode.CLAMP)
        button.paint.shader = textShader
    }

    private fun updateMemoryInfo() {
        handler.postDelayed({
            coroutineScope.launch(Dispatchers.IO) {
                val currentMemoryUsage = getCurrentMemoryUsage()
                val availableMemory = getAvailableMemory()
                val freeMemory = getFreeMemory()
                val cachedMemory = getCachedMemory()
                val swapMemory = getSwapMemory()
                val memoryBuffers = getMemoryBuffers()
                val activeInactiveMemory = getActiveInactiveMemory()

                withContext(Dispatchers.Main) {
                    // Ensure the fragment is still added to the activity
                    if (isAdded && _binding != null) {
                        binding.currentMemoryValue.text = "$currentMemoryUsage MB"
                        binding.currentMemoryMaxValue.text = "$maxMemory MB"
                        binding.availableMemoryValue.text = "$availableMemory MB"
                        binding.availableMemoryMaxValue.text = "$maxMemory MB"
                        binding.cachedMemoryValue.text = "$cachedMemory MB"
                        binding.swapMemoryValue.text = "Total: ${swapMemory.first} MB, Free: ${swapMemory.second} MB, Used: ${swapMemory.third} MB"
                        binding.memoryBuffersValue.text = "$memoryBuffers MB"
                        binding.activeInactiveMemoryValue.text = "Active: ${activeInactiveMemory.first} MB, Inactive: ${activeInactiveMemory.second} MB"

                        binding.currentMemoryProgressBar.max = maxMemory.toInt()
                        binding.currentMemoryProgressBar.progress = currentMemoryUsage.toInt()
                        binding.availableMemoryProgressBar.max = maxMemory.toInt()
                        binding.availableMemoryProgressBar.progress = availableMemory.toInt()

                        updateMemoryUsageChart(binding.memoryUsageChart, currentMemoryUsage.toFloat())
                    }
                }
            }

            // Continue updating
            if (isAdded) {
                updateMemoryInfo()
            }
        }, updateInterval)
    }

    private suspend fun getCurrentMemoryUsage(): Long {
        return withContext(Dispatchers.IO) {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            val totalMemory = memoryInfo.totalMem / (1024 * 1024)
            maxMemory = totalMemory
            val freeMemory = memoryInfo.availMem / (1024 * 1024)
            totalMemory - freeMemory
        }
    }

    private suspend fun getAvailableMemory(): Long {
        return withContext(Dispatchers.IO) {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.availMem / (1024 * 1024)
        }
    }

    private suspend fun getFreeMemory(): Long {
        return withContext(Dispatchers.IO) {
            val file = File("/proc/meminfo")
            val reader = BufferedReader(FileReader(file))
            var freeMemory: Long? = null

            reader.forEachLine { line ->
                if (line.startsWith("MemFree:")) {
                    val parts = line.split("\\s+".toRegex())
                    freeMemory = parts[1].toLongOrNull()?.div(1024)
                }
            }

            reader.close()
            freeMemory ?: 0L
        }
    }

    private suspend fun getCachedMemory(): Long {
        return withContext(Dispatchers.IO) {
            val file = File("/proc/meminfo")
            val reader = BufferedReader(FileReader(file))
            var cachedMemory: Long? = null

            reader.forEachLine { line ->
                if (line.startsWith("Cached:")) {
                    val parts = line.split("\\s+".toRegex())
                    cachedMemory = parts[1].toLongOrNull()?.div(1024)
                }
            }

            reader.close()
            cachedMemory ?: 0L
        }
    }

    private suspend fun getSwapMemory(): Triple<Long, Long, Long> {
        return withContext(Dispatchers.IO) {
            val file = File("/proc/meminfo")
            val reader = BufferedReader(FileReader(file))
            var totalSwap: Long? = null
            var freeSwap: Long? = null

            reader.forEachLine { line ->
                when {
                    line.startsWith("SwapTotal:") -> {
                        val parts = line.split("\\s+".toRegex())
                        totalSwap = parts[1].toLongOrNull()?.div(1024)
                    }
                    line.startsWith("SwapFree:") -> {
                        val parts = line.split("\\s+".toRegex())
                        freeSwap = parts[1].toLongOrNull()?.div(1024)
                    }
                }
            }

            reader.close()
            val usedSwap = (totalSwap ?: 0L) - (freeSwap ?: 0L)
            Triple(totalSwap ?: 0L, freeSwap ?: 0L, usedSwap)
        }
    }

    private suspend fun getMemoryBuffers(): Long {
        return withContext(Dispatchers.IO) {
            val file = File("/proc/meminfo")
            val reader = BufferedReader(FileReader(file))
            var memoryBuffers: Long? = null

            reader.forEachLine { line ->
                if (line.startsWith("Buffers:")) {
                    val parts = line.split("\\s+".toRegex())
                    memoryBuffers = parts[1].toLongOrNull()?.div(1024)
                }
            }

            reader.close()
            memoryBuffers ?: 0L
        }
    }

    private suspend fun getActiveInactiveMemory(): Pair<Long, Long> {
        return withContext(Dispatchers.IO) {
            val file = File("/proc/meminfo")
            val reader = BufferedReader(FileReader(file))
            var activeMemory: Long? = null
            var inactiveMemory: Long? = null

            reader.forEachLine { line ->
                when {
                    line.startsWith("Active:") -> {
                        val parts = line.split("\\s+".toRegex())
                        activeMemory = parts[1].toLongOrNull()?.div(1024)
                    }
                    line.startsWith("Inactive:") -> {
                        val parts = line.split("\\s+".toRegex())
                        inactiveMemory = parts[1].toLongOrNull()?.div(1024)
                    }
                }
            }

            reader.close()
            Pair(activeMemory ?: 0L, inactiveMemory ?: 0L)
        }
    }

    private fun updateMemoryUsageChart(memoryUsageChart: LineChart, memoryUsage: Float) {
        // Add memory usage data to the chart if it is within a reasonable range
        if (memoryUsage >= 0) {
            memoryUsageEntries.add(Entry(memoryUsageEntryCount.toFloat(), memoryUsage))
            memoryUsageEntryCount++

            val dataSet = LineDataSet(memoryUsageEntries, "Memory Usage")
            val lineData = LineData(dataSet)
            memoryUsageChart.data = lineData
            memoryUsageChart.description.text = "MB"
            memoryUsageChart.invalidate() // Refresh chart
        } else {
            Log.e("MemoryFragment", "Invalid memory usage value: $memoryUsage")
        }
    }

    // can't easily close other apps since android 14!!

    private fun forceClearBackgroundApps() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val myPackageName = requireContext().packageName
                Log.d("MemoryFragment", "Excluding package: $myPackageName")

                // Get root access
                val suProcess = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(suProcess.outputStream)

                // Command to find and force-stop non-essential processes, excluding the current app
                val command = "ps | grep -v root | grep -v system | grep -v \"android.process.\" | grep -v radio | grep -v \"com.google.process.\" | grep -v \"com.lge.\" | grep -v shell | grep -v NAME | awk '{if (\$NF != \"$myPackageName\") print \$NF}' | xargs -n1 am force-stop\n"
                os.writeBytes(command)
                os.flush()
                Log.d("MemoryFragment", "Executed command: $command")

                os.writeBytes("exit\n")
                os.flush()
                os.close()
                suProcess.waitFor()

                // Log the exit value to check if the command was successful
                val exitValue = suProcess.exitValue()
                Log.d("MemoryFragment", "Root command exit value: $exitValue")

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Background apps cleared", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("MemoryFragment", "Failed to execute IO command", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            } catch (e: SecurityException) {
                Log.e("MemoryFragment", "Security exception", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Can't get root access", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("MemoryFragment", "General exception", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Can't get root access", Toast.LENGTH_LONG).show()
                }
            }
        }
    }




    private fun politeClearBackgroundApps() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                // Log memory usage before clearing
                val memoryInfoBefore = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfoBefore)
                Log.d("MemoryFragment", "Memory before clearing: ${memoryInfoBefore.availMem / (1024 * 1024)} MB")

                // Attempt to trim memory
                val runningAppProcesses = activityManager.runningAppProcesses
                for (appProcess in runningAppProcesses) {
                    if (appProcess.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                        val packageName = appProcess.processName
                        try {
                            activityManager.killBackgroundProcesses(packageName)
                            Log.d("MemoryFragment", "Requested to kill process: $packageName")
                        } catch (e: SecurityException) {
                            Log.e("MemoryFragment", "Failed to kill background process for package: $packageName", e)
                        }
                    }
                }

                // Log memory usage after clearing
                val memoryInfoAfter = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfoAfter)
                Log.d("MemoryFragment", "Memory after clearing: ${memoryInfoAfter.availMem / (1024 * 1024)} MB")

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Background apps requested to clear", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MemoryFragment", "Failed to request clearing background apps", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to request clearing background apps", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestMemoryTrim() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningAppProcesses = activityManager.runningAppProcesses

                for (appProcess in runningAppProcesses) {
                    if (appProcess.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                        try {
                            val packageName = appProcess.processName
                            activityManager.killBackgroundProcesses(packageName)
                        } catch (e: SecurityException) {
                            Log.e("MemoryFragment", "Failed to kill background process for package: ${appProcess.processName}", e)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Background apps requested to clear", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MemoryFragment", "Failed to request clearing background apps", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to request clearing background apps", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        updateJob.cancel()
    }
}
