package com.example.xmlrealestate.ui.screens.home

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.xmlrealestate.R
import com.example.xmlrealestate.databinding.FragmentListHousesBinding
import com.example.xmlrealestate.domain.model.House
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListHouseFragment : Fragment(R.layout.fragment_list_houses) {
    private val viewModel: ListHouseViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentListHousesBinding.bind(view)
        val adapter = HouseAdapter(object : ListHousesActions {
            override fun onHouseClicked(house: House) {
                hideKeyboard(binding)
                val action = ListHouseFragmentDirections
                    .actionHomeFragmentToDetailedHouseFragment(house.id.toString())
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.navigation_home) {
                    navController.navigate(action)
                }
            }
        })
        binding.rvHouses.adapter = adapter
        binding.propertySearch.setText(viewModel.query)
        binding.propertySearch.doAfterTextChanged { text ->
            viewModel.query = text.toString()
            render(binding, adapter, viewModel.uiState.value, resetScroll = true)
        }
        binding.propertySearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return@setOnEditorActionListener false
            }
            hideKeyboard(binding)
            true
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(binding, adapter, it) }
            }
        }
    }

    private fun render(
        binding: FragmentListHousesBinding,
        adapter: HouseAdapter,
        state: ListHouseContract.State,
        resetScroll: Boolean = false
    ) = with(binding) {
        val query = viewModel.query.trim()
        val postalQuery = query.filterNot(Char::isWhitespace)
        val houses = state.houses.orEmpty()
        val matches = if (query.isEmpty()) houses else houses.filter { house ->
            "${house.locality} ${house.city}".contains(query, ignoreCase = true) ||
                (house.city == "Gurugram" && "Gurgaon".contains(query, ignoreCase = true)) ||
                house.zipCode.filterNot(Char::isWhitespace).contains(postalQuery, ignoreCase = true)
        }
        adapter.submitList(matches) {
            if (resetScroll) rvHouses.scrollToPosition(0)
        }

        val isLoading = state.isLoading && houses.isEmpty()
        resultCount.isVisible = !isLoading && state.error == null && matches.isNotEmpty()
        resultCount.text = resources.getQuantityString(
            R.plurals.property_count, matches.size, matches.size
        )
        loading.isVisible = isLoading
        rvHouses.isVisible = matches.isNotEmpty()
        statusPanel.isVisible = matches.isEmpty() && !isLoading
        val canClear = query.isNotEmpty() && houses.isNotEmpty()
        statusMessage.setText(when {
            state.error != null -> R.string.properties_unavailable
            canClear -> R.string.property_no_matches
            else -> R.string.no_properties
        })
        statusAction.setText(if (canClear) R.string.clear_property_search else R.string.retry_properties)
        statusAction.setOnClickListener {
            if (canClear) propertySearch.text?.clear()
            else viewModel.handleEvent(ListHouseContract.Event.GetHouses)
        }
    }

    private fun hideKeyboard(binding: FragmentListHousesBinding) {
        WindowCompat.getInsetsController(requireActivity().window, binding.propertySearch)
            .hide(WindowInsetsCompat.Type.ime())
        binding.propertySearch.clearFocus()
    }
}
