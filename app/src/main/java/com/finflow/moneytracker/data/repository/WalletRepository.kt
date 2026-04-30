package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.WalletDao
import com.finflow.moneytracker.data.local.entity.Wallet
import com.finflow.moneytracker.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun getWalletsStream(): Flow<List<Wallet>>
    fun getWalletStream(id: Long): Flow<Wallet?>
    suspend fun insertWallet(wallet: Wallet)
    suspend fun updateWallet(wallet: Wallet)
    suspend fun deleteWallet(wallet: Wallet)
}

class DefaultWalletRepository(
    private val walletDao: WalletDao,
    private val remoteDataSource: RemoteDataSource
) : WalletRepository {
    override fun getWalletsStream(): Flow<List<Wallet>> = walletDao.getAll()
    override fun getWalletStream(id: Long): Flow<Wallet?> = walletDao.getById(id)
    
    override suspend fun insertWallet(wallet: Wallet) {
        val generatedId = walletDao.insert(wallet)
        remoteDataSource.syncWallet(wallet.copy(id = generatedId))
    }
    
    override suspend fun updateWallet(wallet: Wallet) {
        val updatedWallet = wallet.copy(updatedAt = System.currentTimeMillis())
        walletDao.update(updatedWallet)
        remoteDataSource.syncWallet(updatedWallet)
    }
    
    override suspend fun deleteWallet(wallet: Wallet) {
        val deletedWallet = wallet.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
        walletDao.update(deletedWallet)
        remoteDataSource.syncWallet(deletedWallet)
    }
}