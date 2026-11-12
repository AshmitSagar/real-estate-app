package com.example.xmlrealestate.ui.screens.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.xmlrealestate.R
import com.example.xmlrealestate.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var navView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configBinding()
        configBottomNav()
    }

    private fun configBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navView = binding.container.findViewById(R.id.nav_view)
    }

    private fun configBottomNav() {
        configBottomNavMenuActions()
        configVisibility()
    }

    //This method is used to handle the bottom navigation menu actions.
    private fun configBottomNavMenuActions() {
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        binding.toolbar.inflateMenu(R.menu.catalog_toolbar_menu)
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId != R.id.navigation_info) return@setOnMenuItemClickListener false
            navController.navigate(R.id.navigation_info)
            true
        }
        binding.toolbar.setNavigationOnClickListener { navController.navigateUp() }
    }

    //This method is used to handle the visibility of the bottom navigation menu and the toolbar.
    private fun configVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isCatalog = destination.id == R.id.navigation_home || destination.id == R.id.navigation_saved
            val isAbout = destination.id == R.id.navigation_info
            navView.visibility = if (isCatalog) View.VISIBLE else View.GONE
            binding.toolbar.visibility = if (isCatalog || isAbout) View.VISIBLE else View.GONE
            binding.toolbar.menu.findItem(R.id.navigation_info).isVisible = isCatalog
            if (isAbout) binding.toolbar.setNavigationIcon(R.drawable.ic_back)
            else binding.toolbar.navigationIcon = null
            binding.toolbar.setNavigationContentDescription(R.string.navigate_back)
        }
    }

    //This method is used to handle the back button press.
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
