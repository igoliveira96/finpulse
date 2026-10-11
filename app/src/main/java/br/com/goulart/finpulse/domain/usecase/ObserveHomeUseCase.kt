package br.com.goulart.finpulse.domain.usecase

import br.com.goulart.finpulse.data.repository.HomeRepository

class ObserveHomeUseCase(private val repository: HomeRepository) {
    operator fun invoke() = repository.observeHome()
}
