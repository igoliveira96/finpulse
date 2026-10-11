package br.com.goulart.finpulse

import br.com.goulart.finpulse.data.datasource.HomeDataSourceImpl
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import br.com.goulart.finpulse.data.local.RoomHomeLocalDataSource
import br.com.goulart.finpulse.data.remote.FavoriteQuotesApi
import br.com.goulart.finpulse.data.remote.mock.MockFavoriteQuotesApi
import br.com.goulart.finpulse.data.repository.HomeRepositoryImpl
import br.com.goulart.finpulse.data.repository.HomeRepository
import br.com.goulart.finpulse.domain.usecase.ObserveHomeUseCase
import br.com.goulart.finpulse.domain.usecase.RevalidateFavoritesUseCase
import br.com.goulart.finpulse.ui.home.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun homeRepository(
    database: FinPulseDatabase,
    quotesApi: FavoriteQuotesApi = MockFavoriteQuotesApi(),
): HomeRepository = HomeRepositoryImpl(HomeDataSourceImpl(RoomHomeLocalDataSource(database), quotesApi))

internal fun homeViewModel(repository: HomeRepository) =
    HomeViewModel(ObserveHomeUseCase(repository), RevalidateFavoritesUseCase(repository))

internal fun HomeRepository.observeSnapshots() = observeHome().map { it.snapshot }.distinctUntilChanged()
