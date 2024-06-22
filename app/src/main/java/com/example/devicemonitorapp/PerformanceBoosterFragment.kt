package com.example.devicemonitorapp

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.devicemonitorapp.databinding.FragmentPerformanceBoosterBinding
import com.google.android.material.snackbar.Snackbar
import java.io.DataOutputStream

class PerformanceBoosterFragment : Fragment() {

    private var _binding: FragmentPerformanceBoosterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPerformanceBoosterBinding.inflate(inflater, container, false)

        // Se verifica daca aplicatii de fundal sunt setatate pe fundal dark mode
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night2) // Replace with your night mode drawable
        } else {
            binding.root.setBackgroundResource(R.drawable.bg2) // Replace with your day mode drawable
        }

        setControls(isDeviceRooted())

        binding.infoLiquidSmoothUI.setOnClickListener {
            showInfoPopup(getString(R.string.dark_mode_title),
                getString(R.string.dark_mode_content)
            )
        }
        binding.infoBetterScrolling.setOnClickListener {
            showInfoPopup(getString(R.string.better_scrolling_title),
                getString(R.string.better_scrolling_content)
            )
        }
        binding.infoFPSUnlocker.setOnClickListener {
            showInfoPopup(getString(R.string.fps_unlocker_title),
                getString(R.string.fps_unlocker_content)
            )
        }
        binding.infoPerformanceTweak.setOnClickListener {
            showInfoPopup(getString(R.string.performance_tweak_title),
                getString(R.string.performance_tweak_content)
            )
        }
        binding.infoRenderingQuality.setOnClickListener {
            showInfoPopup(getString(R.string.rendering_quality_title),
                getString(R.string.rendering_quality_content)
            )
        }
        binding.infoGPUAcceleration.setOnClickListener {
            showInfoPopup(getString(R.string.gpu_acceleration_title),
                getString(R.string.gpu_acceleration_content)
            )
        }
        binding.infoReduceInCallDelay.setOnClickListener {
            showInfoPopup(getString(R.string.reduce_call_title),
                getString(R.string.reduce_call_content)
            )
        }

        return binding.root
    }


    private fun runCommands(vararg commands: String) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (command in commands) {
                os.writeBytes("$command\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(binding.root, "Failed to execute commands", Snackbar.LENGTH_SHORT).show()
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

    private fun setControls(isRooted: Boolean) {
        binding.switchGPUAcceleration.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            isChecked = getPrefBoolean("PERFORMANCE_GPU", false)
            setOnCheckedChangeListener { _, isChecked ->
                setPrefBoolean("PERFORMANCE_GPU", isChecked)
                if (isChecked) {
                    runCommands(
                        "setprop hwui.render_dirty_regions false",
                        "setprop persist.sys.ui.hw 1",
                        "setprop debug.egl.hw 1",
                        "setprop debug.composition.type gpu"
                    )
                } else {
                    runCommands(
                        "mount -o remount,rw /data",
                        "rm -f /data/property/persist.sys.ui.hw",
                        "mount -o remount,ro /data"
                    )
                }
            }
        }

        binding.switchRenderingQuality.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            isChecked = getPrefBoolean("PERFORMANCE_RENDERING", false)
            setOnCheckedChangeListener { _, isChecked ->
                setPrefBoolean("PERFORMANCE_RENDERING", isChecked)
                if (isChecked) {
                    runInternalScript("ren_on")
                }
            }
        }

        binding.switchReduceInCallDelay.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            isChecked = getPrefBoolean("PERFORMANCE_CALL_RING", false)
            setOnCheckedChangeListener { _, isChecked ->
                setPrefBoolean("PERFORMANCE_CALL_RING", isChecked)
                if (isChecked) {
                    runCommand("setprop ro.telephony.call_ring.delay 0")
                } else {
                    runCommand("setprop ro.telephony.call_ring.delay 1")
                }
            }
        }

        binding.switchLiquidSmoothUI.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            isChecked = getPrefBoolean("PERFORMANCE_LS_UI", false)
            setOnCheckedChangeListener { _, isChecked ->
                setPrefBoolean("PERFORMANCE_LS_UI", isChecked)
                if (isChecked) {
                    runCommands(
                        "settings put global transition_animation_scale 0.7",
                        "settings put global animator_duration_scale 0.7",
                        "settings put global window_animation_scale 0.7",
                        "setprop persist.service.lgospd.enable 0",
                        "setprop persist.service.pcsync.enable 0",
                        "setprop touch.pressure.scale 0.001"
                    )
                } else {
                    runCommands(
                        "settings put global transition_animation_scale 1.0",
                        "settings put global animator_duration_scale 1.0",
                        "settings put global window_animation_scale 1.0",
                        "mount -o remount,rw /data",
                        "rm -f /data/property/persist.service.pcsync.enable",
                        "rm -f /data/property/persist.service.lgospd.enable",
                        "mount -o remount,ro /data"
                    )
                }
            }
        }

        binding.switchBetterScrolling.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            isChecked = getPrefBoolean("PERFORMANCE_SCROLLING", false)
            setOnCheckedChangeListener { _, isChecked ->
                setPrefBoolean("PERFORMANCE_SCROLLING", isChecked)
                if (isChecked) {
                    runInternalScript("ro_on")
                }
            }
        }

        binding.switchFPSUnlocker.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            isChecked = getPrefBoolean("PERFORMANCE_FPS_UNLOCKER", false)
            setOnCheckedChangeListener { _, isChecked ->
                setPrefBoolean("PERFORMANCE_FPS_UNLOCKER", isChecked)
                if (isChecked) {
                    runCommand("setprop debug.egl.swapinterval -60")
                } else {
                    runCommand("setprop debug.egl.swapinterval 1")
                }
            }
        }

        binding.switchPerformanceTweak.apply {
            isEnabled = isRooted
            setOnCheckedChangeListener(null)
            isChecked = getPrefBoolean("PERFORMANCE_PERF_TWEAK", false)
            setOnCheckedChangeListener { _, isChecked ->
                setPrefBoolean("PERFORMANCE_PERF_TWEAK", isChecked)
                if (isChecked) {
                    runInternalScript("pf_on")
                    runCommands(
                        "setprop persist.sys.use_dithering 0",
                        "setprop persist.sys.use_16bpp_alpha 1"
                    )
                } else {
                    runCommands(
                        "mount -o remount,rw /data",
                        "rm -f /data/property/persist.sys.use_dithering",
                        "rm -f /data/property/setprop persist.sys.use_16bpp_alpha",
                        "mount -o remount,ro /data"
                    )
                }
            }
        }
    }

    private fun runInternalScript(scriptName: String) {
        // Implement your internal script execution logic here
    }

    private fun runCommand(command: String) {
        runCommands(command)
    }

    private fun getPrefBoolean(key: String, defaultValue: Boolean): Boolean {
        // Implement your shared preferences get boolean logic here
        return defaultValue
    }

    private fun setPrefBoolean(key: String, value: Boolean) {
        // Implement your shared preferences set boolean logic here
    }

    private fun isDeviceRooted(): Boolean {
        // Implement your device root check logic here
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
