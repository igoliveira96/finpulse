package br.com.goulart.finpulse.data.local.model

import br.com.goulart.finpulse.data.Investor
import br.com.goulart.finpulse.data.Portfolio
import br.com.goulart.finpulse.data.Stock
import br.com.goulart.finpulse.data.local.entity.InvestorEntity

fun InvestorEntity.toInvestor() = Investor(firstName, initials)

fun PortfolioWithHistory.toPortfolio() = Portfolio(
    balanceInCents = portfolio.balanceInCents,
    monthlyGainInCents = portfolio.monthlyGainInCents,
    monthlyChangePercent = portfolio.monthlyChangePercent,
    // Room relations do not guarantee ordering; chart samples must be explicit.
    history = history.sortedBy { it.sampleIndex }.map { it.value },
)

fun StockWithHistory.toStock(isFavorite: Boolean) = Stock(
    symbol = stock.symbol,
    company = stock.company,
    priceInCents = stock.priceInCents,
    dailyChangePercent = stock.dailyChangePercent,
    history = history.sortedBy { it.sampleIndex }.map { it.value },
    isFavorite = isFavorite,
)
