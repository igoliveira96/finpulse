package br.com.goulart.finpulse.data.repository

import br.com.goulart.finpulse.data.model.HomeData
import br.com.goulart.finpulse.data.model.HomeSnapshot
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeHome(): Flow<HomeData>
    suspend fun refreshFavorites(): HomeSnapshot
    suspend fun revalidateFavoritesWhileVisible()
}
