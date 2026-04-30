package com.finflow.moneytracker.data.remote

import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.local.entity.Wallet

interface RemoteDataSource {
    suspend fun syncWallet(wallet: Wallet)
    suspend fun syncCategory(category: Category)
    suspend fun syncTransaction(transaction: Transaction)

    // Hàm lấy dữ liệu từ Firebase về
    suspend fun fetchWallets(): List<Wallet>
    suspend fun fetchCategories(): List<Category>
    suspend fun fetchTransactions(): List<Transaction>
}