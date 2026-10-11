package br.com.goulart.finpulse.data.model

data class HomeSnapshot(
    val investor: Investor,
    val portfolio: Portfolio,
    val stocks: List<Stock>,
) {
    val favorites: List<Stock> get() = stocks.filter(Stock::isFavorite)
}
