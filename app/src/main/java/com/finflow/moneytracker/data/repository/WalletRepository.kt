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

    // ĐỌC: Luôn luôn lấy từ DAO. Room sẽ tự động phát (emit) dữ liệu mới khi DB thay đổi.
    override fun getWalletsStream(): Flow<List<Wallet>> = walletDao.getAll()

    override fun getWalletStream(id: Long): Flow<Wallet?> = walletDao.getById(id)

    // GHI: Local trước, Remote sau
    override suspend fun insertWallet(wallet: Wallet) {
        // 1. Ghi vào Local và lấy ID mới
        val generatedId = walletDao.insert(wallet)

        // 2. Cố gắng đồng bộ lên Remote
        try {
            remoteDataSource.syncWallet(wallet.copy(id = generatedId))
        } catch (e: Exception) {
            // Chỉ log lỗi sync, không làm app crash hay chặn UI cập nhật
            android.util.Log.e("SyncError", "Không thể đồng bộ lên Cloud: ${e.message}")
        }
    }

    override suspend fun updateWallet(wallet: Wallet) {
        val updatedWallet = wallet.copy(updatedAt = System.currentTimeMillis())
        // Cập nhật Local
        walletDao.update(updatedWallet)

        // Cập nhật Remote
        try {
            remoteDataSource.syncWallet(updatedWallet)
        } catch (e: Exception) {
            android.util.Log.e("SyncError", "Sync update failed")
        }
    }

    override suspend fun deleteWallet(wallet: Wallet) {
        // Soft delete: Đánh dấu xóa ở local
        val deletedWallet = wallet.copy(isDeleted = true, updatedAt = System.currentTimeMillis())
        walletDao.update(deletedWallet)

        try {
            remoteDataSource.syncWallet(deletedWallet)
        } catch (e: Exception) {
            android.util.Log.e("SyncError", "Sync delete failed")
        }
    }
}