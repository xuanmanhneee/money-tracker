package com.finflow.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.finflow.moneytracker.data.local.entity.Wallet
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction

import com.finflow.moneytracker.data.local.dao.WalletDao
import com.finflow.moneytracker.data.local.dao.CategoryDao
import com.finflow.moneytracker.data.local.dao.TransactionDao

import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Wallet::class, Category::class, Transaction::class],
    version = 3, // Tăng lên 3 để áp dụng thay đổi dữ liệu
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

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
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    populateDatabase(
                        database.walletDao(),
                        database.categoryDao(),
                        database.transactionDao()
                    )
                }
            }

            suspend fun populateDatabase(
                walletDao: WalletDao,
                categoryDao: CategoryDao,
                transactionDao: TransactionDao
            ) {
                val walletId = "default_wallet_id"
                val catFoodId = "cat_food_id"
                val catSalaryId = "cat_salary_id"
                val catTransportId = "cat_transport_id"

                val defaultWallet = Wallet(
                    id = walletId,
                    userId = "sample_user",
                    name = "Ví chính",
                    balance = 14915000L // Số dư sau khi cộng trừ
                )
                walletDao.insert(defaultWallet)

                val catFood = Category(
                    id = catFoodId,
                    userId = "sample_user",
                    name = "Ăn uống",
                    type = 0,
                    icon = "ic_food"
                )
                val catSalary = Category(
                    id = catSalaryId,
                    userId = "sample_user",
                    name = "Tiền lương",
                    type = 1,
                    icon = "ic_salary"
                )
                val catTransport = Category(
                    id = catTransportId,
                    userId = "sample_user",
                    name = "Di chuyển",
                    type = 0,
                    icon = "ic_transport"
                )
                categoryDao.insert(catFood)
                categoryDao.insert(catSalary)
                categoryDao.insert(catTransport)

                // CHI TIÊU: Để số âm để Adapter nhận diện
                val sample1 = Transaction(
                    userId = "sample_user",
                    amount = -35000L, 
                    date = System.currentTimeMillis() - 86400000,
                    note = "Phở bò",
                    categoryId = catFoodId,
                    walletId = walletId
                )
                // THU NHẬP: Để số dương
                val sample2 = Transaction(
                    userId = "sample_user",
                    amount = 15000000L,
                    date = System.currentTimeMillis(),
                    note = "Lương tháng 3",
                    categoryId = catSalaryId,
                    walletId = walletId
                )
                // CHI TIÊU: Để số âm
                val sample3 = Transaction(
                    userId = "sample_user",
                    amount = -50000L,
                    date = System.currentTimeMillis(),
                    note = "Đổ xăng",
                    categoryId = catTransportId,
                    walletId = walletId
                )

                transactionDao.insert(sample1)
                transactionDao.insert(sample2)
                transactionDao.insert(sample3)
            }
        }
    }
}