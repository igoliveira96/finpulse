package br.com.goulart.finpulse.data.local

import androidx.room.withTransaction
import br.com.goulart.finpulse.data.HomeRepository
import br.com.goulart.finpulse.data.HomeSnapshot
import br.com.goulart.finpulse.data.FavoriteQuotesApi
import br.com.goulart.finpulse.data.MockFavoriteQuotesApi
import br.com.goulart.finpulse.data.local.model.toInvestor
import br.com.goulart.finpulse.data.local.model.toPortfolio
import br.com.goulart.finpulse.data.local.model.toStock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomHomeRepository(
    private val database: FinPulseDatabase,
    // The local seed contains a single investor; a future session can supply another id.
    private val investorId: Long = 1L,
    private val quotesApi: FavoriteQuotesApi = MockFavoriteQuotesApi(),
) : HomeRepository {
    private val refreshMutex = Mutex()

    override fun observeHome(): Flow<HomeSnapshot> = database.invalidationTracker.createFlow(
        "investors", "portfolios", "portfolio_history", "stocks", "stock_history", "favorites",
        emitInitialState = true,
    ).map {
        readSnapshot()
    }.distinctUntilChanged()

    override suspend fun refreshFavorites(): HomeSnapshot = refreshMutex.withLock {
        val symbols = database.favoriteDao().getByInvestor(investorId).map { it.stockSymbol }
        if (symbols.isEmpty()) return@withLock readSnapshot()

        // No database transaction is held during the simulated network request.
        val quotes = quotesApi.getQuotes(symbols)
        require(quotes.size == symbols.size && quotes.map { it.symbol }.toSet() == symbols.toSet()) {
            "Quote response must contain exactly the requested favorites"
        }
        require(quotes.all { quote ->
            quote.priceInCents > 0 && quote.dailyChangePercent.isFinite() &&
                quote.history.isNotEmpty() && quote.history.all { it.isFinite() }
        }) { "Invalid quote response" }

        database.withTransaction {
            // A favorite may have been removed while the request was in flight.
            val currentFavorites = database.favoriteDao().getByInvestor(investorId).map { it.stockSymbol }.toSet()
            val stocks = database.stockDao().getAllWithHistory().associateBy { it.stock.symbol }
            quotes.filter { it.symbol in currentFavorites }.forEach { quote ->
                val stock = stocks[quote.symbol]?.stock ?: return@forEach
                database.stockDao().saveWithHistory(
                    stock.copy(priceInCents = quote.priceInCents, dailyChangePercent = quote.dailyChangePercent),
                    quote.history,
                )
            }
            // Return the persisted values so the UI clears its blur only after committing them.
            readSnapshot()
        }
    }

    private suspend fun readSnapshot(): HomeSnapshot = database.withTransaction {
        val investor = checkNotNull(database.investorDao().getById(investorId)) {
            "Investor $investorId is missing from the local database"
        }
        val portfolio = checkNotNull(database.portfolioDao().getWithHistory(investorId)) {
            "Portfolio for investor $investorId is missing from the local database"
        }
        val favorites = database.favoriteDao().getByInvestor(investorId)
            .map { favorite -> favorite.stockSymbol }.toSet()
        val stocks = database.stockDao().getAllWithHistory().map { stock ->
            stock.toStock(isFavorite = stock.stock.symbol in favorites)
        }
        HomeSnapshot(investor.toInvestor(), portfolio.toPortfolio(), stocks)
    }
}
