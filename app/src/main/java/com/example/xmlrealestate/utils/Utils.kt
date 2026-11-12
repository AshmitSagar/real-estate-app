package com.example.xmlrealestate.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import androidx.core.content.ContextCompat
import com.example.xmlrealestate.common.Constants
import com.example.xmlrealestate.domain.model.House


/**
 * A utility class that provides several utility functions for checking internet connectivity. getting the device location and calculating distances between locations.
 */

object Utils {
    /**
     * Checks if the device has an active internet connection.
     * @param context The context used to retrieve system connectivity service.
     * @return `true` if the device has an active internet connection, otherwise return `false`.
     */
    fun hasInternetConnection(context: Context?): Boolean {
        if (context == null) return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /**
     * Returns the most accurate location of the device.
     * @param context the context of the calling activity or application
     * @return The most accurate location of the device, or null if the location is not available
     */
    private fun getMostAccurateLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var mostAccurateLocation: Location? = null

        val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasLocationPermission = permissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasLocationPermission && isLocationEnabled(locationManager)) {
            for (provider in providers) {
                val location = try {
                    locationManager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                } ?: continue
                if (mostAccurateLocation == null || location.accuracy < mostAccurateLocation.accuracy) {
                    mostAccurateLocation = location
                }
            }
        }
        return mostAccurateLocation
    }

    /**
     * Calculates the distance between the specified house and the specified location.
     * @param myObject The house to calculate the distance to
     * @param myLocation The location to calculate the distance from
     * @return The specified house with the distance to the specified location set, if the location is null the distance will be set to [Constants.NO_DISTANCE_AVAILABLE]
     */
    private fun calculateDistanceToHouse(myObject: House, myLocation: Location?): House {
        val distance = if (myLocation == null) {
            Constants.NO_DISTANCE_AVAILABLE
        } else {
            val houseLocation = Location("")
            houseLocation.latitude = myObject.latitude
            houseLocation.longitude = myObject.longitude
            val distanceInMeters = myLocation.distanceTo(houseLocation)
            "%.2f km".format(distanceInMeters / 1000.0)
        }
        myObject.distance = distance
        return myObject
    }

    /**
     * Returns the specified house with the distance to the device location set calling [getMostAccurateLocation]
     */
    fun getHouseWithDistance(myObject: House, context: Context): House {
        val myLocation = getMostAccurateLocation(context)
        return calculateDistanceToHouse(myObject, myLocation)
    }

    /**
     *  Returns the specified houses with the distance to the device location set calling [getMostAccurateLocation], for not having to call [getMostAccurateLocation] multiple times, optimizing the performance.
     * @param myObjects List of [House] to calculate the distance to.
     * @param context Context of the calling activity or application.
     * @return The specified houses with the distance to the device location.
     */
    fun calculateDistanceToHouses(myObjects: List<House>, context: Context): List<House> {
        val myLocation = getMostAccurateLocation(context)
        for (house in myObjects) {
            calculateDistanceToHouse(house, myLocation)
        }
        return myObjects
    }
}
