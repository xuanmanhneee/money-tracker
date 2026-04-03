package com.example.moneytracker.data.repository

import com.example.moneytracker.data.local.dao.TransactionDao
import com.example.moneytracker.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactionsStream(): Flow<List<Transaction>>
    fun getTransactionStream(id: Int): Flow<Transaction?>

    // Thêm các hàm mới từ DAO vào Interface
    fun getTransactionsByDateRangeStream(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getTransactionsByWalletStream(walletId: Int): Flow<List<Transaction>>
    fun getTotalAmountStream(type: Int, startDate: Long, endDate: Long): Flow<Long?>

    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
}

class OfflineTransactionRepository(private val transactionDao: TransactionDao) : TransactionRepository {
    override fun getAllTransactionsStream(): Flow<List<Transaction>> = transactionDao.getAll()

    override fun getTransactionStream(id: Int): Flow<Transaction?> = transactionDao.getById(id)

    // Implement các hàm mới
    override fun getTransactionsByDateRangeStream(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startDate, endDate)

    override fun getTransactionsByWalletStream(walletId: Int): Flow<List<Transaction>> =
        transactionDao.getByWallet(walletId)

    override fun getTotalAmountStream(type: Int, startDate: Long, endDate: Long): Flow<Long?> =
        transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate)

    override suspend fun insertTransaction(transaction: Transaction) = transactionDao.insert(transaction)
    override suspend fun deleteTransaction(transaction: Transaction) = transactionDao.delete(transaction)
    override suspend fun updateTransaction(transaction: Transaction) = transactionDao.update(transaction)
}