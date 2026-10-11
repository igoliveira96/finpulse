package br.com.goulart.finpulse.data.remote.model

data class StockQuote(
    val symbol: String,
    val priceInCents: Long,
    val dailyChangePercent: Double,
    val history: List<Float>,
)
