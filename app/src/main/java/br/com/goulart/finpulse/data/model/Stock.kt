package br.com.goulart.finpulse.data.model

data class Stock(
    val symbol: String,
    val company: String,
    val priceInCents: Long,
    val dailyChangePercent: Double,
    val history: List<Float>,
    val isFavorite: Boolean = false,
)
