package com.example.xmlrealestate.ui.screens.detailedHouse

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.Fade
import androidx.transition.TransitionSet
import coil.load
import com.example.xmlrealestate.R
import com.example.xmlrealestate.common.Constants
import com.example.xmlrealestate.databinding.FragmentDetailedHouseBinding
import com.example.xmlrealestate.domain.model.House
import com.example.xmlrealestate.ui.catalogMotionEnabled
import com.example.xmlrealestate.ui.displayPlace
import com.example.xmlrealestate.ui.displayPrice
import com.example.xmlrealestate.ui.propertyPhotoName
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class DetailedHouseFragment : Fragment(R.layout.fragment_detailed_house) {
    private val viewModel: DetailedHouseViewModel by viewModels()
    private val args: DetailedHouseFragmentArgs by navArgs()
    private var binding: FragmentDetailedHouseBinding? = null
    private var heroPhotoReady = false
    private val requestDistancePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.showDistance()
        else binding?.houseDistanceDetailed?.apply {
            isVisible = true
            setText(R.string.location_permission_denied)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val views = FragmentDetailedHouseBinding.bind(view)
        binding = views
        configureMotion(views)
        views.mapViewHouseLocation.onCreate(savedInstanceState?.getBundle(MAP_STATE))
        views.detailBack.setOnClickListener { findNavController().navigateUp() }
        views.bookmarkProperty.setOnClickListener { viewModel.toggleSaved() }
        views.showDistance.setOnClickListener {
            val permission = Manifest.permission.ACCESS_COARSE_LOCATION
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                viewModel.showDistance()
            } else {
                requestDistancePermission.launch(permission)
            }
        }
        viewModel.loadHouse(args.houseId.toIntOrNull())

        viewLifecycleOwner.lifecycleScope.launch {
            var displayedHouseId: Int? = null
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.uiState, viewModel.savedIds, ::Pair).collect { (state, savedIds) ->
                    views.detailStatus.isVisible = state.house == null
                    views.detailStatus.setText(
                        if (state.error) R.string.property_unavailable else R.string.loading_property
                    )
                    views.detailContent.isVisible = state.house != null
                    val house = state.house
                    views.bookmarkProperty.isVisible = house != null
                    val saved = house != null && house.id in savedIds
                    views.bookmarkProperty.isSelected = saved
                    views.bookmarkProperty.contentDescription = getString(
                        if (saved) R.string.remove_saved_property else R.string.save_property
                    )
                    if (house != null && house.id != displayedHouseId) {
                        renderHouse(views, house)
                        displayedHouseId = house.id
                    }
                    house?.distance?.let { distance ->
                        views.houseDistanceDetailed.isVisible = true
                        views.houseDistanceDetailed.text = if (distance == Constants.NO_DISTANCE_AVAILABLE) {
                            getString(R.string.distance_unavailable)
                        } else getString(R.string.property_distance, distance)
                    }
                    if (state.error) startPostponedEnterTransition()
                }
            }
        }
    }

    private fun configureMotion(views: FragmentDetailedHouseBinding) {
        if (!views.root.context.catalogMotionEnabled()) return
        enterTransition = Fade(Fade.IN).apply { duration = 180 }
        returnTransition = Fade(Fade.OUT).apply { duration = 140 }
        if (!args.animatePhoto) return

        args.houseId.toIntOrNull()?.let { views.housePhotoDetailed.transitionName = propertyPhotoName(it) }
        sharedElementEnterTransition = photoTransition()
        sharedElementReturnTransition = photoTransition()
        heroPhotoReady = false
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                if (!heroPhotoReady) sharedElements.clear()
            }
        })
        postponeEnterTransition(260L, TimeUnit.MILLISECONDS)
    }

    private fun photoTransition() = TransitionSet()
        .addTransition(ChangeBounds())
        .addTransition(ChangeImageTransform())
        .setDuration(280)

    private fun renderHouse(views: FragmentDetailedHouseBinding, house: House) = with(views) {
        val context = root.context
        housePriceDetailed.text = house.displayPrice()
        housePlaceDetailed.text = house.displayPlace(context)
        housePlaceDetailed.isVisible = housePlaceDetailed.text.isNotBlank()
        val numberFormat = NumberFormat.getIntegerInstance()
        houseBedrooms.text = numberFormat.format(house.bedrooms)
        houseBathrooms.text = numberFormat.format(house.bathrooms)
        houseArea.text = getString(R.string.property_area_value, house.size)
        descriptionHeading.isVisible = house.description.isNotBlank()
        descriptionDetailedTextView.isVisible = descriptionHeading.isVisible
        descriptionDetailedTextView.text = house.description
        housePhotoDetailed.load(Constants.IMAGE_RELATIVE_PATH + house.image) {
            placeholder(R.drawable.catalog_photo_placeholder)
            error(R.drawable.catalog_photo_placeholder)
            listener(
                onSuccess = { _, _ ->
                    if (binding === views) {
                        heroPhotoReady = true
                        root.doOnPreDraw { startPostponedEnterTransition() }
                    }
                },
                onError = { _, _ -> if (binding === views) startPostponedEnterTransition() }
            )
        }

        val location = LatLng(house.latitude, house.longitude)
        openMap.setOnClickListener { openMaps(location) }
        mapViewHouseLocation.getMapAsync { map ->
            if (binding !== views) return@getMapAsync
            map.clear()
            map.uiSettings.isMapToolbarEnabled = false
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13f))
            map.addMarker(MarkerOptions().position(location))
            map.setOnMapClickListener { openMaps(location) }
            map.setOnMapLoadedCallback {
                if (binding === views) mapFallback.isVisible = false
            }
        }
    }

    private fun openMaps(location: LatLng) {
        val coordinates = "${location.latitude},${location.longitude}"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$coordinates")))
        } catch (_: ActivityNotFoundException) {
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$coordinates")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.map_open_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding?.mapViewHouseLocation?.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding?.mapViewHouseLocation?.onResume()
    }

    override fun onPause() {
        binding?.mapViewHouseLocation?.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding?.mapViewHouseLocation?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding?.mapViewHouseLocation?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding?.mapViewHouseLocation?.let { mapView ->
            val mapState = Bundle()
            mapView.onSaveInstanceState(mapState)
            outState.putBundle(MAP_STATE, mapState)
        }
    }

    override fun onDestroyView() {
        val mapView = binding?.mapViewHouseLocation
        binding = null
        setEnterSharedElementCallback(null)
        mapView?.onDestroy()
        super.onDestroyView()
    }

    private companion object {
        const val MAP_STATE = "propertyMapState"
    }
}
