package br.com.goulart.finpulse

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun awaitHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("R$ 350.000,00").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun localDatabaseDisplaysPortfolioFavoritesAndCompleteMarketList() {
        awaitHome()
        compose.onNodeWithText("R$ 350.000,00").assertIsDisplayed()
        compose.onNodeWithText("Favoritas").assertIsDisplayed()
        listOf("WEGE3", "BBDC4", "Dados e cotações ilustrativos.").forEach { label ->
            compose.onNodeWithTag("home_list").performScrollToNode(hasText(label))
            compose.onNodeWithText(label).assertIsDisplayed()
        }
        compose.onNodeWithText("Retirada sustentável").assertDoesNotExist()
        compose.onNodeWithText("SWR").assertDoesNotExist()
    }

    @Test
    fun prototypeActionsHaveNoClickHandlers() {
        awaitHome()
        compose.onNodeWithContentDescription("Perfil").assertHasNoClickAction()
        compose.onNodeWithContentDescription("Notificações").assertHasNoClickAction()
        compose.onNodeWithContentDescription("Visibilidade do patrimônio").assertHasNoClickAction()
        compose.onNodeWithText("3M").assertHasNoClickAction()
    }

    @Test
    fun recreationPreservesTheLoadedResponse() {
        awaitHome()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("R$ 350.000,00").assertIsDisplayed()
        compose.onNodeWithText("Carregando seu patrimônio…").assertDoesNotExist()
    }
}
