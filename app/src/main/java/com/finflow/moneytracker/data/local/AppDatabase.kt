package com.finflow.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 6,
    exportSchema = false
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

        private const val LEGACY_DEFAULT_WALLET_NAME = "V\u00ed m\u1eb7c \u0111\u1ecbnh"
        private const val CASH_WALLET_NAME = "Ti\u1ec1n m\u1eb7t"

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()

                val legacyWallet = findActiveWalletByName(db, LEGACY_DEFAULT_WALLET_NAME)
                val cashWallet = findActiveWalletByName(db, CASH_WALLET_NAME)

                // Case 1: no legacy wallet -> nothing to migrate
                if (legacyWallet == null) {
                    return
                }

                // Case 2: only legacy wallet exists -> rename to cash wallet
                if (cashWallet == null) {
                    db.execSQL(
                        """
                        UPDATE wallets
                        SET name = '$CASH_WALLET_NAME', updatedAt = $now
                        WHERE id = '${legacyWallet.id}'
                        """.trimIndent()
                    )
                    return
                }

                // Case 3: both wallets exist -> merge data to cash wallet and remove legacy wallet
                if (legacyWallet.id == cashWallet.id) {
                    return
                }

                db.execSQL(
                    """
                    UPDATE transactions
                    SET wallet_id = '${cashWallet.id}'
                    WHERE wallet_id = '${legacyWallet.id}'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE transactions
                    SET to_wallet_id = '${cashWallet.id}'
                    WHERE to_wallet_id = '${legacyWallet.id}'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE budgets
                    SET wallet_id = '${cashWallet.id}', updated_at = $now
                    WHERE wallet_id = '${legacyWallet.id}'
                    """.trimIndent()
                )

                val mergedBalance = cashWallet.balance + legacyWallet.balance
                db.execSQL(
                    """
                    UPDATE wallets
                    SET balance = $mergedBalance, updatedAt = $now
                    WHERE id = '${cashWallet.id}'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    DELETE FROM wallets
                    WHERE id = '${legacyWallet.id}'
                    """.trimIndent()
                )
            }
        }

        private data class WalletMigrationRow(
            val id: String,
            val balance: Long
        )

        private fun findActiveWalletByName(
            db: SupportSQLiteDatabase,
            walletName: String
        ): WalletMigrationRow? {
            val escapedName = walletName.replace("'", "''")
            db.query(
                """
                SELECT id, balance
                FROM wallets
                WHERE name = '$escapedName' AND isDeleted = 0
                LIMIT 1
                """.trimIndent()
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    return null
                }
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val balance = cursor.getLong(cursor.getColumnIndexOrThrow("balance"))
                return WalletMigrationRow(id = id, balance = balance)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_database"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}