package br.com.goulart.finpulse

import androidx.lifecycle.viewModelScope
import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.remote.mock.MockMarketData
import br.com.goulart.finpulse.data.repository.HomeRepository
import br.com.goulart.finpulse.ui.home.FavoritesRefreshStatus
import br.com.goulart.finpulse.ui.home.HomeUiState
import br.com.goulart.finpulse.ui.home.HomeViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesSwrTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<HomeViewModel>()
    private val cached = MockMarketData.home
    private val fresh = cached.copy(stocks = cached.stocks.map {
        if (it.isFavorite) it.copy(priceInCents = it.priceInCents + 100) else it
    })

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun model(initial: HomeSnapshot = cached, refresh: suspend () -> HomeSnapshot): HomeViewModel {
        val snapshots = MutableStateFlow(initial)
        val repository = object : HomeRepository {
            override fun observeHome() = snapshots
            override suspend fun refreshFavorites() = refresh().also { snapshots.value = it }
        }
        return HomeViewModel(repository).also(models::add)
    }

    private fun HomeViewModel.ready() = state.value as HomeUiState.Ready

    @Test
    fun keepsCachedValuesDuringRefreshAndRevealsTheRevalidatedValues() = runTest(dispatcher) {
        val vm = model { delay(1_200); fresh }
        runCurrent()
        assertEquals(cached, vm.ready().data)
        backgroundScope.launch { vm.revalidateFavoritesWhileVisible() }
        runCurrent()
        assertEquals(FavoritesRefreshStatus.Refreshing, vm.ready().favoritesRefresh)
        assertEquals(cached, vm.ready().data)
        advanceTimeBy(1_199)
        runCurrent()
        assertEquals(cached, vm.ready().data)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(fresh, vm.ready().data)
        assertEquals(FavoritesRefreshStatus.Updated, vm.ready().favoritesRefresh)
    }

    @Test
    fun startsRequestsEveryFiveSecondsRatherThanFiveSecondsAfterEachResponse() = runTest(dispatcher) {
        val starts = mutableListOf<Long>()
        val vm = model { starts += currentTime; delay(1_200); fresh }
        backgroundScope.launch { vm.revalidateFavoritesWhileVisible() }
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(listOf(0L, 5_000L, 10_000L), starts)
    }

    @Test
    fun failureKeepsTheCacheAndTheNextTickRetries() = runTest(dispatcher) {
        var calls = 0
        val vm = model {
            calls++
            delay(1_200)
            if (calls == 1) throw IOException("Offline")
            fresh
        }
        backgroundScope.launch { vm.revalidateFavoritesWhileVisible() }
        runCurrent()
        advanceTimeBy(1_200)
        runCurrent()
        assertEquals(cached, vm.ready().data)
        assertEquals(FavoritesRefreshStatus.Failed, vm.ready().favoritesRefresh)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(2, calls)
        assertEquals(fresh, vm.ready().data)
        assertEquals(FavoritesRefreshStatus.Updated, vm.ready().favoritesRefresh)
    }

    @Test
    fun leavingHomeCancelsRequestsAndRemovesTheRefreshingState() = runTest(dispatcher) {
        var calls = 0
        val vm = model { calls++; delay(1_200); fresh }
        val polling = backgroundScope.launch { vm.revalidateFavoritesWhileVisible() }
        runCurrent()
        assertEquals(FavoritesRefreshStatus.Refreshing, vm.ready().favoritesRefresh)
        polling.cancelAndJoin()
        advanceTimeBy(15_000)
        runCurrent()
        assertEquals(1, calls)
        assertEquals(cached, vm.ready().data)
        assertEquals(FavoritesRefreshStatus.Idle, vm.ready().favoritesRefresh)
    }

    @Test
    fun slowRequestsTimeOutWithoutOverlappingAndRetryOnSchedule() = runTest(dispatcher) {
        var active = 0
        var maxActive = 0
        var calls = 0
        val vm = model {
            calls++
            active++
            maxActive = maxOf(maxActive, active)
            try { delay(10_000); fresh } finally { active-- }
        }
        backgroundScope.launch { vm.revalidateFavoritesWhileVisible() }
        runCurrent()
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(FavoritesRefreshStatus.Failed, vm.ready().favoritesRefresh)
        assertEquals(cached, vm.ready().data)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, calls)
        assertEquals(1, maxActive)
    }

    @Test
    fun doesNotRequestQuotesWhenThereAreNoFavorites() = runTest(dispatcher) {
        val withoutFavorites = cached.copy(stocks = cached.stocks.map { it.copy(isFavorite = false) })
        var calls = 0
        val vm = model(withoutFavorites) { calls++; fresh }
        backgroundScope.launch { vm.revalidateFavoritesWhileVisible() }
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0, calls)
        assertEquals(withoutFavorites, vm.ready().data)
    }
}
