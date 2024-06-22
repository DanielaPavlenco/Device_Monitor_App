package com.example.devicemonitorapp

import android.app.UiModeManager
import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import soup.neumorphism.NeumorphButton
import com.example.devicemonitorapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    //Variabila pentru binding-ul layout-ului
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //influenteaza layout-ul folosind binding-ul generat
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verifica si seteaza fundalul in functie de modul intunecat
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night1) // Replace with your night mode drawable
        } else {
            binding.root.setBackgroundResource(R.drawable.pic_one) // Replace with your day mode drawable
        }

        // Aplica gradiebt pe textul butoanelor
        applyGradientToText(binding.CpuMonitoring, R.color.orange, R.color.green)
        applyGradientToText(binding.GpuMonitoring, R.color.green, R.color.orange)
        applyGradientToText(binding.batteryManagement, R.color.orange, R.color.green)
        applyGradientToText(binding.performanceBooster, R.color.orange, R.color.green)
        applyGradientToText(binding.memoryManagement, R.color.green, R.color.orange)
        applyGradientToText(binding.storageManagement, R.color.orange, R.color.green)
        applyGradientToText(binding.appManagement, R.color.green, R.color.orange)
        applyGradientToText(binding.networkUsageStats, R.color.orange, R.color.green)
        applyGradientToText(binding.taskAutomation, R.color.green, R.color.orange)
        applyGradientToText(binding.refreshRateMods, R.color.orange, R.color.green)

        // Seteaza textul pentru butoane
        binding.CpuMonitoring.text = getString(R.string.cpu_monitoring)
        binding.GpuMonitoring.text = getString(R.string.gpu_monitoring)
        binding.batteryManagement.text = getString(R.string.battery_management)
        binding.memoryManagement.text = getString(R.string.memory_management)
        binding.storageManagement.text = getString(R.string.storage_management)
        binding.appManagement.text = getString(R.string.app_management)
        binding.networkUsageStats.text = getString(R.string.network_usage_stats)
        binding.taskAutomation.text = getString(R.string.task_automation)
        binding.refreshRateMods.text = getString(R.string.refresh_rate_mods)

        // Seteaza click listeners pentru butoane
        binding.CpuMonitoring.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_CpuFragment)
        }

        binding.GpuMonitoring.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_GpuFragment)
        }
        binding.batteryManagement.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_batteryFragment)
        }
        binding.performanceBooster.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_performanceBoosterFragment)
        }
        binding.memoryManagement.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_memoryFragment)
        }
        binding.storageManagement.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_storageFragment)
        }
        binding.appManagement.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_appManagementFragment)
        }
        binding.networkUsageStats.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_networkFragment)
        }
        binding.taskAutomation.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_terminalFragment)
        }
        binding.refreshRateMods.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_displayFragment)
        }
    }

    //Aplica gradient pe textul butoanelor
    private fun applyGradientToText(button: NeumorphButton, startColorResId: Int, endColorResId: Int) {
        val paint = button.paint
        val width = paint.measureText(button.text.toString())
        val textShader: Shader = LinearGradient(0f, 0f, width, button.textSize,
            intArrayOf(
                ContextCompat.getColor(requireContext(), startColorResId),
                ContextCompat.getColor(requireContext(), endColorResId)
            ), null, Shader.TileMode.CLAMP)
        button.paint.shader = textShader
    }

    //Curata binding-ul cand view-ul este distrus pentru a preveni scurgerile de memorie
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
