package com.example.xmlrealestate.ui.screens.home

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.core.app.SharedElementCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.Fade
import com.example.xmlrealestate.MobileNavigationDirections
import com.example.xmlrealestate.R
import com.example.xmlrealestate.databinding.FragmentListHousesBinding
import com.example.xmlrealestate.domain.model.House
import com.example.xmlrealestate.ui.catalogMotionEnabled
import com.example.xmlrealestate.ui.propertyPhotoName
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ListHouseFragment : Fragment(R.layout.fragment_list_houses) {
    private val viewModel: ListHouseViewModel by viewModels()
    private val savedOnly get() = arguments?.getBoolean("savedOnly") == true
    private var returningToPhoto = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentListHousesBinding.bind(view)
        val adapter = HouseAdapter(
            onOpen = { house, photo -> openProperty(binding, house, photo) },
            onSave = viewModel::toggleSaved
        )
        binding.rvHouses.adapter = adapter
        binding.catalogTitle.setText(if (savedOnly) R.string.saved_homes else R.string.explore_properties)
        binding.searchField.isVisible = !savedOnly
        binding.cityChoices.isVisible = !savedOnly
        binding.openFilters.isVisible = !savedOnly

        if (!savedOnly) bindFilters(binding)
        if (view.context.catalogMotionEnabled()) {
            exitTransition = Fade(Fade.OUT).apply { duration = 140 }
            reenterTransition = Fade(Fade.IN).apply { duration = 140 }
            configureSharedElement(binding)
            if (returningToPhoto) postponeEnterTransition(280L, TimeUnit.MILLISECONDS)
        }

        var previousFilters: CatalogFilters? = null
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observeCatalog(savedOnly).collect { screen ->
                    val filtersChanged = screen.filters != previousFilters
                    val resetScroll = !savedOnly && filtersChanged && previousFilters != null
                    if (!savedOnly && filtersChanged) renderFilters(binding, screen.filters)
                    previousFilters = screen.filters
                    render(binding, adapter, screen, resetScroll)
                }
            }
        }
    }

    private fun bindFilters(binding: FragmentListHousesBinding) = with(binding) {
        propertySearch.setText(viewModel.filters.value.query)
        propertySearch.doAfterTextChanged { text ->
            viewModel.updateFilters(viewModel.filters.value.copy(query = text.toString()))
        }
        propertySearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            hideKeyboard(binding)
            true
        }
        cityFilters.setOnCheckedStateChangeListener { _, ids ->
            val city = cities[ids.firstOrNull()].orEmpty()
            viewModel.updateFilters(viewModel.filters.value.copy(city = city))
        }
        openFilters.setOnClickListener {
            hideKeyboard(binding)
            if (childFragmentManager.findFragmentByTag(FILTER_SHEET) == null) {
                CatalogFilterSheet().show(childFragmentManager, FILTER_SHEET)
            }
        }
        childFragmentManager.setFragmentResultListener(
            CatalogFilterSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, result ->
            viewModel.updateFilters(viewModel.filters.value.copy(
                maxPrice = result.getInt(CatalogFilterSheet.BUDGET_KEY),
                bedrooms = result.getInt(CatalogFilterSheet.BEDROOMS_KEY)
            ))
        }
    }

    private fun renderFilters(binding: FragmentListHousesBinding, filters: CatalogFilters) = with(binding) {
        cityFilters.check(cities.entries.firstOrNull { it.value == filters.city }?.key ?: R.id.filterAll)
        activeFilters.removeAllViews()
        if (filters.maxPrice != 0) {
            CatalogFilterSheet.budgets[filters.maxPrice]?.let { label ->
                addActiveFilter(binding, label) {
                    viewModel.updateFilters(viewModel.filters.value.copy(maxPrice = 0))
                }
            }
        }
        if (filters.bedrooms != 0) {
            CatalogFilterSheet.bedroomCounts[filters.bedrooms]?.let { label ->
                addActiveFilter(binding, label) {
                    viewModel.updateFilters(viewModel.filters.value.copy(bedrooms = 0))
                }
            }
        }
        activeFilters.isVisible = activeFilters.childCount != 0
    }

    private fun addActiveFilter(binding: FragmentListHousesBinding, label: Int, onRemove: () -> Unit) {
        val group = binding.activeFilters
        val chip = layoutInflater.inflate(R.layout.item_filter_chip, group, false) as Chip
        chip.isCheckable = false
        chip.setText(label)
        chip.isCloseIconVisible = true
        chip.closeIconContentDescription = getString(R.string.clear_filter_option, getString(label))
        chip.setOnCloseIconClickListener { onRemove() }
        group.addView(chip)
    }

    private fun render(
        binding: FragmentListHousesBinding,
        adapter: HouseAdapter,
        screen: ListHouseViewModel.CatalogScreenState,
        resetScroll: Boolean
    ) = with(binding) {
        adapter.submitList(screen.listings) {
            if (resetScroll) rvHouses.scrollToPosition(0)
            if (returningToPhoto && !screen.catalog.isLoading) {
                root.doOnPreDraw {
                    returningToPhoto = false
                    startPostponedEnterTransition()
                }
            }
        }

        val count = screen.listings.size
        val error = screen.catalog.error != null
        val isLoading = screen.catalog.isLoading && count == 0
        catalogControls.isVisible = !savedOnly || count > 0
        resultCount.isVisible = !isLoading && !error
        resultCount.text = resources.getQuantityString(R.plurals.property_count, count, count)
        loading.isVisible = isLoading
        rvHouses.isVisible = count != 0
        statusPanel.isVisible = count == 0 && !isLoading
        val clearFilters = !screen.filters.isDefault && !savedOnly && !error
        statusMessage.setText(when {
            error -> R.string.properties_unavailable
            savedOnly -> R.string.empty_saved_homes
            clearFilters -> R.string.property_no_matches
            else -> R.string.no_properties
        })
        statusAction.setText(when {
            error -> R.string.retry_properties
            savedOnly -> R.string.explore_properties
            clearFilters -> R.string.clear_filters
            else -> R.string.retry_properties
        })
        statusAction.setOnClickListener {
            when {
                error -> viewModel.handleEvent(ListHouseContract.Event.GetHouses)
                savedOnly -> requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
                    .selectedItemId = R.id.navigation_home
                clearFilters -> {
                    viewModel.updateFilters(CatalogFilters())
                    propertySearch.text?.clear()
                }
                else -> viewModel.handleEvent(ListHouseContract.Event.GetHouses)
            }
        }
    }

    private fun openProperty(binding: FragmentListHousesBinding, house: House, photo: ImageView?) {
        val nav = findNavController()
        val sourceId = if (savedOnly) R.id.navigation_saved else R.id.navigation_home
        if (nav.currentDestination?.id != sourceId) return
        hideKeyboard(binding)
        val sharedPhoto = photo?.takeIf { it.context.catalogMotionEnabled() }
        returningToPhoto = sharedPhoto != null
        val action = MobileNavigationDirections.actionOpenProperty(house.id.toString(), returningToPhoto)
        if (sharedPhoto == null) nav.navigate(action)
        else nav.navigate(action, FragmentNavigatorExtras(sharedPhoto to propertyPhotoName(house.id)))
    }

    private fun configureSharedElement(binding: FragmentListHousesBinding) {
        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                for (name in names) {
                    val photo = binding.rvHouses.findViewWithTag<View>(name)
                    if (photo == null) sharedElements.remove(name)
                    else sharedElements[name] = photo
                }
            }
        })
    }

    private fun hideKeyboard(binding: FragmentListHousesBinding) {
        WindowCompat.getInsetsController(requireActivity().window, binding.propertySearch)
            .hide(WindowInsetsCompat.Type.ime())
        binding.propertySearch.clearFocus()
    }

    override fun onDestroyView() {
        setExitSharedElementCallback(null)
        super.onDestroyView()
    }

    private companion object {
        const val FILTER_SHEET = "catalogFilters"
        val cities = mapOf(
            R.id.filterAll to "",
            R.id.filterNoida to "Noida",
            R.id.filterDelhi to "Delhi",
            R.id.filterGurugram to "Gurugram"
        )
    }
}
