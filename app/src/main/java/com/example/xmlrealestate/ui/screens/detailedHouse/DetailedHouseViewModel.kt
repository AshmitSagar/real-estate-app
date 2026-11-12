package com.example.xmlrealestate.ui.screens.detailedHouse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xmlrealestate.domain.usecases.house.GetHouseUseCase
import com.example.xmlrealestate.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailedHouseViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getHouseUseCase: GetHouseUseCase
) : ViewModel() {
    private val state = MutableStateFlow(DetailedHouseContract.State())
    val uiState = state.asStateFlow()
    private var requestedId: Int? = null

    fun loadHouse(houseId: Int?) {
        if (houseId == null) {
            state.value = DetailedHouseContract.State(error = true)
            return
        }
        if (requestedId == houseId) return
        requestedId = houseId
        viewModelScope.launch {
            getHouseUseCase(houseId).catch {
                state.value = DetailedHouseContract.State(error = true)
            }.collect { house ->
                state.value = DetailedHouseContract.State(
                    house = Utils.getHouseWithDistance(house, appContext)
                )
            }
        }
    }
}
