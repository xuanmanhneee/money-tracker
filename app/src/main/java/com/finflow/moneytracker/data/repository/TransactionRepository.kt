package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.CategoryDao
import com.finflow.moneytracker.data.local.dao.TransactionDao
import com.finflow.moneytracker.data.local.dao.WalletDao
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.local.model.CategoryType
import com.finflow.moneytracker.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactionsStream(): Flow<List<Transaction>>
    fun getTransactionStream(id: Long): Flow<Transaction?>
    fun getTransactionsByDateRangeStream(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getTransactionsByWalletStream(walletId: Long): Flow<List<Transaction>>
    fun getTotalAmountStream(type: Int, startDate: Long, endDate: Long): Flow<Long?>

    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
}

class DefaultTransactionRepository(
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
    private val categoryDao: CategoryDao,
    private val remoteDataSource: RemoteDataSource
) : TransactionRepository {
    override fun getAllTransactionsStream(): Flow<List<Transaction>> = transactionDao.getAll()
    override fun getTransactionStream(id: Long): Flow<Transaction?> = transactionDao.getById(id)
    override fun getTransactionsByDateRangeStream(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startDate, endDate)

    override fun getTransactionsByWalletStream(walletId: Long): Flow<List<Transaction>> =
        transactionDao.getByWallet(walletId)

    override fun getTotalAmountStream(
        type: Int,
        startDate: Long,
        endDate: Long
    ): Flow<Long?> =
        transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate)

    override suspend fun insertTransaction(transaction: Transaction) {
        val generatedId = transactionDao.insert(transaction)
        val insertedTransaction = transaction.copy(id = generatedId)

        if (insertedTransaction.toWalletId == null) {

            val category = insertedTransaction.categoryId
                ?.let { categoryDao.getById(it) }

            val delta = when (category?.type) {
                CategoryType.INCOME -> insertedTransaction.amount
                CategoryType.EXPENSE -> -insertedTransaction.amount
                null -> 0L
            }

            walletDao.addBalance(insertedTransaction.walletId, delta)
        } else {
            walletDao.addBalance(insertedTransaction.walletId, -insertedTransaction.amount)
            walletDao.addBalance(insertedTransaction.toWalletId, insertedTransaction.amount)
        }

        trySyncTransaction(insertedTransaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val updatedTransaction = transaction.copy(updatedAt = System.currentTimeMillis())
        transactionDao.update(updatedTransaction)
        trySyncTransaction(updatedTransaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val deletedTransaction = transaction.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.update(deletedTransaction)

        if (transaction.toWalletId == null) {

            val category = transaction.categoryId
                ?.let { categoryDao.getById(it) }

            val delta = when (category?.type) {
                CategoryType.INCOME -> -transaction.amount
                CategoryType.EXPENSE -> transaction.amount
                null -> 0L
            }

            walletDao.addBalance(transaction.walletId, delta)

        } else {
            walletDao.addBalance(transaction.walletId, transaction.amount)
            walletDao.addBalance(transaction.toWalletId, -transaction.amount)
        }

        trySyncTransaction(deletedTransaction)
    }

    private suspend fun trySyncTransaction(transaction: Transaction) {
        try {
            remoteDataSource.syncTransaction(transaction)
            walletDao.getByIdOnce(transaction.walletId)?.let { remoteDataSource.syncWallet(it) }
            transaction.toWalletId?.let { toId ->
                walletDao.getByIdOnce(toId)?.let { remoteDataSource.syncWallet(it) }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncError", "Sync failed: ${e.message}")
        }
    }

}
