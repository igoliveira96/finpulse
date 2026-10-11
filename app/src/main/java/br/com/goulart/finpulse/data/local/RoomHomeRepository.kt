package br.com.goulart.finpulse.data.local

import androidx.room.withTransaction
import br.com.goulart.finpulse.data.HomeRepository
import br.com.goulart.finpulse.data.HomeSnapshot
import br.com.goulart.finpulse.data.local.model.toInvestor
import br.com.goulart.finpulse.data.local.model.toPortfolio
import br.com.goulart.finpulse.data.local.model.toStock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomHomeRepository(
    private val database: FinPulseDatabase,
    // The local seed contains a single investor; a future session can supply another id.
    private val investorId: Long = 1L,
) : HomeRepository {
    override fun observeHome(): Flow<HomeSnapshot> = database.invalidationTracker.createFlow(
        "investors", "portfolios", "portfolio_history", "stocks", "stock_history", "favorites",
        emitInitialState = true,
    ).map {
        // Read all sections from one consistent snapshot, including favorites and chart samples.
        database.withTransaction {
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
    }.distinctUntilChanged()
}
