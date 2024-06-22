package com.example.devicemonitorapp

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.IOException

class DisplayFragment : Fragment() {

    //Variabilele pentru controalele UI
    private lateinit var sliderBrightness: Slider
    private lateinit var textViewCurrentBrightness: TextView
    private lateinit var spinnerRefreshRate: Spinner
    private lateinit var textViewCurrentRefreshRate: TextView
    private lateinit var textViewCurrentResolution: TextView
    private lateinit var editTextResolution: EditText
    private lateinit var resetButton: MaterialButton
    private lateinit var applyButton: MaterialButton

    private lateinit var textViewCurrentFontSize: TextView
    private lateinit var sliderFontSize: Slider
    private lateinit var resetFontSizeButton: MaterialButton
    private lateinit var applyFontSizeButton: MaterialButton

    private lateinit var switchColorCorrection: SwitchMaterial
    private lateinit var spinnerColorCorrection: Spinner

    private lateinit var switchMagnification: SwitchMaterial
    private lateinit var textViewMagnificationScale: TextView
    private lateinit var sliderMagnification: Slider
    private lateinit var resetMagnificationButton: MaterialButton
    private lateinit var applyMagnificationButton: MaterialButton

    //Valorile implicite pentru rezolutie, dimensiunile de font și marire
    private var defaultResolution: String = "1080x1920"  // Placeholder, will be set dynamically
    private var defaultFontSize: Float = 1.0f  // Placeholder, will be set dynamically
    private var defaultMagnificationScale: Float = 1.0f  // Placeholder, will be set dynamically

    companion object {
        private const val WRITE_SETTINGS_PERMISSION_REQUEST = 100
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_display, container, false)

        //Initializarea elementelor UI
        sliderBrightness = view.findViewById(R.id.sliderBrightness)
        textViewCurrentBrightness = view.findViewById(R.id.textViewCurrentBrightness)
        spinnerRefreshRate = view.findViewById(R.id.spinnerRefreshRate)
        textViewCurrentRefreshRate = view.findViewById(R.id.textViewCurrentRefreshRate)
        textViewCurrentResolution = view.findViewById(R.id.textViewCurrentResolution)
        editTextResolution = view.findViewById(R.id.editTextResolution)
        resetButton = view.findViewById(R.id.resetButton)
        applyButton = view.findViewById(R.id.applyButton)

        textViewCurrentFontSize = view.findViewById(R.id.textViewCurrentFontSize)
        sliderFontSize = view.findViewById(R.id.sliderFontSize)
        resetFontSizeButton = view.findViewById(R.id.resetFontSize)
        applyFontSizeButton = view.findViewById(R.id.applyFontSize)

        switchColorCorrection = view.findViewById(R.id.switchColorCorrection)
        spinnerColorCorrection = view.findViewById(R.id.spinnerColorCorrection)

        switchMagnification = view.findViewById(R.id.switchMagnification)
        textViewMagnificationScale = view.findViewById(R.id.textViewMagnificationScale)
        sliderMagnification = view.findViewById(R.id.sliderMagnification)
        resetMagnificationButton = view.findViewById(R.id.resetMagnificationButton)
        applyMagnificationButton = view.findViewById(R.id.applyMagnificationButton)


        //Verifica permisiunile necesare
        checkWriteSettingsPermission()
        fetchDisplayModes()

        //Seteaza conroalele UI
        setupBrightnessControl()
        setupRefreshRateControl()
        setupResolutionControl()
        setupFontSizeControl()
        setupColorCorrectionControl()
        setupMagnificationControl()

        //Seteaza butoanele de informatii
        view.findViewById<ImageButton>(R.id.infoColorCorrection).setOnClickListener {
            showInfoPopup(getString(R.string.color_correction_title),
                getString(R.string.color_correction_content)
            )
        }

        view.findViewById<ImageButton>(R.id.infoMagnification).setOnClickListener {
            showInfoPopup(getString(R.string.magnification_title),
                getString(R.string.magnification_content)
            )
        }

        // Verifica si seteaza fundalul in functie de modul intunecat
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        val root = view.findViewById<ScrollView>(R.id.scrollViewRoot)

        if (isNightMode) {
            root.setBackgroundResource(R.drawable.night2) // Inlocuieste cu drawable-ul pentru modul dark
        } else {
            root.setBackgroundResource(R.drawable.bg2) // Inlocuieste cu drawable-ul pentru modul zi
        }

