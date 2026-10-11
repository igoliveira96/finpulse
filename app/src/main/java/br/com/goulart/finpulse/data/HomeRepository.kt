package br.com.goulart.finpulse.data

import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeHome(): Flow<HomeSnapshot>
    suspend fun refreshFavorites(): HomeSnapshot
}
