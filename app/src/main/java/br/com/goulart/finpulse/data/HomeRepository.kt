package br.com.goulart.finpulse.data

import kotlinx.coroutines.flow.Flow

fun interface HomeRepository {
    fun observeHome(): Flow<HomeSnapshot>
}
