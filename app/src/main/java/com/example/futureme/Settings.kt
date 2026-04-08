package com.example.futureme

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.futureme.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // SharedPreferences file for storing app settings
        val sharedPreferences = requireActivity().getSharedPreferences(
            "futureme_settings",
            Context.MODE_PRIVATE
        )

        // Read saved dark mode value
        val isDarkModeOn = sharedPreferences.getBoolean("dark_mode", false)

        // Set switch state when page opens
        binding.darkModeSwitch.isChecked = isDarkModeOn

        // Listen for toggle changes
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save the setting
            sharedPreferences.edit()
                .putBoolean("dark_mode", isChecked)
                .apply()

            // Apply the mode
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // sets up the add tag button for navigation
        setupTagButton()


        return binding.root
    }

    private fun setupTagButton(){
        binding.addTagButton.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_addTags)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}