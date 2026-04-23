package com.finflow.moneytracker.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.finflow.moneytracker.data.local.entity.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    fun getById(id: String): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0 ORDER BY date DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (wallet_id = :walletId OR to_wallet_id = :walletId) AND isDeleted = 0 ORDER BY date DESC")
    fun getByWallet(walletId: String): Flow<List<Transaction>>

    @Query("""
        SELECT SUM(t.amount) FROM transactions t 
        INNER JOIN categories c ON t.category_id = c.id 
        WHERE c.type = :type AND t.date BETWEEN :startDate AND :endDate AND t.isDeleted = 0
    """)
    fun getTotalAmountByTypeAndDateRange(type: Int, startDate: Long, endDate: Long): Flow<Long?>
}