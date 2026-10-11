package br.com.goulart.finpulse

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import br.com.goulart.finpulse.data.local.FinPulseDatabase
import br.com.goulart.finpulse.data.local.RoomHomeRepository
import br.com.goulart.finpulse.ui.home.HomeRoute
import br.com.goulart.finpulse.ui.home.HomeViewModel
import br.com.goulart.finpulse.ui.theme.FinPulseTheme
import java.util.UUID
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeRoomIntegrationTest {
    @get:Rule
    val compose = createComposeRule()
    private lateinit var context: Context
    private lateinit var database: FinPulseDatabase
    private lateinit var name: String
    private var homeViewModel: HomeViewModel? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        name = "home-ui-${UUID.randomUUID()}.db"
        database = FinPulseDatabase.create(context, name)
    }

    @After
    fun tearDown() {
        homeViewModel?.viewModelScope?.cancel()
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun homeDisplaysDatabaseValuesAndReflectsPersistedChanges() {
        val portfolio = runBlocking {
            val investor = requireNotNull(database.investorDao().getById(1))
            database.investorDao().upsert(investor.copy(firstName = "Ana", initials = "AN"))
            val original = requireNotNull(database.portfolioDao().getWithHistory(1)).portfolio
            database.portfolioDao().upsert(original.copy(balanceInCents = 42_000_000))
            original
        }
        val viewModel = HomeViewModel(RoomHomeRepository(database))
        homeViewModel = viewModel
        compose.setContent { FinPulseTheme { HomeRoute(viewModel) } }
        awaitText("R$ 420.000,00")
        compose.onNodeWithText("Olá, Ana").assertIsDisplayed()
        compose.onNodeWithText("R$ 350.000,00").assertDoesNotExist()

        runBlocking { database.portfolioDao().upsert(portfolio.copy(balanceInCents = 51_000_000)) }
        awaitText("R$ 510.000,00")
        compose.onNodeWithText("R$ 420.000,00").assertDoesNotExist()
    }

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text).assertIsDisplayed()
    }
}
