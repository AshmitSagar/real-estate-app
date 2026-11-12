package com.example.xmlrealestate.ui.screens.home

import com.example.xmlrealestate.domain.model.House

interface ListHouseContract {
    sealed class Event {
        object GetHouses : Event()
    }

    data class State(
        val houses: List<House>? = null,
        val isLoading: Boolean = true,
        val error: String? = null
    )
}
