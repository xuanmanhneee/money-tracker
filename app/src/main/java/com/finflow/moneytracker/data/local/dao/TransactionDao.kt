package com.finflow.moneytracker.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.finflow.moneytracker.data.local.entity.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id AND is_deleted = 0")
    fun getById(id: Long): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND is_deleted = 0 ORDER BY date DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (wallet_id = :walletId OR to_wallet_id = :walletId) AND is_deleted = 0 ORDER BY date DESC")
    fun getByWallet(walletId: Long): Flow<List<Transaction>>

    @Query("""
        SELECT SUM(t.amount) FROM transactions t 
        INNER JOIN categories c ON t.category_id = c.id 
        WHERE c.type = :type AND t.date BETWEEN :startDate AND :endDate AND t.is_deleted = 0
    """)
    fun getTotalAmountByTypeAndDateRange(type: Int, startDate: Long, endDate: Long): Flow<Long?>

    // Trong TransactionDao.kt

    // Trả về list điểm (x = ngày/tháng, y = tổng tiền)
    @Query("""
    SELECT 
        CAST(strftime('%d', datetime(date / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS xValue,
        SUM(amount) AS yValue
    FROM transactions
    WHERE date BETWEEN :startDate AND :endDate
      AND category_id IN (SELECT id FROM categories WHERE type = :type)
      AND is_deleted = 0
    GROUP BY xValue
    ORDER BY xValue ASC
""")
    fun getChartDataByDay(type: Int, startDate: Long, endDate: Long): Flow<List<ChartPoint>>

    @Query("""
    SELECT 
        CAST(strftime('%m', datetime(date / 1000, 'unixepoch', 'localtime')) AS INTEGER) AS xValue,
        SUM(amount) AS yValue
    FROM transactions
    WHERE date BETWEEN :startDate AND :endDate
      AND category_id IN (SELECT id FROM categories WHERE type = :type)
      AND is_deleted = 0
    GROUP BY xValue
    ORDER BY xValue ASC
""")
    fun getChartDataByMonth(type: Int, startDate: Long, endDate: Long): Flow<List<ChartPoint>>

    // Data class ánh xạ kết quả query
    data class ChartPoint(val xValue: Int, val yValue: Long)
}
