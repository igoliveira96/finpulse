package br.com.goulart.finpulse.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import br.com.goulart.finpulse.data.local.entity.PortfolioEntity
import br.com.goulart.finpulse.data.local.entity.PortfolioHistoryEntity
import br.com.goulart.finpulse.data.local.entity.StockEntity
import br.com.goulart.finpulse.data.local.entity.StockHistoryEntity

data class PortfolioWithHistory(
    @Embedded val portfolio: PortfolioEntity,
    @Relation(parentColumn = "investor_id", entityColumn = "investor_id")
    val history: List<PortfolioHistoryEntity>,
)

data class StockWithHistory(
    @Embedded val stock: StockEntity,
    @Relation(parentColumn = "symbol", entityColumn = "stock_symbol")
    val history: List<StockHistoryEntity>,
)
