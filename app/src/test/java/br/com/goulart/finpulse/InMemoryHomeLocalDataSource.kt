package br.com.goulart.finpulse

import br.com.goulart.finpulse.data.local.HomeLocalDataSource
import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.remote.model.StockQuote
import kotlinx.coroutines.flow.MutableStateFlow

internal class InMemoryHomeLocalDataSource(initial: HomeSnapshot) : HomeLocalDataSource {
    val snapshots = MutableStateFlow(initial)

    override fun observeHome() = snapshots
    override suspend fun readHome() = snapshots.value
    override suspend fun favoriteSymbols() = snapshots.value.favorites.map { it.symbol }

    override suspend fun saveFavoriteQuotes(quotes: List<StockQuote>): HomeSnapshot {
        val bySymbol = quotes.associateBy { it.symbol }
        val updated = snapshots.value.copy(stocks = snapshots.value.stocks.map { stock ->
            val quote = bySymbol[stock.symbol]
            if (stock.isFavorite && quote != null) {
                stock.copy(
                    priceInCents = quote.priceInCents,
                    dailyChangePercent = quote.dailyChangePercent,
                    history = quote.history,
                )
            } else stock
        })
        snapshots.value = updated
        return updated
    }
}