        return view
    }

    //Verifica permisiunea de a modifica setarile sistemului
    private fun checkWriteSettingsPermission() {
        if (!Settings.System.canWrite(requireContext())) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + requireActivity().packageName)
            startActivityForResult(intent, WRITE_SETTINGS_PERMISSION_REQUEST)
        }
    }

    //Gestioneaza rezultatul cererii de permisiune
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WRITE_SETTINGS_PERMISSION_REQUEST) {
            if (Settings.System.canWrite(requireContext())) {
                // Permisiune acordata, pote continua cu setarea luminozitatii
            } else {
                // Permisiune refuzata, afiseaza un mesaj utilizatorului
                Toast.makeText(requireContext(), "Permission not granted. Cannot change settings.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Seteaza controlul luminozitatii
    private fun setupBrightnessControl() {
        val brightness = getScreenBrightness()
        sliderBrightness.value = brightness.toFloat()
        textViewCurrentBrightness.text = "Current Brightness: $brightness"

        sliderBrightness.addOnChangeListener { _, value, _ ->
            if (Settings.System.canWrite(requireContext())) {
                setScreenBrightness(value.toInt())
                textViewCurrentBrightness.text = "Current Brightness: ${value.toInt()}"
            } else {
                checkWriteSettingsPermission()
            }
        }
    }

    //Seteaza controlul ratei de refresh
    private fun setupRefreshRateControl() {
        val displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        val supportedModes = display.supportedModes.map { it.refreshRate.toInt() }.distinct().sorted()

        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, supportedModes).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRefreshRate.adapter = adapter
        }

        spinnerRefreshRate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedRate = parent.getItemAtPosition(position) as Int
                textViewCurrentRefreshRate.text = "Current Refresh Rate: ${selectedRate} Hz"
                setRefreshRate(selectedRate)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Nu face nimic
            }
        }

        // Seteaza cea mai mare rata de refresh disponibila candse incarca pagina
        if (supportedModes.isNotEmpty()) {
            val highestRefreshRate = supportedModes.last()
            spinnerRefreshRate.setSelection(supportedModes.indexOf(highestRefreshRate))
            setRefreshRate(highestRefreshRate)
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

    //Seteaza controlul rezolutiei
    private fun setupResolutionControl() {
        fetchAvailableResolutions { currentResolution ->
            defaultResolution = currentResolution  // Set the fetched resolution as the default
            textViewCurrentResolution.text = "Current Resolution: $currentResolution"
            editTextResolution.setText(currentResolution)

            applyButton.setOnClickListener {
                val newResolution = editTextResolution.text.toString()
                setResolution(newResolution)
            }

            resetButton.setOnClickListener {
                editTextResolution.setText(defaultResolution)
                setResolution(defaultResolution)
            }

            editTextResolution.setOnEditorActionListener { v, _, _ ->
                val newResolution = v.text.toString()
                setResolution(newResolution)
                true
            }
        }
    }

    //Seteaza rezolutia ecranului
    private fun setResolution(resolution: String) {
        val (width, height) = resolution.split("x").map { it.trim() }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Incearca sa seteze rezolutia fara root
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "wm size ${width}x${height}"))
                    process.waitFor()
                    if (process.exitValue() == 0) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Resolution set to $resolution without root", Toast.LENGTH_SHORT).show()
                            Log.d("DisplayFragment", "Resolution set to $resolution without root")
                        }
                    } else {
                        throw IOException("Non-zero exit value without root")
                    }
                } catch (e: IOException) {
                    Log.e("DisplayFragment", "Failed to set resolution without root, trying with root", e)
                    // Attempt with root
                    val suProcess = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(suProcess.outputStream)
                    os.writeBytes("wm size ${width}x${height}\n")
                    os.flush()
                    os.close()
                    suProcess.waitFor()
                    if (suProcess.exitValue() == 0) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Resolution set to $resolution with root", Toast.LENGTH_SHORT).show()
                            Log.d("DisplayFragment", "Resolution set to $resolution with root")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to set resolution even with root", Toast.LENGTH_SHORT).show()
                            Log.e("DisplayFragment", "Failed to set resolution even with root")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DisplayFragment", "Error setting resolution", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error setting resolution.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Obtine rezolutiile disponibile
    private fun fetchAvailableResolutions(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                val currentMode = display.mode
                val currentResolution = "${currentMode.physicalWidth}x${currentMode.physicalHeight}"

                withContext(Dispatchers.Main) {
                    callback(currentResolution)
                }
            } catch (e: Exception) {
                Log.e("DisplayFragment", "Error fetching resolutions", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error fetching resolutions.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Seteaza controlul dimensiunii fontului
    private fun setupFontSizeControl() {
        textViewCurrentFontSize.text = "Current Font Size: $defaultFontSize"
        sliderFontSize.value = defaultFontSize

        sliderFontSize.addOnChangeListener { _, value, _ ->
            textViewCurrentFontSize.text = "Current Font Size: $value"
        }

        applyFontSizeButton.setOnClickListener {
            val newFontSize = sliderFontSize.value
            setFontSize(newFontSize)
        }

        resetFontSizeButton.setOnClickListener {
            sliderFontSize.value = defaultFontSize
            setFontSize(defaultFontSize)
        }
    }

    //Seteaza controlul corecției de culoare
    private fun setupColorCorrectionControl() {
        val colorModes = listOf("Monochromatic", "Protanomaly", "Deuteranomaly", "Tritanomaly")
        val colorModeCodes = listOf(0, 11, 12, 13)

        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, colorModes).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerColorCorrection.adapter = adapter
        }

        spinnerColorCorrection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCode = colorModeCodes[position]
                setColorCorrectionMode(selectedCode)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Nu face nimic
            }
        }

        switchColorCorrection.setOnCheckedChangeListener { _, isChecked ->
            setDisplayDaltonizerEnabled(isChecked)
        }
    }

    //Seteaza controlul maririi ecranului
    private fun setupMagnificationControl() {
        switchMagnification.setOnCheckedChangeListener { _, isChecked ->
            setMagnificationEnabled(isChecked)
        }

        sliderMagnification.addOnChangeListener { _, value, _ ->
            textViewMagnificationScale.text = "Magnification Scale: $value"
        }

        applyMagnificationButton.setOnClickListener {
            val scale = sliderMagnification.value
            setMagnificationScale(scale)
        }

        resetMagnificationButton.setOnClickListener {
            sliderMagnification.value = defaultMagnificationScale
            setMagnificationScale(defaultMagnificationScale)
        }
    }

    //Activarea sau dezactivarea maririi ecranului
    private fun setMagnificationEnabled(enabled: Boolean) {
        try {
            val resolver: ContentResolver = requireContext().contentResolver
            Settings.Secure.putInt(resolver, "accessibility_display_magnification_enabled", if (enabled) 1 else 0)
            Toast.makeText(requireContext(), "Magnification ${if (enabled) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("displayFragment", "Error setting magnification enabled", e)
            Toast.makeText(requireContext(), "Error setting magnification enabled", Toast.LENGTH_SHORT).show()
        }
    }

    //Seteaza scala de marire a ecranului
    private fun setMagnificationScale(scale: Float) {
        try {
            val resolver: ContentResolver = requireContext().contentResolver
            Settings.Secure.putFloat(resolver, "accessibility_display_magnification_scale", scale)
            Toast.makeText(requireContext(), "Magnification scale set to $scale", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("displayFragment", "Error setting magnification scale", e)
            Toast.makeText(requireContext(), "Error setting magnification scale", Toast.LENGTH_SHORT).show()
        }
    }

    //Obtine luminozitatea ecranului
    private fun getScreenBrightness(): Int {
        return try {
            Settings.System.getInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            Log.e("displayFragment", "Screen brightness setting not found", e)
            255 // Default la luminozitatea maxima
        }
    }

    //Seteaza luminozitatea ecranului
    private fun setScreenBrightness(brightness: Int) {
        // Seteaza modul de luminozitate la manual
        Settings.System.putInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        // Seteaza nievlul de luminozitate
        Settings.System.putInt(requireContext().contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
    }

    //Seteaza rata de refresh a ecranului
    private fun setRefreshRate(refreshRate: Int) {
        val displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        val supportedModes = display.supportedModes
        val targetMode = supportedModes.find { it.refreshRate.toInt() == refreshRate }

        if (targetMode != null) {
            val window = activity?.window
            if (window != null) {
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = targetMode.modeId
                window.attributes = layoutParams
                Log.d("displayFragment", "Setting refresh rate to ${refreshRate}Hz")
            }
        } else {
            Toast.makeText(requireContext(), "Selected refresh rate not supported on this device.", Toast.LENGTH_SHORT).show()
            Log.d("displayFragment", "Selected refresh rate not supported on this device.")
        }
    }

    //Seteaza dimensiunea fontului
    private fun setFontSize(fontSize: Float) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Foloseste Settings.System pentru a seta scala fontului
                Settings.System.putFloat(requireContext().contentResolver, Settings.System.FONT_SCALE, fontSize)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Font size set to $fontSize", Toast.LENGTH_SHORT).show()
                    Log.d("displayFragment", "Font size set to $fontSize")
                }
            } catch (e: Exception) {
                Log.e("displayFragment", "Error setting font size", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error setting font size.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Seteaza modul de corectie a culorilor
    private fun setColorCorrectionMode(mode: Int) {
        try {
            val resolver: ContentResolver = requireContext().contentResolver
            Settings.Secure.putInt(resolver, "accessibility_display_daltonizer", mode)
            Log.d("displayFragment", "Color correction mode set to $mode")
        } catch (e: Exception) {
            Log.e("displayFragment", "Error setting color correction mode", e)
        }
    }

    //Activeaza sau dezactiveaza daltonizatorul
    private fun setDisplayDaltonizerEnabled(enabled: Boolean) {
        try {
            val resolver: ContentResolver = requireContext().contentResolver
            val value = if (enabled) 1 else 0
            Settings.Secure.putInt(resolver, "accessibility_display_daltonizer_enabled", value)
            Log.d("displayFragment", "Display daltonizer enabled: $enabled")
        } catch (e: Exception) {
            Log.e("displayFragment", "Error enabling display daltonizer", e)
        }
    }

    //Obtine modurile de afisare disponibile
    private fun fetchDisplayModes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val displayManager = requireContext().getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                val supportedModes = display.supportedModes

                val displayModes = supportedModes.map { mode ->
                    "Mode ID: ${mode.modeId}, Resolution: ${mode.physicalWidth}x${mode.physicalHeight}, Refresh Rate: ${mode.refreshRate}Hz"
                }

                withContext(Dispatchers.Main) {
                    displayModes.forEach { mode ->
                        Log.d("displayFragment", "Display Mode: $mode")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
