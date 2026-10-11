package br.com.goulart.finpulse

import br.com.goulart.finpulse.ui.home.currency
import br.com.goulart.finpulse.ui.home.percent
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketFormatTest {
    @Test
    fun centsUseBrazilianCurrencyWithoutFloatingPointRounding() {
        assertEquals("R$ 350.000,00", currency(35_000_000))
        assertEquals("R$ 38,42", currency(3_842))
        assertEquals("R$ 0,01", currency(1))
    }

    @Test
    fun variationsKeepTheirSignAndBrazilianDecimalSeparator() {
        assertEquals("+1,24%", percent(1.24))
        assertEquals("-0,85%", percent(-0.85))
    }
}
