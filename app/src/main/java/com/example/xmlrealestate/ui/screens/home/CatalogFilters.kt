package com.example.xmlrealestate.ui.screens.home

import com.example.xmlrealestate.domain.model.House
import java.util.Locale

internal data class CatalogFilters(
    val query: String = "",
    val city: String = "",
    val maxPrice: Int = 0,
    val bedrooms: Int = 0
) {
    val isDefault: Boolean
        get() = query.isBlank() && city.isEmpty() && maxPrice == 0 && bedrooms == 0

    fun apply(houses: List<House>): List<House> {
        val queryWords = query.lowercase(Locale.ROOT).split(QUERY_SEPARATOR).filter(String::isNotEmpty)
        return houses.filter { house ->
            val cityNames = if (house.city == "Gurugram") "Gurugram Gurgaon" else house.city
            val searchable = "${house.locality} $cityNames ${house.zipCode}".lowercase(Locale.ROOT)
            (city.isEmpty() || house.city == city) &&
                (maxPrice == 0 || house.price <= maxPrice) &&
                (bedrooms == 0 || house.bedrooms == bedrooms) &&
                queryWords.all(searchable::contains)
        }
    }

    private companion object {
        val QUERY_SEPARATOR = Regex("[\\s,·]+")
    }
}
