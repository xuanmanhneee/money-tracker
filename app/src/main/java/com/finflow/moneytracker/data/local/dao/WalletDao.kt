package com.finflow.moneytracker.data.local.dao

import androidx.room.*
import com.finflow.moneytracker.data.local.entity.Wallet
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: Wallet): Long

    @Update
    suspend fun update(wallet: Wallet)

    @Delete
    suspend fun delete(wallet: Wallet)

    @Query("SELECT * FROM wallets WHERE is_deleted = 0")
    fun getAll(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE id = :id AND is_deleted = 0")
    fun getById(id: Long): Flow<Wallet?>

    @Query("SELECT * FROM wallets WHERE id = :id AND is_deleted = 0")
    suspend fun getByIdOnce(id: Long): Wallet?

    @Query("UPDATE wallets SET balance = balance + :amount, updated_at = :timestamp WHERE id = :walletId")
    suspend fun addBalance(walletId: Long, amount: Long, timestamp: Long = System.currentTimeMillis())
}
