package br.com.goulart.finpulse

import android.app.Application
import br.com.goulart.finpulse.data.datasource.HomeDataSourceImpl
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import br.com.goulart.finpulse.data.local.RoomHomeLocalDataSource
import br.com.goulart.finpulse.data.remote.mock.MockFavoriteQuotesApi
import br.com.goulart.finpulse.data.repository.HomeRepositoryImpl
import br.com.goulart.finpulse.data.repository.HomeRepository
import br.com.goulart.finpulse.domain.usecase.ObserveHomeUseCase
import br.com.goulart.finpulse.domain.usecase.RevalidateFavoritesUseCase

class FinPulseApplication : Application() {
    // Shared dependencies for this process; Room opens on the first asynchronous query.
    val database: FinPulseDatabase by lazy { FinPulseDatabase.create(this) }
    val homeRepository: HomeRepository by lazy {
        HomeRepositoryImpl(
            HomeDataSourceImpl(RoomHomeLocalDataSource(database), MockFavoriteQuotesApi()),
        )
    }
    val observeHomeUseCase by lazy { ObserveHomeUseCase(homeRepository) }
    val revalidateFavoritesUseCase by lazy { RevalidateFavoritesUseCase(homeRepository) }
}
