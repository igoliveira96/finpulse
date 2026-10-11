package br.com.goulart.finpulse.data

data class Portfolio(
    val balanceInCents: Long,
    val monthlyGainInCents: Long,
    val monthlyChangePercent: Double,
    val history: List<Float>,
)
