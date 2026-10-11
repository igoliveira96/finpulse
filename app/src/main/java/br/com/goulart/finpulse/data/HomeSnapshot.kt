package br.com.goulart.finpulse.data

data class HomeSnapshot(
    val investor: Investor,
    val portfolio: Portfolio,
    val stocks: List<Stock>,
) {
    val favorites: List<Stock> get() = stocks.filter(Stock::isFavorite)
}
