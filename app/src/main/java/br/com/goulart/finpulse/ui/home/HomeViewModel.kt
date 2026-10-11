package br.com.goulart.finpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.goulart.finpulse.FinPulseApplication
import br.com.goulart.finpulse.domain.usecase.ObserveHomeUseCase
import br.com.goulart.finpulse.domain.usecase.RevalidateFavoritesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observeHome: ObserveHomeUseCase,
    private val revalidateFavorites: RevalidateFavoritesUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                observeHome().collect { home ->
                    mutableState.value = HomeUiState.Ready(home.snapshot, home.favoritesRefresh)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = HomeUiState.Error
            }
        }
    }

    /** Owned by the visible Home's lifecycle; cancellation stops both polling and requests. */
    suspend fun revalidateFavoritesWhileVisible() {
        state.first { it is HomeUiState.Ready }
        revalidateFavorites()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY]) as FinPulseApplication
                HomeViewModel(application.observeHomeUseCase, application.revalidateFavoritesUseCase)
            }
        }
    }
}
