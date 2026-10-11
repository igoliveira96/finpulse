package br.com.goulart.finpulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.goulart.finpulse.data.FinPulseApi
import br.com.goulart.finpulse.data.HomeSnapshot
import br.com.goulart.finpulse.data.MockFinPulseApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val data: HomeSnapshot) : HomeUiState
    data object Error : HomeUiState
}

class HomeViewModel(api: FinPulseApi) : ViewModel() {
    private val mutableState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                mutableState.value = HomeUiState.Ready(api.getHome())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = HomeUiState.Error
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(HomeViewModel::class.java))
                return HomeViewModel(MockFinPulseApi()) as T
            }
        }
    }
}
