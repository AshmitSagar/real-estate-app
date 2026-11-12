package com.example.xmlrealestate.data.repository

import android.content.Context
import com.example.xmlrealestate.domain.model.House
import com.example.xmlrealestate.network.NetworkResult
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseRepository @Inject constructor(@ApplicationContext context: Context) {
    private val catalog by lazy {
        context.assets.open("india_properties.json").bufferedReader().use { reader ->
            Gson().fromJson(reader, Array<House>::class.java).toList()
        }
    }

    fun getAllHouses(): Flow<NetworkResult<List<House>>> = flow {
        emit(NetworkResult.Loading())
        emit(NetworkResult.Success(catalog))
    }.flowOn(Dispatchers.IO)

    fun getHouse(id: Int): Flow<House> = flow {
        val house = catalog.first { it.id == id }
        emit(house.copy())
    }.flowOn(Dispatchers.IO)
}
