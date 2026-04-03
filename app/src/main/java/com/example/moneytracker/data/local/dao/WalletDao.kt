package com.example.moneytracker.data.local.dao

import androidx.room.*
import com.example.moneytracker.data.local.entity.Wallet
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: Wallet)

    @Update
    suspend fun update(wallet: Wallet)

    @Delete
    suspend fun delete(wallet: Wallet)

    // Lấy toàn bộ ví, trả về Flow để UI tự update khi có ví mới
    @Query("SELECT * FROM wallets")
    fun getAll(): Flow<List<Wallet>>

    // Lấy 1 ví cụ thể bằng ID
    @Query("SELECT * FROM wallets WHERE id = :id")
    fun getById(id: Int): Flow<Wallet?>

    // Lệnh tiện ích: Cập nhật nhanh số dư của ví mà không cần query cả object Wallet
    @Query("UPDATE wallets SET balance = balance + :amount WHERE id = :walletId")
    suspend fun addBalance(walletId: Int, amount: Long)
}