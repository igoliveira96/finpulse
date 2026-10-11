package br.com.goulart.finpulse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import br.com.goulart.finpulse.data.local.entity.StockEntity
import br.com.goulart.finpulse.data.local.entity.StockHistoryEntity
import br.com.goulart.finpulse.data.local.model.StockWithHistory
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StockDao {
    @Transaction
    @Query("SELECT * FROM stocks ORDER BY display_order, symbol")
    abstract suspend fun getAllWithHistory(): List<StockWithHistory>

    @Transaction
    @Query("SELECT * FROM stocks ORDER BY display_order, symbol")
    abstract fun observeAllWithHistory(): Flow<List<StockWithHistory>>

    @Transaction
    @Query("""
        SELECT stocks.* FROM stocks
        INNER JOIN favorites ON favorites.stock_symbol = stocks.symbol
        WHERE favorites.investor_id = :investorId
        ORDER BY stocks.display_order, stocks.symbol
    """)
    abstract suspend fun getFavoritesWithHistory(investorId: Long): List<StockWithHistory>

    @Upsert
    abstract suspend fun upsert(stock: StockEntity)

    @Upsert
    protected abstract suspend fun upsertHistory(points: List<StockHistoryEntity>)

    @Query("DELETE FROM stock_history WHERE stock_symbol = :symbol")
    protected abstract suspend fun deleteHistory(symbol: String)

    @Query("DELETE FROM stocks WHERE symbol = :symbol")
    abstract suspend fun deleteBySymbol(symbol: String)

    @Transaction
    open suspend fun saveWithHistory(stock: StockEntity, values: List<Float>) {
        upsert(stock)
        deleteHistory(stock.symbol)
        upsertHistory(values.mapIndexed { index, value -> StockHistoryEntity(stock.symbol, index, value) })
    }
}
