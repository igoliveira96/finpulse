package br.com.goulart.finpulse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.goulart.finpulse.data.local.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE investor_id = :investorId ORDER BY stock_symbol")
    suspend fun getByInvestor(investorId: Long): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE investor_id = :investorId AND stock_symbol = :symbol")
    suspend fun remove(investorId: Long, symbol: String)
}
