package br.com.goulart.finpulse

import androidx.lifecycle.viewModelScope
import br.com.goulart.finpulse.data.HomeRepository
import br.com.goulart.finpulse.data.HomeSnapshot
import br.com.goulart.finpulse.data.MockMarketData
import br.com.goulart.finpulse.ui.home.HomeUiState
import br.com.goulart.finpulse.ui.home.HomeViewModel
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private val viewModels = mutableListOf<HomeViewModel>()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: HomeRepository) = HomeViewModel(repository).also(viewModels::add)

    private fun repository(observe: () -> Flow<HomeSnapshot>) = object : HomeRepository {
        override fun observeHome() = observe()
        override suspend fun refreshFavorites() = observe().first()
    }

    @Test
    fun showsLoadingUntilTheRepositoryEmitsItsFirstSnapshot() = runTest(dispatcher) {
        val response = CompletableDeferred<HomeSnapshot>()
        val viewModel = viewModel(repository { flow { emit(response.await()) } })
        runCurrent()
        assertEquals(HomeUiState.Loading, viewModel.state.value)
        response.complete(MockMarketData.home)
        runCurrent()
        assertEquals(HomeUiState.Ready(MockMarketData.home), viewModel.state.value)
    }

    @Test
    fun aFailedDatabaseReadBecomesAnErrorState() = runTest(dispatcher) {
        val viewModel = viewModel(repository { flow { throw IOException("Unavailable") } })
        runCurrent()
        assertEquals(HomeUiState.Error, viewModel.state.value)
    }

    @Test
    fun cancellationIsNotPresentedAsADatabaseFailure() = runTest(dispatcher) {
        val viewModel = viewModel(repository { flow { throw CancellationException() } })
        runCurrent()
        assertEquals(HomeUiState.Loading, viewModel.state.value)
    }

    @Test
    fun repositoryChangesUpdateTheLoadedState() = runTest(dispatcher) {
        val snapshots = MutableStateFlow(MockMarketData.home)
        val viewModel = viewModel(repository { snapshots })
        runCurrent()
        val changed = MockMarketData.home.copy(
            portfolio = MockMarketData.home.portfolio.copy(balanceInCents = 42_000_000),
        )
        snapshots.value = changed
        runCurrent()
        assertEquals(HomeUiState.Ready(changed), viewModel.state.value)
    }

    @Test
    fun observingStateDoesNotCreateNewRepositorySubscriptions() = runTest(dispatcher) {
        var subscriptions = 0
        val viewModel = viewModel(repository {
            flow {
                subscriptions++
                emit(MockMarketData.home)
                awaitCancellation()
            }
        })
        runCurrent()
        repeat(3) { assertEquals(HomeUiState.Ready(MockMarketData.home), viewModel.state.value) }
        assertEquals(1, subscriptions)
    }

    @Test
    fun aFailureAfterLoadingDoesNotSilentlyKeepStaleData() = runTest(dispatcher) {
        val failure = CompletableDeferred<Unit>()
        val viewModel = viewModel(repository {
            flow {
                emit(MockMarketData.home)
                failure.await()
                throw IllegalStateException("Missing portfolio")
            }
        })
        runCurrent()
        assertEquals(HomeUiState.Ready(MockMarketData.home), viewModel.state.value)
        failure.complete(Unit)
        runCurrent()
        assertEquals(HomeUiState.Error, viewModel.state.value)
    }
}
