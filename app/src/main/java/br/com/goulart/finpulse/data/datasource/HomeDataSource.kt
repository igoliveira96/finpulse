package br.com.goulart.finpulse.data.datasource

import br.com.goulart.finpulse.data.model.HomeData
import br.com.goulart.finpulse.data.model.HomeSnapshot
import kotlinx.coroutines.flow.Flow

interface HomeDataSource {
    fun observeHome(): Flow<HomeData>
    suspend fun refreshFavorites(): HomeSnapshot
    suspend fun revalidateFavoritesWhileVisible()
}
