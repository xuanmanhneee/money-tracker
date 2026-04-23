package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.WalletDao
import com.finflow.moneytracker.data.local.entity.Wallet
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun getWalletsStream(): Flow<List<Wallet>>
    fun getWalletStream(id: String): Flow<Wallet?>
    suspend fun insertWallet(wallet: Wallet)
    suspend fun updateWallet(wallet: Wallet)
    suspend fun deleteWallet(wallet: Wallet)
}

class OfflineWalletRepository(private val walletDao: WalletDao) : WalletRepository {
    override fun getWalletsStream(): Flow<List<Wallet>> = walletDao.getAll()
    override fun getWalletStream(id: String): Flow<Wallet?> = walletDao.getById(id)
    override suspend fun insertWallet(wallet: Wallet) = walletDao.insert(wallet)
    override suspend fun updateWallet(wallet: Wallet) = walletDao.update(wallet.copy(updatedAt = System.currentTimeMillis()))
    
    override suspend fun deleteWallet(wallet: Wallet) {
        // Đánh dấu xóa thay vì xóa thật để đồng bộ Firebase sau này
        walletDao.update(wallet.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
    }
}