package com.example.devicemonitorapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.devicemonitorapp.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ascunderea toolbar-ului in timpul afisarii Splash Screen
        (activity as? MainActivity)?.hideToolbar()

        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().navigate(R.id.action_splashFragment_to_initialFragment)
            // Afisarea toolbar-ului dupa navigarea de splash screen
            (activity as? MainActivity)?.showToolbar()
        }, 3000) // Delay de 3 secunde
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
