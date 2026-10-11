package br.com.goulart.finpulse.data.remote

import br.com.goulart.finpulse.data.remote.model.StockQuote

fun interface FavoriteQuotesApi {
    suspend fun getQuotes(symbols: List<String>): List<StockQuote>
}
