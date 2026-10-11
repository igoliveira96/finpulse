package br.com.goulart.finpulse

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import br.com.goulart.finpulse.data.remote.FavoriteQuotesApi
import br.com.goulart.finpulse.data.remote.mock.MockMarketData
import br.com.goulart.finpulse.data.remote.model.StockQuote
import br.com.goulart.finpulse.ui.home.HomeRoute
import br.com.goulart.finpulse.ui.home.HomeViewModel
import br.com.goulart.finpulse.ui.theme.FinPulseTheme
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeSwrIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private lateinit var database: FinPulseDatabase
    private lateinit var name: String
    private var viewModel: HomeViewModel? = null

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        name = "swr-ui-${UUID.randomUUID()}.db"
        database = FinPulseDatabase.create(context, name)
    }

    @After fun tearDown() {
        viewModel?.viewModelScope?.cancel()
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun cachedValuesRemainVisibleInTheBlurStateUntilRevalidationCompletes() {
        val response = CompletableDeferred<Unit>()
        val api = FavoriteQuotesApi { symbols ->
            response.await()
            symbols.map { symbol ->
                val stock = MockMarketData.home.stocks.first { it.symbol == symbol }
                StockQuote(symbol, stock.priceInCents + 100, 3.5, stock.history)
            }
        }
        val model = homeViewModel(homeRepository(database, quotesApi = api))
        viewModel = model
        compose.setContent { FinPulseTheme { HomeRoute(model) } }
        val values = compose.onNodeWithTag("favorite_values_PETR4")
        compose.waitUntil(10_000) {
            runCatching { values.fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Atualizando…" }
                .getOrDefault(false)
        }
        values.assert(hasAnyDescendant(hasText("R$ 38,42")))
        compose.onNodeWithText("R$ 350.000,00").assertIsDisplayed()
        compose.onNodeWithText("Carregando seu patrimônio…").assertDoesNotExist()

        response.complete(Unit)
        compose.waitUntil(10_000) {
            values.fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Cotação disponível"
        }
        values.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Cotação disponível"))
        values.assert(hasAnyDescendant(hasText("R$ 39,42")))
        compose.onNodeWithText("R$ 350.000,00").assertIsDisplayed()
    }
}
