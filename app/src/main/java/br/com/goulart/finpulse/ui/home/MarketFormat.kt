package br.com.goulart.finpulse.ui.home

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val Brazil = Locale.forLanguageTag("pt-BR")

internal fun currency(cents: Long): String = NumberFormat.getCurrencyInstance(Brazil)
    .format(BigDecimal.valueOf(cents, 2)).replace('\u00A0', ' ')

internal fun percent(value: Double): String = String.format(Brazil, "%+.2f%%", value)
