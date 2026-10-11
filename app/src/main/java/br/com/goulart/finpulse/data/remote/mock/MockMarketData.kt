package br.com.goulart.finpulse.data.remote.mock

import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.model.Investor
import br.com.goulart.finpulse.data.model.Portfolio
import br.com.goulart.finpulse.data.model.Stock

/** Deterministic demo quotes, shared with previews; these are not live market data. */
object MockMarketData {
    val home = HomeSnapshot(
        investor = Investor("Igor", "IG"),
        portfolio = Portfolio(
            balanceInCents = 35_000_000,
            monthlyGainInCents = 842_000,
            monthlyChangePercent = 2.46,
            history = listOf(18f, 21f, 20f, 24f, 29f, 28f, 32f, 30f, 35f, 34f, 39f,
                42f, 47f, 43f, 40f, 45f, 48f, 51f, 56f, 54f, 60f, 63f, 61f, 68f, 70f, 78f),
        ),
        stocks = listOf(
            Stock("PETR4", "Petrobras", 3_842, 1.24,
                listOf(18f, 23f, 20f, 28f, 25f, 30f, 26f, 32f, 36f, 34f, 40f), true),
            Stock("VALE3", "Vale", 6_280, -0.85,
                listOf(40f, 35f, 38f, 27f, 31f, 24f, 28f, 26f, 30f, 25f, 29f), true),
            Stock("ITUB4", "Itaú Unibanco", 3_416, 0.62,
                listOf(20f, 25f, 23f, 29f, 26f, 32f, 30f, 36f, 35f, 39f, 42f), true),
            Stock("WEGE3", "WEG", 5_230, 2.18,
                listOf(14f, 21f, 18f, 26f, 24f, 31f, 29f, 35f, 32f, 40f, 46f)),
            Stock("BBAS3", "Banco do Brasil", 2_745, 0.94,
                listOf(18f, 24f, 22f, 29f, 26f, 31f, 30f, 35f, 33f, 39f, 41f)),
            Stock("BBDC4", "Bradesco", 1_482, -0.47,
                listOf(45f, 38f, 42f, 31f, 34f, 29f, 32f, 23f, 27f, 21f, 18f)),
        ),
    )
}
