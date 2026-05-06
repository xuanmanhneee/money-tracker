package com.finflow.moneytracker.data.remote

import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.local.entity.Wallet

class TestRemoteDataSource : RemoteDataSource {

    override suspend fun syncWallet(wallet: Wallet) {
        println("""
            
            ┌──────────────────────────────────────────────────────────┐
            │ 🚀 [SYNC WALLET]                                         │
            ├──────────────────────────────────────────────────────────┤
            │ > Tên: ${wallet.name.padEnd(45)} │
            │ > Số dư: ${wallet.balance.toString().padEnd(42)} │
            │ > ID: ${wallet.id.toString().padEnd(46)} │
            └──────────────────────────────────────────────────────────┘
        """.trimIndent())
    }

    override suspend fun syncCategory(category: Category) {
        println("""
            
            ┌──────────────────────────────────────────────────────────┐
            │ 📂 [SYNC CATEGORY]                                       │
            ├──────────────────────────────────────────────────────────┤
            │ > Tên: ${category.name.padEnd(45)} │
            │ > Hạn mức: ${(category.monthlyBudgetLimit?.toString() ?: "Không có").padEnd(40)} │
            │ > ID: ${category.id.toString().padEnd(46)} │
            └──────────────────────────────────────────────────────────┘
        """.trimIndent())
    }

    override suspend fun syncTransaction(transaction: Transaction) {
        println("""
            
            ┌──────────────────────────────────────────────────────────┐
            │ 💸 [SYNC TRANSACTION]                                    │
            ├──────────────────────────────────────────────────────────┤
            │ > Số tiền: ${transaction.amount.toString().padEnd(42)} │
            │ > Ghi chú: ${(transaction.note ?: "").padEnd(42)} │
            │ > Ngày: ${transaction.date.toString().padEnd(44)} │
            │ > ID: ${transaction.id.toString().padEnd(46)} │
            └──────────────────────────────────────────────────────────┘
        """.trimIndent())
    }

    override suspend fun fetchWallets(): List<Wallet> = emptyList()

    override suspend fun fetchCategories(): List<Category> = emptyList()

    override suspend fun fetchTransactions(): List<Transaction> = emptyList()
}
