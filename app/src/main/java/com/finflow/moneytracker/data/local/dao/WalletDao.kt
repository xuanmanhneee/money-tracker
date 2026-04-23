package com.finflow.moneytracker.data.local.dao

import androidx.room.*
import com.finflow.moneytracker.data.local.entity.Wallet
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: Wallet)

    @Update
    suspend fun update(wallet: Wallet)

    // Thay vì xóa thật, ta đánh dấu isDeleted = true (thực hiện ở Repository)
    @Delete
    suspend fun delete(wallet: Wallet)

    @Query("SELECT * FROM wallets WHERE isDeleted = 0")
    fun getAll(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE id = :id AND isDeleted = 0")
    fun getById(id: String): Flow<Wallet?>

    @Query("UPDATE wallets SET balance = balance + :amount, updatedAt = :timestamp WHERE id = :walletId")
    suspend fun addBalance(walletId: String, amount: Long, timestamp: Long = System.currentTimeMillis())
}