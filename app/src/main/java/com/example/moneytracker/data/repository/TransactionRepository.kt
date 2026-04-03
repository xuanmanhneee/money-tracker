package com.example.moneytracker.data.repository

import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.data.model.PaymentMethod

class TransactionRepository {
    
    companion object {
        private val transactions = mutableListOf<Transaction>()
        
        private var walletBalance = mapOf(
            PaymentMethod.CASH to 0.0,
            PaymentMethod.BANK to 0.0
        ).toMutableMap()
        
        private var instance: TransactionRepository? = null
        
        fun getInstance(): TransactionRepository {
            if (instance == null) {
                instance = TransactionRepository()
            }
            return instance!!
        }
    }

    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
        updateWalletBalance(transaction)
    }

    fun updateTransaction(oldTransaction: Transaction, newTransaction: Transaction) {
        // Xóa giao dịch cũ khỏi balance
        removeFromWalletBalance(oldTransaction)
        
        // Cập nhật transaction trong list
        val index = transactions.indexOfFirst { it.id == oldTransaction.id }
        if (index != -1) {
            transactions[index] = newTransaction
        }
        
        // Thêm giao dịch mới vào balance
        updateWalletBalance(newTransaction)
    }

    fun deleteTransaction(transaction: Transaction) {
        transactions.remove(transaction)
        removeFromWalletBalance(transaction)
    }

    fun getAllTransactions(): List<Transaction> {
        return transactions.sortedByDescending { it.date }
    }

    fun getTransactionsByPaymentMethod(paymentMethod: PaymentMethod): List<Transaction> {
        return transactions
            .filter { it.paymentMethod == paymentMethod }
            .sortedByDescending { it.date }
    }

    private fun updateWalletBalance(transaction: Transaction) {
        val currentBalance = walletBalance[transaction.paymentMethod] ?: 0.0
        val newBalance = if (transaction.category.type.displayName == "Thu nhập") {
            currentBalance + transaction.amount
        } else {
            currentBalance - transaction.amount
        }
        walletBalance[transaction.paymentMethod] = newBalance
    }

    private fun removeFromWalletBalance(transaction: Transaction) {
        val currentBalance = walletBalance[transaction.paymentMethod] ?: 0.0
        val newBalance = if (transaction.category.type.displayName == "Thu nhập") {
            currentBalance - transaction.amount
        } else {
            currentBalance + transaction.amount
        }
        walletBalance[transaction.paymentMethod] = newBalance
    }

    fun getWalletBalance(paymentMethod: PaymentMethod): Double {
        return walletBalance[paymentMethod] ?: 0.0
    }

    fun getTotalBalance(): Double {
        return walletBalance.values.sum()
    }
}
