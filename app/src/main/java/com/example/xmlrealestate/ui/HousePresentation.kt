package com.example.xmlrealestate.ui

import android.content.Context
import android.icu.text.NumberFormat
import android.icu.util.Currency
import com.example.xmlrealestate.R
import com.example.xmlrealestate.domain.model.House
import java.util.Locale

internal fun House.displayPrice(): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
        currency = Currency.getInstance("INR")
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }.format(price)

internal fun House.displayPlace(context: Context): String {
    val place = listOf(locality, city).filter(String::isNotBlank).joinToString(", ")
    return when {
        place.isBlank() -> zipCode
        zipCode.isBlank() -> place
        else -> context.getString(R.string.property_place, place, zipCode)
    }
}

internal fun House.displaySpecs(context: Context): String {
    val beds = context.resources.getQuantityString(R.plurals.property_beds, bedrooms, bedrooms)
    val baths = context.resources.getQuantityString(R.plurals.property_baths, bathrooms, bathrooms)
    return context.getString(R.string.property_specs, beds, baths, size)
}
