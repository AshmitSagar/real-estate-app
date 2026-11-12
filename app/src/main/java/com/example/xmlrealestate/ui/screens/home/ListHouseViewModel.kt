package com.example.xmlrealestate.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xmlrealestate.data.repository.SavedHomesRepository
import com.example.xmlrealestate.domain.usecases.house.GetAllHousesUseCase
import com.example.xmlrealestate.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListHouseViewModel @Inject constructor(
    private val getAllHousesUseCase: GetAllHousesUseCase,
    private val savedHomes: SavedHomesRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val state = MutableStateFlow(ListHouseContract.State())
    private var loadJob: Job? = null

    private val currentFilters = MutableStateFlow(
        CatalogFilters(
            query = savedState.get<String>("propertyQuery").orEmpty(),
            city = savedState.get<String>("propertyCity").orEmpty(),
            maxPrice = savedState.get<Int>("propertyBudget") ?: 0,
            bedrooms = savedState.get<Int>("propertyBedrooms") ?: 0
        )
    )
    internal val filters = currentFilters.asStateFlow()

    internal fun updateFilters(filters: CatalogFilters) {
        if (filters == currentFilters.value) return
        savedState["propertyQuery"] = filters.query
        savedState["propertyCity"] = filters.city
        savedState["propertyBudget"] = filters.maxPrice
        savedState["propertyBedrooms"] = filters.bedrooms
        currentFilters.value = filters
    }

    internal fun observeCatalog(savedOnly: Boolean) =
        combine(state, currentFilters, savedHomes.ids) { catalog, filters, savedIds ->
            val houses = catalog.houses.orEmpty()
            val matches = if (savedOnly) houses.filter { it.id in savedIds } else filters.apply(houses)
            val featured = if (!savedOnly && filters.isDefault) matches.find { it.id == FEATURED_ID } else null
            val ordered = if (featured == null) matches else listOf(featured) + matches.filterNot { it.id == FEATURED_ID }
            CatalogScreenState(
                catalog,
                filters,
                ordered.map { house ->
                    CatalogListing(house, house.id == featured?.id, house.id in savedIds)
                }
            )
        }

    internal fun countMatching(filters: CatalogFilters): Int = filters.apply(state.value.houses.orEmpty()).size

    fun toggleSaved(houseId: Int) = savedHomes.toggle(houseId)

    init {
        getHouses()
    }

    fun handleEvent(event: ListHouseContract.Event) {
        when (event) {
            ListHouseContract.Event.GetHouses -> getHouses()
        }
    }

    private fun getHouses() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            getAllHousesUseCase().catch { error ->
                state.update { it.copy(isLoading = false, error = error.toString()) }
            }.collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        state.value = ListHouseContract.State(
                            houses = result.data.orEmpty().sortedBy { it.price },
                            isLoading = false
                        )
                    }
                    is NetworkResult.Error -> {
                        state.update { it.copy(isLoading = false, error = result.message.orEmpty()) }
                    }
                    is NetworkResult.Loading -> {
                        state.update { it.copy(isLoading = true, error = null) }
                    }
                }
            }
        }
    }

    internal data class CatalogScreenState(
        val catalog: ListHouseContract.State,
        val filters: CatalogFilters,
        val listings: List<CatalogListing>
    )

    private companion object {
        const val FEATURED_ID = 7
    }
}
