package br.com.goulart.finpulse.data.datasource

import br.com.goulart.finpulse.data.local.HomeLocalDataSource
import br.com.goulart.finpulse.data.model.FavoritesRefreshStatus
import br.com.goulart.finpulse.data.model.HomeData
import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.remote.FavoriteQuotesApi
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class HomeDataSourceImpl(
    private val local: HomeLocalDataSource,
    private val remote: FavoriteQuotesApi,
) : HomeDataSource {
    private val home = MutableStateFlow<HomeData?>(null)
    private val pollingMutex = Mutex()
    private val refreshMutex = Mutex()

    override fun observeHome() = channelFlow {
        launch {
            local.observeHome().collect { snapshot ->
                home.update { current ->
                    current?.copy(snapshot = snapshot) ?: HomeData(snapshot, FavoritesRefreshStatus.Idle)
                }
            }
        }
        home.filterNotNull().collect { send(it) }
    }

    override suspend fun refreshFavorites(): HomeSnapshot = refreshMutex.withLock {
        val symbols = local.favoriteSymbols()
        if (symbols.isEmpty()) return@withLock local.readHome()
        setRefreshStatus(FavoritesRefreshStatus.Refreshing)
        try {
            val refreshed = withTimeout(4.seconds) {
                // No database transaction is held during the remote request.
                val quotes = remote.getQuotes(symbols)
                require(quotes.size == symbols.size && quotes.map { it.symbol }.toSet() == symbols.toSet()) {
                    "Quote response must contain exactly the requested favorites"
                }
                require(quotes.all { quote ->
                    quote.priceInCents > 0 && quote.dailyChangePercent.isFinite() &&
                        quote.history.isNotEmpty() && quote.history.all { it.isFinite() }
                }) { "Invalid quote response" }
                local.saveFavoriteQuotes(quotes)
            }
            // Reveal the values only after the local transaction has committed.
            home.value = HomeData(refreshed, FavoritesRefreshStatus.Updated)
            refreshed
        } catch (timeout: TimeoutCancellationException) {
            setRefreshStatus(FavoritesRefreshStatus.Failed)
            throw timeout
        } catch (cancelled: CancellationException) {
            setRefreshStatus(FavoritesRefreshStatus.Idle)
            throw cancelled
        } catch (error: Exception) {
            setRefreshStatus(FavoritesRefreshStatus.Failed)
            throw error
        }
    }

    /** The caller's visible lifecycle owns the loop and any in-flight request. */
    override suspend fun revalidateFavoritesWhileVisible() = pollingMutex.withLock {
        home.filterNotNull().first()
        flow {
            while (currentCoroutineContext().isActive) {
                emit(Unit)
                delay(5.seconds)
            }
        }.conflate().collect {
            try {
                refreshFavorites()
            } catch (_: TimeoutCancellationException) {
                // Keep the local cache and retry at the next tick.
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the local cache and retry at the next tick.
            }
        }
    }

    private fun setRefreshStatus(status: FavoritesRefreshStatus) {
        home.update { it?.copy(favoritesRefresh = status) }
    }
}
