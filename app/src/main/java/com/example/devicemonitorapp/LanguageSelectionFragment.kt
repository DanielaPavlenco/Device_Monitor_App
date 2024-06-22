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

    //Variabila pentru binding-ul layout-ului
    private var _binding: FragmentLanguageSelectionBinding? = null
    private val binding get() = _binding!!

    //Metoda pentru creare si inflatare a view-ului fragmentului
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLanguageSelectionBinding.inflate(inflater, container, false)
        // Verifica si seteaza fundalul in functie de modul intunecat
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night1) // Inlocuieste cu drawable-ul pentru modul dark
        } else {
            binding.root.setBackgroundResource(R.drawable.pic_one) // Inlocuieste cu drawable-ul pentru modul zi
        }
        return binding.root
    }

    //Metoda pentru configurarea view-ului dupa ce a fost creat
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Seteaza un click listener pentru butonul de activare
        binding.activateButton.setOnClickListener {
            val selectedId = binding.radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                try {
                    //GAseste butonul radio selectat si obtine limba selectata
                    val selectedRadioButton: RadioButton = view.findViewById(selectedId)
                    val selectedLanguage = when (selectedRadioButton.text.toString()) {
                        "English" -> "en"
                        "Romanian" -> "ro"
                        "French" -> "fr"
                        "German" -> "de"
                        else -> "en"
                    }
                    //Seteaza limba aplicatiei
                    setLocale(requireContext(), selectedLanguage)
                    //Navigheaza inaapoi la fragmentul initial
                    findNavController().navigate(R.id.action_languageSelectionFragment_to_initialFragment)
                } catch (e: Exception) {
                    Log.e("LanguageSelection", "Error setting locale", e)
                }
            }
        }
    }

    //Metoda pentru setarea limbii alicatiei
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
