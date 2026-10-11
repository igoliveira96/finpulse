package br.com.goulart.finpulse

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import br.com.goulart.finpulse.data.local.entity.FavoriteEntity
import br.com.goulart.finpulse.data.local.model.toInvestor
import br.com.goulart.finpulse.data.local.model.toPortfolio
import br.com.goulart.finpulse.data.local.model.toStock
import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.remote.mock.MockMarketData
import java.util.UUID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class FinPulseDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: FinPulseDatabase
    private lateinit var databaseName: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "finpulse-test-${UUID.randomUUID()}.db"
        database = FinPulseDatabase.create(context, databaseName)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun firstOpenImportsTheCompleteMockAndValidatesTheRoomSchema() = runBlocking {
        val investor = requireNotNull(database.investorDao().getById(1))
        val portfolio = requireNotNull(database.portfolioDao().getWithHistory(1))
        val favorites = database.favoriteDao().getByInvestor(1).map { it.stockSymbol }.toSet()
        val stocks = database.stockDao().getAllWithHistory()
            .map { it.toStock(it.stock.symbol in favorites) }
        assertEquals(MockMarketData.home, HomeSnapshot(investor.toInvestor(), portfolio.toPortfolio(), stocks))
        assertEquals(MockMarketData.home.favorites,
            database.stockDao().getFavoritesWithHistory(1).map { it.toStock(isFavorite = true) })
        assertEquals(1, database.openHelper.readableDatabase.version)
    }

    @Test
    fun reopeningPreservesEditsWithoutReimportingTheSeed() = runBlocking {
        val original = requireNotNull(database.investorDao().getById(1))
        database.investorDao().upsert(original.copy(firstName = "Nome local"))
        database.favoriteDao().remove(1, "PETR4")
        database.close()
        database = FinPulseDatabase.create(context, databaseName)

        assertEquals("Nome local", database.investorDao().getById(1)?.firstName)
        assertEquals(2, database.favoriteDao().getByInvestor(1).size)
        assertEquals(6, database.stockDao().getAllWithHistory().size)
        assertEquals(26, database.portfolioDao().getWithHistory(1)?.history?.size)
        // Local edits must not mutate the independent mock fixture.
        assertEquals(original.toInvestor(), MockMarketData.home.investor)
        assertTrue(MockMarketData.home.favorites.any { it.symbol == "PETR4" })
    }

    @Test
    fun favoritesAreUniqueAndCannotReferenceMissingStocks() = runBlocking {
        database.favoriteDao().add(FavoriteEntity(1, "PETR4"))
        database.favoriteDao().add(FavoriteEntity(1, "PETR4"))
        assertEquals(3, database.favoriteDao().getByInvestor(1).size)
        try {
            database.favoriteDao().add(FavoriteEntity(1, "MISSING"))
            fail("Foreign key must reject an unknown stock")
        } catch (_: SQLiteConstraintException) {
            assertEquals(3, database.favoriteDao().getByInvestor(1).size)
        }
    }

    @Test
    fun updatingAStockReplacesItsHistoryAndPreservesItsFavorite() = runBlocking {
        val stock = database.stockDao().getAllWithHistory().first().stock
        database.stockDao().saveWithHistory(stock.copy(priceInCents = 4_000), listOf(3f, 1f, 2f))
        val updated = database.stockDao().getAllWithHistory().first().toStock(isFavorite = true)
        assertEquals(4_000L, updated.priceInCents)
        assertEquals(listOf(3f, 1f, 2f), updated.history)
        assertTrue(database.favoriteDao().getByInvestor(1).any { it.stockSymbol == stock.symbol })
    }

    @Test
    fun deletingAStockCascadesToHistoryAndFavorites() = runBlocking {
        database.stockDao().deleteBySymbol("PETR4")
        assertEquals(5, database.stockDao().getAllWithHistory().size)
        assertFalse(database.favoriteDao().getByInvestor(1).any { it.stockSymbol == "PETR4" })
        database.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM stock_history WHERE stock_symbol = 'PETR4'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun deletingAnInvestorCascadesToPortfolioHistoryAndFavorites() = runBlocking {
        database.investorDao().deleteById(1)
        assertNull(database.investorDao().getById(1))
        assertNull(database.portfolioDao().getWithHistory(1))
        assertTrue(database.favoriteDao().getByInvestor(1).isEmpty())
        assertEquals(6, database.stockDao().getAllWithHistory().size)
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM portfolio_history").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun observersReceivePersistedQuoteUpdates() = runBlocking {
        val stock = database.stockDao().getAllWithHistory().first().stock
        withTimeout(5_000) {
            val update = async(start = CoroutineStart.UNDISPATCHED) {
                database.stockDao().observeAllWithHistory().first { stocks ->
                    stocks.any { it.stock.symbol == stock.symbol && it.stock.priceInCents == 9_999L }
                }
            }
            database.stockDao().upsert(stock.copy(priceInCents = 9_999))
            assertEquals(9_999L, update.await().first().stock.priceInCents)
        }
    }
}
