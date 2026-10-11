package br.com.goulart.finpulse.data.repository

import br.com.goulart.finpulse.data.datasource.HomeDataSource

class HomeRepositoryImpl(private val dataSource: HomeDataSource) : HomeRepository {
    override fun observeHome() = dataSource.observeHome()
    override suspend fun refreshFavorites() = dataSource.refreshFavorites()
    override suspend fun revalidateFavoritesWhileVisible() = dataSource.revalidateFavoritesWhileVisible()
}
