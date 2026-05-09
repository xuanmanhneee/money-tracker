package com.finflow.moneytracker.di

import android.content.Context
import com.finflow.moneytracker.data.local.AppDatabase
import com.finflow.moneytracker.data.local.dao.CategoryDao
import com.finflow.moneytracker.data.local.dao.TransactionDao
import com.finflow.moneytracker.data.local.dao.WalletDao
import com.finflow.moneytracker.data.remote.FirestoreRemoteDataSource
import com.finflow.moneytracker.data.remote.RemoteDataSource
import com.finflow.moneytracker.data.repository.CategoryRepository
import com.finflow.moneytracker.data.repository.BudgetRepository
import com.finflow.moneytracker.data.repository.DefaultCategoryRepository
import com.finflow.moneytracker.data.repository.DefaultTransactionRepository
import com.finflow.moneytracker.data.repository.DefaultWalletRepository
import com.finflow.moneytracker.data.repository.OfflineBudgetRepository
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

interface AppContainer {
    val walletRepository: WalletRepository
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val budgetRepository: BudgetRepository
    val remoteDataSource: RemoteDataSource
    
    // Expose DAOs for low-level sync operations
    val walletDao: WalletDao
    val categoryDao: CategoryDao
    val transactionDao: TransactionDao
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val walletDao: WalletDao by lazy { database.walletDao() }
    override val categoryDao: CategoryDao by lazy { database.categoryDao() }
    override val transactionDao: TransactionDao by lazy { database.transactionDao() }

    override val remoteDataSource: RemoteDataSource by lazy {
        FirestoreRemoteDataSource(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
    }

    override val walletRepository: WalletRepository by lazy {
        DefaultWalletRepository(walletDao, remoteDataSource)
    }

    override val categoryRepository: CategoryRepository by lazy {
        DefaultCategoryRepository(categoryDao, remoteDataSource)
    }

    override val transactionRepository: TransactionRepository by lazy {
        DefaultTransactionRepository(
            transactionDao,
            walletDao,
            categoryDao,
            remoteDataSource
        )
    }

    override val budgetRepository: BudgetRepository by lazy {
        OfflineBudgetRepository(
            database.budgetDao(),
            database.budgetAllocationDao(),
            database.budgetHistoryDao()
        )
    }
}
