package br.com.goulart.finpulse.data.local

import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.remote.model.StockQuote
import kotlinx.coroutines.flow.Flow

interface HomeLocalDataSource {
    fun observeHome(): Flow<HomeSnapshot>
    suspend fun readHome(): HomeSnapshot
    suspend fun favoriteSymbols(): List<String>
    suspend fun saveFavoriteQuotes(quotes: List<StockQuote>): HomeSnapshot
}
