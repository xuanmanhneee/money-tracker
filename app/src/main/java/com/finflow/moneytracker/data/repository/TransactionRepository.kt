package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.TransactionDao
import com.finflow.moneytracker.data.local.dao.WalletDao
import com.finflow.moneytracker.data.local.entity.Transaction
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
    
    // HÀM MỚI: Xử lý chuyển khoản giữa 2 ví
    suspend fun transferMoney(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Long,
        note: String?,
        date: Long,
        transferCategoryId: Long
    )
}

class DefaultTransactionRepository(
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
    private val remoteDataSource: RemoteDataSource
) : TransactionRepository {
    override fun getAllTransactionsStream(): Flow<List<Transaction>> = transactionDao.getAll()
    override fun getTransactionStream(id: Long): Flow<Transaction?> = transactionDao.getById(id)
    override fun getTransactionsByDateRangeStream(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startDate, endDate)

    override fun getTransactionsByWalletStream(walletId: Long): Flow<List<Transaction>> =
        transactionDao.getByWallet(walletId)

    override fun getTotalAmountStream(type: Int, startDate: Long, endDate: Long): Flow<Long?> =
        transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate)

    override suspend fun insertTransaction(transaction: Transaction) {
        // 1. Lưu vào Local và lấy ID thực tế
        val generatedId = transactionDao.insert(transaction)
        val insertedTransaction = transaction.copy(id = generatedId)
        
        // 2. Cập nhật số dư ví (Giao dịch thường)
        if (insertedTransaction.toWalletId == null) {
            walletDao.addBalance(insertedTransaction.walletId, insertedTransaction.amount)
        }
        
        // 3. Sync lên Firebase với ID đúng
        remoteDataSource.syncTransaction(insertedTransaction)
    }
    
    override suspend fun updateTransaction(transaction: Transaction) {
        val updatedTransaction = transaction.copy(updatedAt = System.currentTimeMillis())
        transactionDao.update(updatedTransaction)
        remoteDataSource.syncTransaction(updatedTransaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        val deletedTransaction = transaction.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
        transactionDao.update(deletedTransaction)
        
        // Hoàn lại tiền khi xóa
        walletDao.addBalance(transaction.walletId, -transaction.amount)
        
        remoteDataSource.syncTransaction(deletedTransaction)
    }

    override suspend fun transferMoney(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Long,
        note: String?,
        date: Long,
        transferCategoryId: Long
    ) {
        val transferTx = Transaction(
            walletId = fromWalletId,
            toWalletId = toWalletId,
            categoryId = transferCategoryId,
            amount = amount,
            date = date,
            note = note
        )
        
        // 1. Lưu giao dịch và lấy ID
        val generatedId = transactionDao.insert(transferTx)
        val insertedTx = transferTx.copy(id = generatedId)
        
        // 2. Trừ tiền ví gửi
        walletDao.addBalance(fromWalletId, -amount)
        
        // 3. Cộng tiền ví nhận
        walletDao.addBalance(toWalletId, amount)
        
        // 4. Sync
        remoteDataSource.syncTransaction(insertedTx)
    }
}
