package br.com.goulart.finpulse.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import br.com.goulart.finpulse.data.local.entity.InvestorEntity

@Dao
interface InvestorDao {
    @Query("SELECT * FROM investors WHERE id = :id")
    suspend fun getById(id: Long): InvestorEntity?

    @Upsert
    suspend fun upsert(investor: InvestorEntity)

    @Query("DELETE FROM investors WHERE id = :id")
    suspend fun deleteById(id: Long)
}
