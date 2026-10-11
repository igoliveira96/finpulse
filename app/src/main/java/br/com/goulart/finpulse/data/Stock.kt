package br.com.goulart.finpulse.data

data class Stock(
    val symbol: String,
    val company: String,
    val priceInCents: Long,
    val dailyChangePercent: Double,
    val history: List<Float>,
    val isFavorite: Boolean = false,
)
