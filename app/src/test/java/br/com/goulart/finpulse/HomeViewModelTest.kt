package br.com.goulart.finpulse

import br.com.goulart.finpulse.data.FinPulseApi
import br.com.goulart.finpulse.data.MockFinPulseApi
import br.com.goulart.finpulse.data.MockMarketData
import br.com.goulart.finpulse.ui.home.HomeUiState
import br.com.goulart.finpulse.ui.home.HomeViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun showsLoadingUntilTheSimulatedApiResponds() = runTest(dispatcher) {
        val viewModel = HomeViewModel(MockFinPulseApi(latencyMillis = 1_200))
        assertEquals(HomeUiState.Loading, viewModel.state.value)
        advanceTimeBy(1_199)
        assertEquals(HomeUiState.Loading, viewModel.state.value)
        advanceTimeBy(1)
        runCurrent()
        val result = (viewModel.state.value as HomeUiState.Ready).data
        assertEquals(35_000_000L, result.portfolio.balanceInCents)
        assertEquals(listOf("PETR4", "VALE3", "ITUB4"), result.favorites.map { it.symbol })
        assertEquals(6, result.stocks.size)
    }

    @Test
    fun aFailedRequestBecomesAnErrorState() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FinPulseApi { throw IOException("Unavailable") })
        advanceUntilIdle()
        assertEquals(HomeUiState.Error, viewModel.state.value)
    }

    @Test
    fun cancellationIsNotPresentedAsANetworkFailure() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FinPulseApi { throw CancellationException() })
        advanceUntilIdle()
        assertEquals(HomeUiState.Loading, viewModel.state.value)
    }

    @Test
    fun responseIsRequestedOnlyOncePerViewModel() = runTest(dispatcher) {
        var requests = 0
        val viewModel = HomeViewModel(FinPulseApi { requests++; MockMarketData.home })
        advanceUntilIdle()
        repeat(3) { assertEquals(HomeUiState.Ready(MockMarketData.home), viewModel.state.value) }
        assertEquals(1, requests)
    }
}
