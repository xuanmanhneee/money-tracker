package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.TransactionDao
import com.finflow.moneytracker.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactionsStream(): Flow<List<Transaction>>
    fun getTransactionStream(id: String): Flow<Transaction?>
    fun getTransactionsByDateRangeStream(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getTransactionsByWalletStream(walletId: String): Flow<List<Transaction>>
    fun getTotalAmountStream(type: Int, startDate: Long, endDate: Long): Flow<Long?>

    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
}

class OfflineTransactionRepository(private val transactionDao: TransactionDao) : TransactionRepository {
    override fun getAllTransactionsStream(): Flow<List<Transaction>> = transactionDao.getAll()
    override fun getTransactionStream(id: String): Flow<Transaction?> = transactionDao.getById(id)
    override fun getTransactionsByDateRangeStream(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startDate, endDate)

    override fun getTransactionsByWalletStream(walletId: String): Flow<List<Transaction>> =
        transactionDao.getByWallet(walletId)

    override fun getTotalAmountStream(type: Int, startDate: Long, endDate: Long): Flow<Long?> =
        transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate)

    override suspend fun insertTransaction(transaction: Transaction) = transactionDao.insert(transaction)
    
    override suspend fun updateTransaction(transaction: Transaction) = 
        transactionDao.update(transaction.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.update(transaction.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
    }
}