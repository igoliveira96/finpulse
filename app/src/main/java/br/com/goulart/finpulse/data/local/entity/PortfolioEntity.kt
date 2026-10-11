package br.com.goulart.finpulse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "portfolios",
    foreignKeys = [ForeignKey(
        entity = InvestorEntity::class,
        parentColumns = ["id"], childColumns = ["investor_id"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class PortfolioEntity(
    @PrimaryKey @ColumnInfo(name = "investor_id") val investorId: Long,
    @ColumnInfo(name = "balance_in_cents") val balanceInCents: Long,
    @ColumnInfo(name = "monthly_gain_in_cents") val monthlyGainInCents: Long,
    @ColumnInfo(name = "monthly_change_percent") val monthlyChangePercent: Double,
)

@Entity(
    tableName = "portfolio_history",
    primaryKeys = ["investor_id", "sample_index"],
    foreignKeys = [ForeignKey(
        entity = PortfolioEntity::class,
        parentColumns = ["investor_id"], childColumns = ["investor_id"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class PortfolioHistoryEntity(
    @ColumnInfo(name = "investor_id") val investorId: Long,
    @ColumnInfo(name = "sample_index") val sampleIndex: Int,
    val value: Float,
)
