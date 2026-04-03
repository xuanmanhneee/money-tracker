package com.example.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.moneytracker.data.local.entity.Wallet
import com.example.moneytracker.data.local.entity.Category
import com.example.moneytracker.data.local.entity.Transaction

import com.example.moneytracker.data.local.dao.WalletDao
import com.example.moneytracker.data.local.dao.CategoryDao
import com.example.moneytracker.data.local.dao.TransactionDao

import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Wallet::class, Category::class, Transaction::class],
    version = 1,
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
                    // 1. TRUYỀN CONTEXT VÀO CALLBACK
                    .addCallback(DatabaseCallback(context))
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // 2. KHAI BÁO CLASS NHẬN CONTEXT
        // --- CẬP NHẬT LẠI FILE APPDATABASE.KT ---
        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    // Lưu ý: Bây giờ chúng ta truyền cả 3 DAO vào đây
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
                // 1. TẠO VÍ TRƯỚC (Khớp 100% với class Wallet)
                val defaultWallet = Wallet(
                    id = 1,
                    name = "Ví chính",
                    balance = 0L
                )
                walletDao.insert(defaultWallet)

                // 2. TẠO DANH MỤC (Khớp 100% với class Category)
                val catFood = Category(
                    id = 1,
                    name = "Ăn uống",
                    type = 0, // 0: Chi tiêu
                    icon = "ic_food"
                )
                val catSalary = Category(
                    id = 2,
                    name = "Tiền lương",
                    type = 1, // 1: Thu nhập
                    icon = "ic_salary"
                )
                val catTransport = Category(
                    id = 3,
                    name = "Di chuyển",
                    type = 0, // 0: Chi tiêu
                    icon = "ic_transport"
                )
                categoryDao.insert(catFood)
                categoryDao.insert(catSalary)
                categoryDao.insert(catTransport)

                // 3. TẠO GIAO DỊCH (Sẽ không còn lỗi Foreign Key nữa)
                val sample1 = Transaction(
                    amount = -35000L,
                    date = System.currentTimeMillis() - 86400000, // Hôm qua
                    note = "Phở bò",
                    categoryId = 1, // Trỏ về "Ăn uống"
                    walletId = 1    // Trỏ về "Ví chính"
                )
                val sample2 = Transaction(
                    amount = 15000000L,
                    date = System.currentTimeMillis(), // Hôm nay
                    note = "Lương tháng 3",
                    categoryId = 2, // Trỏ về "Tiền lương"
                    walletId = 1
                )
                val sample3 = Transaction(
                    amount = -50000L,
                    date = System.currentTimeMillis(), // Hôm nay
                    note = "Đổ xăng",
                    categoryId = 3, // Trỏ về "Di chuyển"
                    walletId = 1
                )

                transactionDao.insert(sample1)
                transactionDao.insert(sample2)
                transactionDao.insert(sample3)
            }
        }
    }
}