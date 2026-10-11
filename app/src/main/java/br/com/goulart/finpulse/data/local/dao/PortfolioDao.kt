package br.com.goulart.finpulse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import br.com.goulart.finpulse.data.local.entity.PortfolioEntity
import br.com.goulart.finpulse.data.local.entity.PortfolioHistoryEntity
import br.com.goulart.finpulse.data.local.model.PortfolioWithHistory

@Dao
abstract class PortfolioDao {
    @Transaction
    @Query("SELECT * FROM portfolios WHERE investor_id = :investorId")
    abstract suspend fun getWithHistory(investorId: Long): PortfolioWithHistory?

    @Upsert
    abstract suspend fun upsert(portfolio: PortfolioEntity)

    @Upsert
    protected abstract suspend fun upsertHistory(points: List<PortfolioHistoryEntity>)

    @Query("DELETE FROM portfolio_history WHERE investor_id = :investorId")
    protected abstract suspend fun deleteHistory(investorId: Long)

    @Transaction
    open suspend fun saveWithHistory(portfolio: PortfolioEntity, values: List<Float>) {
        upsert(portfolio)
        deleteHistory(portfolio.investorId)
        upsertHistory(values.mapIndexed { index, value ->
            PortfolioHistoryEntity(portfolio.investorId, index, value)
        })
    }
}
