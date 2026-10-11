package br.com.goulart.finpulse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
    val company: String,
    @ColumnInfo(name = "price_in_cents") val priceInCents: Long,
    @ColumnInfo(name = "daily_change_percent") val dailyChangePercent: Double,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
)

@Entity(
    tableName = "stock_history",
    primaryKeys = ["stock_symbol", "sample_index"],
    foreignKeys = [ForeignKey(
        entity = StockEntity::class,
        parentColumns = ["symbol"], childColumns = ["stock_symbol"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class StockHistoryEntity(
    @ColumnInfo(name = "stock_symbol") val stockSymbol: String,
    @ColumnInfo(name = "sample_index") val sampleIndex: Int,
    val value: Float,
)
