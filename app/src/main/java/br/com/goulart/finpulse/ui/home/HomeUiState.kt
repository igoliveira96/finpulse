package br.com.goulart.finpulse.ui.home

import br.com.goulart.finpulse.data.model.FavoritesRefreshStatus
import br.com.goulart.finpulse.data.model.HomeSnapshot

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val data: HomeSnapshot,
        val favoritesRefresh: FavoritesRefreshStatus = FavoritesRefreshStatus.Idle,
    ) : HomeUiState
    data object Error : HomeUiState
}
