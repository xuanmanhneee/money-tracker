package com.example.moneytracker.di

import android.content.Context
import com.example.moneytracker.data.local.AppDatabase
import com.example.moneytracker.data.repository.CategoryRepository
import com.example.moneytracker.data.repository.OfflineCategoryRepository
import com.example.moneytracker.data.repository.OfflineTransactionRepository
import com.example.moneytracker.data.repository.OfflineWalletRepository
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.data.repository.WalletRepository

interface AppContainer {
    // Phơi bày Repository ra cho ViewModel sử dụng
    val walletRepository: WalletRepository
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    // Khởi tạo Repository và tự động truyền DAO vào bên trong nó
    override val walletRepository: WalletRepository by lazy {
        OfflineWalletRepository(AppDatabase.getDatabase(context).walletDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        OfflineCategoryRepository(AppDatabase.getDatabase(context).categoryDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        OfflineTransactionRepository(AppDatabase.getDatabase(context).transactionDao())
    }
}