package com.example.xmlrealestate.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xmlrealestate.domain.usecases.house.GetAllHousesUseCase
import com.example.xmlrealestate.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListHouseViewModel @Inject constructor(
    private val getAllHousesUseCase: GetAllHousesUseCase,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val state = MutableStateFlow(ListHouseContract.State())
    val uiState = state.asStateFlow()
    private var loadJob: Job? = null

    var query: String
        get() = savedState.get<String>("propertyQuery").orEmpty()
        set(value) { savedState["propertyQuery"] = value }

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
}
