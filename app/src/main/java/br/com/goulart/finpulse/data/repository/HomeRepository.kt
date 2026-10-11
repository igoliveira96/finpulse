package br.com.goulart.finpulse.data.repository

import br.com.goulart.finpulse.data.model.HomeSnapshot
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeHome(): Flow<HomeSnapshot>
    suspend fun refreshFavorites(): HomeSnapshot
}
