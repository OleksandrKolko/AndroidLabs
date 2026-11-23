package com.example.lab18.task1.data

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Upsert
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.flow.Flow

/**
 * Складений первинний ключ (date + currency):
 * USD за дві дати — два записи; повторне завантаження тієї самої дати оновлює запис.
 */
@Entity(tableName = "exchange_rates", primaryKeys = ["date", "currency"])
data class ExchangeRateEntity(
    val date: String,
    val currency: String,
    val baseCurrency: String,
    val purchaseRate: Double?,
    val saleRate: Double?
)

@Dao
interface ExchangeRateDao {

    @Upsert
    suspend fun upsertAll(rates: List<ExchangeRateEntity>)

    // Реактивний запит: Room сам повторить його після кожної зміни таблиці
    @Query("SELECT * FROM exchange_rates WHERE date = :date ORDER BY currency ASC")
    fun observeRates(date: String): Flow<List<ExchangeRateEntity>>

    // ⭐ Дати, для яких у базі вже є курси
    @Query("SELECT DISTINCT date FROM exchange_rates")
    fun observeSavedDates(): Flow<List<String>>
}

@Database(entities = [ExchangeRateEntity::class], version = 1, exportSchema = false)
abstract class ExchangeDatabase : RoomDatabase() {

    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: ExchangeDatabase? = null

        fun getDatabase(context: Context): ExchangeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExchangeDatabase::class.java,
                    "exchange_rates.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
