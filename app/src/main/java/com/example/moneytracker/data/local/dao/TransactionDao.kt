package com.example.moneytracker.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.moneytracker.data.local.entity.Transaction

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Int): Flow<Transaction?>

    // Lấy toàn bộ giao dịch, sắp xếp mới nhất lên đầu
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    // Lấy giao dịch trong một khoảng thời gian (VD: Từ ngày 1 đến ngày 31 của tháng)
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    // Lấy giao dịch của một ví cụ thể
    @Query("SELECT * FROM transactions WHERE wallet_id = :walletId OR to_wallet_id = :walletId ORDER BY date DESC")
    fun getByWallet(walletId: Int): Flow<List<Transaction>>

    // TÍNH TOÁN: Lấy tổng tiền Thu/Chi trong 1 khoảng thời gian (Dùng để vẽ biểu đồ)
    // Cần JOIN với bảng categories để biết giao dịch đó là loại nào
    @Query("""
        SELECT SUM(t.amount) FROM transactions t 
        INNER JOIN categories c ON t.category_id = c.id 
        WHERE c.type = :type AND t.date BETWEEN :startDate AND :endDate
    """)
    fun getTotalAmountByTypeAndDateRange(type: Int, startDate: Long, endDate: Long): Flow<Long?>
}