package com.example.xmlrealestate.domain.model

data class House(
    val id: Int,
    val image: String,
    val price: Int,
    val bedrooms: Int,
    val bathrooms: Int,
    val size: Int,
    val description: String,
    val zipCode: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val creationDate: String,
    var distance: String? = null,
    val locality: String = "",
)
