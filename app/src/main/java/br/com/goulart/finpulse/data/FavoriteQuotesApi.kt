package br.com.goulart.finpulse.data

import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

data class StockQuote(
    val symbol: String,
    val priceInCents: Long,
    val dailyChangePercent: Double,
    val history: List<Float>,
)

fun interface FavoriteQuotesApi {
    suspend fun getQuotes(symbols: List<String>): List<StockQuote>
}

/** A deterministic market simulation for the SWR demo, not a live quote service. */
class MockFavoriteQuotesApi(private val latencyMillis: Long = 1_200L) : FavoriteQuotesApi {
    private val requests = AtomicInteger()
    private val referenceStocks = MockMarketData.home.stocks.associateBy { it.symbol }
    private val movements = listOf(24L, -18L, 37L, 9L, -29L, 16L)

    override suspend fun getQuotes(symbols: List<String>): List<StockQuote> {
        delay(latencyMillis.milliseconds)
        val cycle = requests.getAndIncrement()
        return symbols.mapIndexed { index, symbol ->
            val reference = requireNotNull(referenceStocks[symbol]) { "Unknown simulated symbol: $symbol" }
            val movement = movements[Math.floorMod(cycle + index, movements.size)]
            val price = (reference.priceInCents + movement).coerceAtLeast(1)
            val previousClose = reference.priceInCents / (1 + reference.dailyChangePercent / 100)
            val variation = round((price / previousClose - 1) * 10_000) / 100
            val lastPoint = reference.history.last() * price.toFloat() / reference.priceInCents
            StockQuote(symbol, price, variation, reference.history.drop(1) + lastPoint)
        }
    }
}
