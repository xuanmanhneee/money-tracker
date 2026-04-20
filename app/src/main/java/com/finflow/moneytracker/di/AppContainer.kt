package com.finflow.moneytracker.di

import android.content.Context
import com.finflow.moneytracker.data.local.AppDatabase
import com.finflow.moneytracker.data.repository.CategoryRepository
import com.finflow.moneytracker.data.repository.BudgetRepository
import com.finflow.moneytracker.data.repository.OfflineCategoryRepository
import com.finflow.moneytracker.data.repository.OfflineBudgetRepository
import com.finflow.moneytracker.data.repository.OfflineTransactionRepository
import com.finflow.moneytracker.data.repository.OfflineWalletRepository
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository

interface AppContainer {
    // Phơi bày Repository ra cho ViewModel sử dụng
    val walletRepository: WalletRepository
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val budgetRepository: BudgetRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    // Khởi tạo Repository và tự động truyền DAO vào bên trong nó
    override val walletRepository: WalletRepository by lazy {
        OfflineWalletRepository(database.walletDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        OfflineCategoryRepository(database.categoryDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        OfflineTransactionRepository(database.transactionDao())
    }

    override val budgetRepository: BudgetRepository by lazy {
        OfflineBudgetRepository(
            database.budgetDao(),
            database.budgetAllocationDao(),
            database.budgetHistoryDao()
        )
    }
}