package com.example.xmlrealestate.ui.screens.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.xmlrealestate.R
import com.example.xmlrealestate.common.Constants
import com.example.xmlrealestate.databinding.FragmentAboutBinding

class AboutFragment : Fragment(R.layout.fragment_about) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        FragmentAboutBinding.bind(view).aboutDeveloperContact.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GITHUB_PROFILE_URL)))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.profile_open_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
