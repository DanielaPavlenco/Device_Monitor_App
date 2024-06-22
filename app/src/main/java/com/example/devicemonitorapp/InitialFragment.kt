package com.example.devicemonitorapp

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.devicemonitorapp.databinding.FragmentInitialBinding

class InitialFragment : Fragment() {

    //Variabila pentru binding-ul layout-ului
    private var _binding: FragmentInitialBinding? = null
    private val binding get() = _binding!!

    //Metoda pentru creare si inflatare a view-ului fragmentului
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInitialBinding.inflate(inflater, container, false)
        return binding.root
    }

    //Metoda pentru configurarea view-ului dupa ce a fost creat
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ascunde toolbar-ul in timpul ecranului splash
        (activity as? MainActivity)?.hideToolbar()

        // Verifica si seteaza fundalul in functie de modul intunecat
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        if (isNightMode) {
            binding.root.setBackgroundResource(R.drawable.night1) // Inlocuieste cu drwable-ul pentru modul dark
        } else {
            binding.root.setBackgroundResource(R.drawable.pic_one) // Inlocuieste cu drawable-ul pentru modul zi
        }

        //Verifica statusul permisiunii necesare
        checkPermissionStatus()

        //Aplica gradient pe textul butoanelor
        applyGradientToText(binding.languageSelection, R.color.orange, R.color.green)
        applyGradientToText(binding.faqs, R.color.green, R.color.orange)
        applyGradientToText(binding.aboutApp, R.color.orange, R.color.green)
        applyGradientToText(binding.nextButton, R.color.green, R.color.orange)

        //Seteaza textul butoanelor
        binding.languageSelection.text = getString(R.string.language_selection)
        binding.faqs.text = getString(R.string.faqs)
        binding.aboutApp.text = getString(R.string.about_app)
        binding.nextButton.text = getString(R.string.next)

        //Seteaza click listeners pentru butoane
        binding.languageSelection.setOnClickListener {
            findNavController().navigate(R.id.action_initialFragment_to_languageSelectionFragment)
        }

        binding.aboutApp.setOnClickListener {
            findNavController().navigate(R.id.action_initialFragment_to_aboutFragment)
        }
        binding.faqs.setOnClickListener {
            findNavController().navigate(R.id.action_initialFragment_to_faqsFragment)
        }
        binding.nextButton.setOnClickListener {
            findNavController().navigate(R.id.action_initialFragment_to_homeFragment)
        }
    }

    //Aaplica gradient pe textul butoanelor
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

    //verifica statusul permisiunii necerare
    private fun checkPermissionStatus() {
        val requiredPermission = "android.permission.WRITE_SECURE_SETTINGS"
        val checkVal = ContextCompat.checkSelfPermission(requireContext(), requiredPermission)
        if (checkVal == PackageManager.PERMISSION_GRANTED) {
            //Permisiunea a fost acordata
            Toast.makeText(requireContext(), "WRITE_SECURE_SETTINGS has been granted to the application. You may now continue!", Toast.LENGTH_LONG).show()
            showMainContent()
        } else {
            //Permisiunea nu a fostt acordata
            Toast.makeText(requireContext(), "WRITE_SECURE_SETTINGS has not been granted to the application. Please grant access to continue.", Toast.LENGTH_LONG).show()
            showPermissionDialog()
        }
    }

    //Afiseza un dialog pentru cererea permisiunii
    private fun showPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("This app requires WRITE_SECURE_SETTINGS permission to function correctly. Please grant the permission using ADB with the following command:\n\nadb shell pm grant ${requireContext().packageName} android.permission.WRITE_SECURE_SETTINGS\n\nIf you don't grant the permission, some features may not work correctly.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                showMainContent()
            }
            .setCancelable(false)
            .show()
    }

    //Metoda pentru curatarea binding-ului la dispitrugerea view-ului
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //Afiseaza continutul principal
    private fun showMainContent() {
        binding.textView.visibility = View.VISIBLE
        binding.aboutAppCard.visibility = View.VISIBLE
        binding.faqsCard.visibility = View.VISIBLE
        binding.languageSelectionCard.visibility = View.VISIBLE
        binding.nextButtonCard.visibility = View.VISIBLE
    }
}
