package com.example.xmlrealestate.ui.screens.home

import com.example.xmlrealestate.domain.model.House
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogFiltersTest {
    @Test
    fun combinesCityBudgetAndBedrooms() {
        val houses = listOf(
            house(1, "Noida", "Sector 62", 15_000_000, 3),
            house(2, "Noida", "Sector 62", 15_000_001, 3),
            house(3, "Noida", "Sector 62", 12_000_000, 2),
            house(4, "Delhi", "Saket", 14_000_000, 3)
        )
        val filters = CatalogFilters(city = "Noida", maxPrice = 15_000_000, bedrooms = 3)

        assertEquals(listOf(1), filters.apply(houses).map(House::id))
    }

    @Test
    fun matchesGurgaonAliasAndLocalityTogether() {
        val houses = listOf(
            house(1, "Gurugram", "South City II", 13_250_000, 3, "122018"),
            house(2, "Gurugram", "DLF Phase II", 16_500_000, 3, "122008")
        )

        assertEquals(listOf(1), CatalogFilters(query = "South City II, Gurgaon 122018").apply(houses).map(House::id))
        assertEquals(emptyList<House>(), CatalogFilters(query = "Gurgaon 110017").apply(houses))
    }

    private fun house(
        id: Int,
        city: String,
        locality: String,
        price: Int,
        bedrooms: Int,
        pinCode: String = "201309"
    ) = House(
        id = id,
        image = "",
        price = price,
        bedrooms = bedrooms,
        bathrooms = 2,
        size = 100,
        description = "",
        zipCode = pinCode,
        city = city,
        latitude = 28.6,
        longitude = 77.2,
        creationDate = "",
        locality = locality
    )
}
