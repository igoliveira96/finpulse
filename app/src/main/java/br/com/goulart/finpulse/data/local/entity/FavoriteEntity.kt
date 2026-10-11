package br.com.goulart.finpulse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["investor_id", "stock_symbol"],
    foreignKeys = [
        ForeignKey(
            entity = InvestorEntity::class,
            parentColumns = ["id"], childColumns = ["investor_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StockEntity::class,
            parentColumns = ["symbol"], childColumns = ["stock_symbol"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stock_symbol")],
)
data class FavoriteEntity(
    @ColumnInfo(name = "investor_id") val investorId: Long,
    @ColumnInfo(name = "stock_symbol") val stockSymbol: String,
)
