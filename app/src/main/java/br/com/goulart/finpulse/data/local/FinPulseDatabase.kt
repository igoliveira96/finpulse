package br.com.goulart.finpulse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.goulart.finpulse.data.local.dao.FavoriteDao
import br.com.goulart.finpulse.data.local.dao.InvestorDao
import br.com.goulart.finpulse.data.local.dao.PortfolioDao
import br.com.goulart.finpulse.data.local.dao.StockDao
import br.com.goulart.finpulse.data.local.entity.FavoriteEntity
import br.com.goulart.finpulse.data.local.entity.InvestorEntity
import br.com.goulart.finpulse.data.local.entity.PortfolioEntity
import br.com.goulart.finpulse.data.local.entity.PortfolioHistoryEntity
import br.com.goulart.finpulse.data.local.entity.StockEntity
import br.com.goulart.finpulse.data.local.entity.StockHistoryEntity

@Database(
    entities = [InvestorEntity::class, PortfolioEntity::class, PortfolioHistoryEntity::class,
        StockEntity::class, StockHistoryEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FinPulseDatabase : RoomDatabase() {
    abstract fun investorDao(): InvestorDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun stockDao(): StockDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "finpulse.db"
        private const val SEED_ASSET = "database/finpulse.db"

        /** Copies and validates the seed on first open; existing user data is never reseeded. */
        fun create(context: Context, name: String = DATABASE_NAME): FinPulseDatabase =
            Room.databaseBuilder(context.applicationContext, FinPulseDatabase::class.java, name)
                .createFromAsset(SEED_ASSET)
                .build()
    }
}
