package br.com.goulart.finpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.goulart.finpulse.FinPulseApplication
import br.com.goulart.finpulse.data.HomeRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state = mutableState.asStateFlow()
    private val pollingMutex = Mutex()

    init {
        viewModelScope.launch {
            try {
                repository.observeHome().collect { snapshot ->
                    mutableState.update { current ->
                        (current as? HomeUiState.Ready)?.copy(data = snapshot) ?: HomeUiState.Ready(snapshot)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = HomeUiState.Error
            }
        }
    }

    /** Owned by the visible Home's lifecycle; cancellation stops both polling and requests. */
    suspend fun revalidateFavoritesWhileVisible() = pollingMutex.withLock {
        state.first { it is HomeUiState.Ready }
        flow {
            while (currentCoroutineContext().isActive) {
                emit(Unit)
                delay(5.seconds)
            }
        }.conflate().collect { refreshFavorites() }
    }

    private suspend fun refreshFavorites() {
        val cached = mutableState.value as? HomeUiState.Ready ?: return
        if (cached.data.favorites.isEmpty()) return
        setRefreshStatus(FavoritesRefreshStatus.Refreshing)
        try {
            val refreshed = withTimeout(4.seconds) { repository.refreshFavorites() }
            mutableState.update { current ->
                if (current is HomeUiState.Ready) {
                    current.copy(data = refreshed, favoritesRefresh = FavoritesRefreshStatus.Updated)
                } else current
            }
        } catch (_: TimeoutCancellationException) {
            setRefreshStatus(FavoritesRefreshStatus.Failed)
        } catch (cancelled: CancellationException) {
            setRefreshStatus(FavoritesRefreshStatus.Idle)
            throw cancelled
        } catch (_: Exception) {
            // SWR keeps the last local data usable and retries on the next tick.
            setRefreshStatus(FavoritesRefreshStatus.Failed)
        }
    }

    private fun setRefreshStatus(status: FavoritesRefreshStatus) {
        mutableState.update { current ->
            (current as? HomeUiState.Ready)?.copy(favoritesRefresh = status) ?: current
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY]) as FinPulseApplication
                HomeViewModel(application.homeRepository)
            }
        }
    }
}
