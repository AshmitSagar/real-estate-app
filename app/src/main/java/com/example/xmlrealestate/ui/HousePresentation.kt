package com.example.xmlrealestate.ui

import android.content.Context
import com.example.xmlrealestate.R
import com.example.xmlrealestate.domain.model.House
import java.text.NumberFormat
import java.util.Currency

internal fun House.displayPrice(): String =
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("USD")
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }.format(price)

internal fun House.displayPlace(context: Context): String = when {
    city.isBlank() -> zipCode
    zipCode.isBlank() -> city
    else -> context.getString(R.string.property_place, city, zipCode)
}

internal fun House.displaySpecs(context: Context): String {
    val beds = context.resources.getQuantityString(R.plurals.property_beds, bedrooms, bedrooms)
    val baths = context.resources.getQuantityString(R.plurals.property_baths, bathrooms, bathrooms)
    return context.getString(R.string.property_specs, beds, baths, size)
}
