package br.com.goulart.finpulse

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.remote.mock.MockMarketData
import br.com.goulart.finpulse.data.repository.RoomHomeRepository
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class RoomHomeRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: FinPulseDatabase
    private lateinit var name: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        name = "home-repository-${UUID.randomUUID()}.db"
        database = FinPulseDatabase.create(context, name)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun readsPersistedEditsInsteadOfReturningTheMock() = runBlocking {
        val investor = requireNotNull(database.investorDao().getById(1))
        database.investorDao().upsert(investor.copy(firstName = "Investidor local"))
        val portfolio = requireNotNull(database.portfolioDao().getWithHistory(1)).portfolio
        database.portfolioDao().saveWithHistory(portfolio.copy(balanceInCents = 42_000_000), listOf(8f, 2f, 9f))
        database.favoriteDao().remove(1, "VALE3")

        val actual = RoomHomeRepository(database).observeHome().first()
        assertEquals("Investidor local", actual.investor.firstName)
        assertEquals(42_000_000L, actual.portfolio.balanceInCents)
        assertEquals(listOf(8f, 2f, 9f), actual.portfolio.history)
        assertEquals(listOf("PETR4", "ITUB4"), actual.favorites.map { it.symbol })
        assertEquals(MockMarketData.home.stocks.map { it.symbol }, actual.stocks.map { it.symbol })
    }

    @Test
    fun observesChangesAcrossAllSixTablesWithConsistentHistories() = runBlocking {
        val snapshots = Channel<HomeSnapshot>(Channel.UNLIMITED)
        val collection = launch { RoomHomeRepository(database).observeHome().collect { snapshots.send(it) } }
        suspend fun awaitSnapshot(predicate: (HomeSnapshot) -> Boolean): HomeSnapshot = withTimeout(5_000) {
            var snapshot = snapshots.receive()
            while (!predicate(snapshot)) snapshot = snapshots.receive()
            snapshot
        }
        try {
            assertEquals(MockMarketData.home, awaitSnapshot { true })

            val investor = requireNotNull(database.investorDao().getById(1))
            database.investorDao().upsert(investor.copy(initials = "FP"))
            awaitSnapshot { it.investor.initials == "FP" }

            val portfolio = requireNotNull(database.portfolioDao().getWithHistory(1)).portfolio
            database.portfolioDao().saveWithHistory(portfolio.copy(balanceInCents = 123_456), listOf(1f, 5f))
            val changedPortfolio = awaitSnapshot { it.portfolio.balanceInCents == 123_456L }
            assertEquals(listOf(1f, 5f), changedPortfolio.portfolio.history)
            database.portfolioDao().saveWithHistory(portfolio.copy(balanceInCents = 123_456), listOf(9f, 7f))
            awaitSnapshot { it.portfolio.history == listOf(9f, 7f) }

            val stock = database.stockDao().getAllWithHistory().first().stock
            database.stockDao().saveWithHistory(stock.copy(priceInCents = 8_765), listOf(3f, 4f))
            val changedStock = awaitSnapshot { it.stocks.first().priceInCents == 8_765L }
            assertEquals(listOf(3f, 4f), changedStock.stocks.first().history)
            database.stockDao().saveWithHistory(stock.copy(priceInCents = 8_765), listOf(5f, 2f))
            awaitSnapshot { it.stocks.first().history == listOf(5f, 2f) }

            database.favoriteDao().remove(1, stock.symbol)
            assertEquals(2, awaitSnapshot { !it.stocks.first().isFavorite }.favorites.size)
        } finally {
            collection.cancel()
            collection.join()
            snapshots.close()
        }
    }

    @Test
    fun missingRequiredDataFailsWithoutFallingBackToTheMock() = runBlocking {
        database.investorDao().deleteById(1)
        try {
            RoomHomeRepository(database).observeHome().first()
            fail("A missing investor must fail instead of returning mock data")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains("Investor 1"))
        }
    }

    @Test
    fun anEmptyMarketRemainsEmptyInsteadOfUsingMockStocks() = runBlocking {
        database.stockDao().getAllWithHistory().forEach { database.stockDao().deleteBySymbol(it.stock.symbol) }
        val snapshot = RoomHomeRepository(database).observeHome().first()
        assertTrue(snapshot.stocks.isEmpty())
        assertTrue(snapshot.favorites.isEmpty())
        assertEquals(MockMarketData.home.portfolio, snapshot.portfolio)
    }
}
