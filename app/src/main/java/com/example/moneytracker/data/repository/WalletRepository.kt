package com.example.moneytracker.data.repository

import com.example.moneytracker.data.local.dao.WalletDao
import com.example.moneytracker.data.local.entity.Wallet
import kotlinx.coroutines.flow.Flow

// Interface
interface WalletRepository {
    fun getWalletsStream(): Flow<List<Wallet>>
    fun getWalletStream(id: Int): Flow<Wallet?>
    suspend fun insertWallet(wallet: Wallet)
    suspend fun updateWallet(wallet: Wallet)
}

// Implementation
class OfflineWalletRepository(private val walletDao: WalletDao) : WalletRepository {
    override fun getWalletsStream(): Flow<List<Wallet>> = walletDao.getAll()
    override fun getWalletStream(id: Int): Flow<Wallet?> = walletDao.getById(id)
    override suspend fun insertWallet(wallet: Wallet) = walletDao.insert(wallet)
    override suspend fun updateWallet(wallet: Wallet) = walletDao.update(wallet)
}