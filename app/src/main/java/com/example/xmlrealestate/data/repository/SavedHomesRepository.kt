package com.example.xmlrealestate.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedHomesRepository @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("saved_homes", Context.MODE_PRIVATE)
    private val savedIds = MutableStateFlow(
        preferences.getStringSet(KEY, emptySet()).orEmpty().mapNotNull(String::toIntOrNull).toSet()
    )
    val ids = savedIds.asStateFlow()

    @Synchronized
    fun toggle(houseId: Int) {
        val current = savedIds.value
        val updated = if (houseId in current) current - houseId else current + houseId
        preferences.edit().putStringSet(KEY, updated.map(Int::toString).toSet()).apply()
        savedIds.value = updated
    }

    private companion object {
        const val KEY = "property_ids"
    }
}
