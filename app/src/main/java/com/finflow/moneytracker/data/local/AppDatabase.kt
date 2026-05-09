package com.finflow.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finflow.moneytracker.data.local.converter.CategoryTypeConverter
import com.finflow.moneytracker.data.local.entity.Wallet
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.local.entity.Budget
import com.finflow.moneytracker.data.local.entity.BudgetAllocation
import com.finflow.moneytracker.data.local.entity.BudgetHistory
import com.finflow.moneytracker.data.local.dao.WalletDao
import com.finflow.moneytracker.data.local.dao.CategoryDao
import com.finflow.moneytracker.data.local.dao.TransactionDao
import com.finflow.moneytracker.data.local.dao.BudgetDao
import com.finflow.moneytracker.data.local.dao.BudgetAllocationDao
import com.finflow.moneytracker.data.local.dao.BudgetHistoryDao

@Database(
    entities = [
        Wallet::class,
        Category::class,
        Transaction::class,
        Budget::class,
        BudgetAllocation::class,
        BudgetHistory::class
    ],
    version = 7,
    exportSchema = false
)

@TypeConverters(
    CategoryTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetAllocationDao(): BudgetAllocationDao
    abstract fun budgetHistoryDao(): BudgetHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_database"
                )
                    .fallbackToDestructiveMigration() // Xóa data cũ để khớp schema mới
                    .addCallback(SeedCallback()) // Thêm dữ liệu default vào bảng
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private class SeedCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                val currentTime = System.currentTimeMillis()

                // Ví mặc định
                db.execSQL("""
                INSERT INTO wallets ( user_id, name, balance, is_default, is_deleted, updated_at) 
                VALUES ( 'SYSTEM', 'Tiền mặt', 0, 1, 0, $currentTime)
                """.trimIndent())
            }
        }
    }
}