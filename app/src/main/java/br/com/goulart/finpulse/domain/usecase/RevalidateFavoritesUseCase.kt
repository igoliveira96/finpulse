package br.com.goulart.finpulse.domain.usecase

import br.com.goulart.finpulse.data.repository.HomeRepository

class RevalidateFavoritesUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke() = repository.revalidateFavoritesWhileVisible()
}
