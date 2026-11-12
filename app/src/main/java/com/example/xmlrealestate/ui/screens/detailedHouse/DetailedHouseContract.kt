package com.example.xmlrealestate.ui.screens.detailedHouse

import com.example.xmlrealestate.domain.model.House

interface DetailedHouseContract {
    data class State(
        val house: House? = null,
        val error: Boolean = false
    )
}
