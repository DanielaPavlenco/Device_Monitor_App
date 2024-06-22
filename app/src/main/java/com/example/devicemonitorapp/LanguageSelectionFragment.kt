package com.example.devicemonitorapp

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.devicemonitorapp.databinding.FragmentLanguageSelectionBinding
import java.util.*

class LanguageSelectionFragment : Fragment() {

    private var _binding: FragmentLanguageSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLanguageSelectionBinding.inflate(inflater, container, false)
        // Check and set the background drawable based on the dark mode setting
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night1) // Replace with your night mode drawable
        } else {
            binding.root.setBackgroundResource(R.drawable.pic_one) // Replace with your day mode drawable
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.activateButton.setOnClickListener {
            val selectedId = binding.radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                try {
                    val selectedRadioButton: RadioButton = view.findViewById(selectedId)
                    val selectedLanguage = when (selectedRadioButton.text.toString()) {
                        "English" -> "en"
                        "Romanian" -> "ro"
                        "French" -> "fr"
                        "German" -> "de"
                        else -> "en"
                    }
                    setLocale(requireContext(), selectedLanguage)
                    findNavController().navigate(R.id.action_languageSelectionFragment_to_initialFragment)
                } catch (e: Exception) {
                    Log.e("LanguageSelection", "Error setting locale", e)
                }
            }
        }
    }

    private fun setLocale(context: Context, lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // Repornire activitate pentru a aplica noua limbă
        val intent = requireActivity().intent
        requireActivity().finish()
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
