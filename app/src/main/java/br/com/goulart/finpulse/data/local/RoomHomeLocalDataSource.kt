package br.com.goulart.finpulse.data.local

import androidx.room.withTransaction
import br.com.goulart.finpulse.data.local.model.toInvestor
import br.com.goulart.finpulse.data.local.model.toPortfolio
import br.com.goulart.finpulse.data.local.model.toStock
import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.remote.model.StockQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomHomeLocalDataSource(
    private val database: FinPulseDatabase,
    // The local seed contains a single investor; a future session can supply another id.
    private val investorId: Long = 1L,
) : HomeLocalDataSource {
    override fun observeHome(): Flow<HomeSnapshot> = database.invalidationTracker.createFlow(
        "investors", "portfolios", "portfolio_history", "stocks", "stock_history", "favorites",
        emitInitialState = true,
    ).map { readHome() }.distinctUntilChanged()

    override suspend fun favoriteSymbols(): List<String> =
        database.favoriteDao().getByInvestor(investorId).map { it.stockSymbol }

    override suspend fun saveFavoriteQuotes(quotes: List<StockQuote>): HomeSnapshot = database.withTransaction {
        // A favorite may have been removed while the request was in flight.
        val currentFavorites = favoriteSymbols().toSet()
        val stocks = database.stockDao().getAllWithHistory().associateBy { it.stock.symbol }
        quotes.filter { it.symbol in currentFavorites }.forEach { quote ->
            val stock = stocks[quote.symbol]?.stock ?: return@forEach
            database.stockDao().saveWithHistory(
                stock.copy(priceInCents = quote.priceInCents, dailyChangePercent = quote.dailyChangePercent),
                quote.history,
            )
        }
        readHome()
    }

    override suspend fun readHome(): HomeSnapshot = database.withTransaction {
        val investor = checkNotNull(database.investorDao().getById(investorId)) {
            "Investor $investorId is missing from the local database"
        }
        val portfolio = checkNotNull(database.portfolioDao().getWithHistory(investorId)) {
            "Portfolio for investor $investorId is missing from the local database"
        }
        val favorites = favoriteSymbols().toSet()
        val stocks = database.stockDao().getAllWithHistory().map { stock ->
            stock.toStock(isFavorite = stock.stock.symbol in favorites)
        }
        HomeSnapshot(investor.toInvestor(), portfolio.toPortfolio(), stocks)
    }
}
