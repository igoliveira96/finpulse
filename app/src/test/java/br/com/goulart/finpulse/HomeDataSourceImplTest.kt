package br.com.goulart.finpulse

import br.com.goulart.finpulse.data.datasource.HomeDataSourceImpl
import br.com.goulart.finpulse.data.local.HomeLocalDataSource
import br.com.goulart.finpulse.data.model.FavoritesRefreshStatus
import br.com.goulart.finpulse.data.model.HomeData
import br.com.goulart.finpulse.data.remote.FavoriteQuotesApi
import br.com.goulart.finpulse.data.remote.mock.MockMarketData
import br.com.goulart.finpulse.data.remote.model.StockQuote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDataSourceImplTest {
    private val cached = MockMarketData.home

    private fun quotes(symbols: List<String>) = cached.stocks.filter { it.symbol in symbols }.map {
        StockQuote(it.symbol, it.priceInCents + 100, it.dailyChangePercent, it.history)
    }

    @Test
    fun waitsForTheObservedLocalCacheBeforeRevalidating() = runTest {
        val local = InMemoryHomeLocalDataSource(cached)
        var calls = 0
        val source = HomeDataSourceImpl(local, FavoriteQuotesApi {
            calls++
            delay(1_200)
            quotes(it)
        })
        backgroundScope.launch { source.revalidateFavoritesWhileVisible() }
        runCurrent()
        assertEquals(0, calls)

        val states = mutableListOf<HomeData>()
        backgroundScope.launch { source.observeHome().collect { states += it } }
        runCurrent()
        assertEquals(1, calls)
        assertEquals(cached, states.last().snapshot)
        assertEquals(FavoritesRefreshStatus.Refreshing, states.last().favoritesRefresh)
    }

    @Test
    fun resumingVisibilityRevalidatesImmediatelyWithoutAddingLocalSubscriptions() = runTest {
        val local = InMemoryHomeLocalDataSource(cached)
        var subscriptions = 0
        val countedLocal = object : HomeLocalDataSource by local {
            override fun observeHome() = flow {
                subscriptions++
                local.observeHome().collect { emit(it) }
            }
        }
        val starts = mutableListOf<Long>()
        val source = HomeDataSourceImpl(countedLocal, FavoriteQuotesApi {
            starts += currentTime
            delay(1_200)
            quotes(it)
        })
        backgroundScope.launch { source.observeHome().collect {} }
        val firstVisibility = backgroundScope.launch { source.revalidateFavoritesWhileVisible() }
        runCurrent()
        firstVisibility.cancelAndJoin()
        advanceTimeBy(15_000)
        runCurrent()
        backgroundScope.launch { source.revalidateFavoritesWhileVisible() }
        runCurrent()

        assertEquals(listOf(0L, 15_000L), starts)
        assertEquals(1, subscriptions)
        assertEquals(cached, local.readHome())
    }

    @Test
    fun concurrentVisibilityCallsShareOnePollingLoopAndNeverOverlapRequests() = runTest {
        val local = InMemoryHomeLocalDataSource(cached)
        var active = 0
        var maximumActive = 0
        val starts = mutableListOf<Long>()
        val source = HomeDataSourceImpl(local, FavoriteQuotesApi {
            starts += currentTime
            active++
            maximumActive = maxOf(maximumActive, active)
            try {
                delay(1_200)
                quotes(it)
            } finally {
                active--
            }
        })
        backgroundScope.launch { source.observeHome().collect {} }
        val firstVisibility = backgroundScope.launch { source.revalidateFavoritesWhileVisible() }
        backgroundScope.launch { source.revalidateFavoritesWhileVisible() }
        runCurrent()
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(listOf(0L, 5_000L), starts)
        assertEquals(1, maximumActive)

        firstVisibility.cancelAndJoin()
        runCurrent()
        assertEquals(listOf(0L, 5_000L, 6_000L), starts)
        assertEquals(1, maximumActive)
    }

    @Test
    fun invalidRemoteResponsesKeepTheCompleteLocalCache() = runTest {
        val valid = quotes(cached.favorites.map { it.symbol })
        val responses = listOf(
            valid.dropLast(1),
            listOf(valid.first(), valid.first(), valid.last()),
            valid.map { it.copy(priceInCents = 0) },
            valid.map { it.copy(dailyChangePercent = Double.NaN) },
            valid.map { it.copy(history = emptyList()) },
            valid.map { it.copy(history = listOf(Float.NaN)) },
        )
        for (response in responses) {
            val local = InMemoryHomeLocalDataSource(cached)
            val source = HomeDataSourceImpl(local, FavoriteQuotesApi { response })
            val states = mutableListOf<HomeData>()
            backgroundScope.launch { source.observeHome().collect { states += it } }
            runCurrent()
            val failure = runCatching { source.refreshFavorites() }.exceptionOrNull()
            runCurrent()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(cached, local.readHome())
            assertEquals(cached, states.last().snapshot)
            assertEquals(FavoritesRefreshStatus.Failed, states.last().favoritesRefresh)
        }
    }
}
