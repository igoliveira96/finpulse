package br.com.goulart.finpulse

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import br.com.goulart.finpulse.data.remote.FavoriteQuotesApi
import br.com.goulart.finpulse.data.remote.mock.MockMarketData
import br.com.goulart.finpulse.data.remote.model.StockQuote
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class FavoritesRevalidationTest {
    private lateinit var context: Context
    private lateinit var database: FinPulseDatabase
    private lateinit var name: String

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        name = "favorite-quotes-${UUID.randomUUID()}.db"
        database = FinPulseDatabase.create(context, name)
    }
    @After fun tearDown() { database.close(); context.deleteDatabase(name) }

    private fun quotes(symbols: List<String>) = symbols.map { symbol ->
        val stock = MockMarketData.home.stocks.first { it.symbol == symbol }
        StockQuote(symbol, stock.priceInCents + 100, 3.5, listOf(1f, 2f, 3f))
    }

    @Test
    fun requestsAndPersistsOnlyFavoriteQuotes() = runBlocking {
        var requested = emptyList<String>()
        val repository = homeRepository(database, quotesApi = FavoriteQuotesApi {
            requested = it
            quotes(it)
        })
        val before = repository.observeSnapshots().first()
        val updated = repository.refreshFavorites()
        assertEquals(before.favorites.map { it.symbol }.toSet(), requested.toSet())
        assertEquals(before.portfolio, updated.portfolio)
        assertEquals(before.investor, updated.investor)
        assertEquals(before.stocks.filterNot { it.isFavorite }, updated.stocks.filterNot { it.isFavorite })
        assertEquals(before.favorites.map { it.priceInCents + 100 }, updated.favorites.map { it.priceInCents })
        assertEquals(listOf(1f, 2f, 3f), updated.favorites.first().history)
        database.close()
        database = FinPulseDatabase.create(context, name)
        assertEquals(updated, homeRepository(database).observeSnapshots().first())
    }

    @Test
    fun failurePreservesAllCachedValues() = runBlocking {
        val repository = homeRepository(database, quotesApi = FavoriteQuotesApi { throw IOException("Offline") })
        val before = repository.observeSnapshots().first()
        try { repository.refreshFavorites(); fail("Expected request failure") } catch (_: IOException) { }
        assertEquals(before, repository.observeSnapshots().first())
    }

    @Test
    fun aPartialResponseCannotPartiallyOverwriteTheCache() = runBlocking {
        val repository = homeRepository(database, quotesApi = FavoriteQuotesApi { quotes(it).dropLast(1) })
        val before = repository.observeSnapshots().first()
        try { repository.refreshFavorites(); fail("Expected response validation failure") } catch (_: IllegalArgumentException) { }
        assertEquals(before, repository.observeSnapshots().first())
    }

    @Test
    fun aFavoriteRemovedDuringTheRequestIsNotUpdatedOrRestored() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val respond = CompletableDeferred<Unit>()
        val repository = homeRepository(database, quotesApi = FavoriteQuotesApi {
            started.complete(Unit)
            respond.await()
            quotes(it)
        })
        val before = repository.observeSnapshots().first()
        val request = async { repository.refreshFavorites() }
        started.await()
        database.favoriteDao().remove(1, "PETR4")
        respond.complete(Unit)
        val updated = request.await()
        assertFalse(updated.stocks.first { it.symbol == "PETR4" }.isFavorite)
        assertEquals(before.stocks.first { it.symbol == "PETR4" }.priceInCents,
            updated.stocks.first { it.symbol == "PETR4" }.priceInCents)
        assertEquals(2, updated.favorites.size)
    }
}
