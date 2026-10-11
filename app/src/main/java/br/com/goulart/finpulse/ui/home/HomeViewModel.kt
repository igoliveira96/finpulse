package br.com.goulart.finpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.goulart.finpulse.FinPulseApplication
import br.com.goulart.finpulse.data.HomeRepository
import br.com.goulart.finpulse.data.HomeSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val data: HomeSnapshot) : HomeUiState
    data object Error : HomeUiState
}

class HomeViewModel(repository: HomeRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.observeHome().collect { snapshot ->
                    mutableState.value = HomeUiState.Ready(snapshot)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = HomeUiState.Error
            }
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
