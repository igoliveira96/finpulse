package br.com.goulart.finpulse.data.model

data class Portfolio(
    val balanceInCents: Long,
    val monthlyGainInCents: Long,
    val monthlyChangePercent: Double,
    val history: List<Float>,
)
