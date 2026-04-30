package com.finflow.moneytracker.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.finflow.moneytracker.MoneyTrackerApplication
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first

class FirestoreSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        
        if (user == null || user.isAnonymous) {
            Log.d("SyncWorker", "User là Khách hoặc chưa đăng nhập. Bỏ qua đồng bộ Cloud.")
            return ListenableWorker.Result.success()
        }

        val container = (applicationContext as MoneyTrackerApplication).container
        val uid = user.uid
        
        return try {
            Log.d("SyncWorker", "Bắt đầu kéo dữ liệu từ Firebase xuống...")
            val remoteDataSource = container.remoteDataSource

            // 1. KÉO DỮ LIỆU TỪ FIREBASE XUỐNG (PULL)
            // Sử dụng DAO trực tiếp để tránh các side-effect của Repository (như tự động cập nhật số dư ví)
            
            val remoteWallets = remoteDataSource.fetchWallets()
            for (remoteWallet in remoteWallets) {
                container.walletDao.insert(remoteWallet)
            }

            val remoteCategories = remoteDataSource.fetchCategories()
            for (remoteCategory in remoteCategories) {
                container.categoryDao.insert(remoteCategory)
            }

            val remoteTransactions = remoteDataSource.fetchTransactions()
            for (remoteTx in remoteTransactions) {
                container.transactionDao.insert(remoteTx)
            }

            // 2. ĐẨY DỮ LIỆU LOCAL LÊN (PUSH)
            // Đảm bảo những gì mới tạo ở local (userId trống) sẽ được gán UID và đẩy lên Cloud
            Log.d("SyncWorker", "Bắt đầu đẩy dữ liệu Local lên Firebase...")
            
            val wallets = container.walletRepository.getWalletsStream().first()
            for (wallet in wallets) {
                if (wallet.userId.isEmpty() || wallet.userId == uid) {
                    val walletToSync = if (wallet.userId.isEmpty()) wallet.copy(userId = uid) else wallet
                    if (wallet.userId.isEmpty()) container.walletDao.update(walletToSync)
                    remoteDataSource.syncWallet(walletToSync)
                }
            }

            val categories = container.categoryRepository.getAllCategoriesStream().first()
            for (category in categories) {
                if (category.userId.isEmpty() || category.userId == uid) {
                    val categoryToSync = if (category.userId.isEmpty()) category.copy(userId = uid) else category
                    if (category.userId.isEmpty()) container.categoryDao.update(categoryToSync)
                    remoteDataSource.syncCategory(categoryToSync)
                }
            }

            val transactions = container.transactionRepository.getAllTransactionsStream().first()
            for (transaction in transactions) {
                if (transaction.userId.isEmpty() || transaction.userId == uid) {
                    val transactionToSync = if (transaction.userId.isEmpty()) transaction.copy(userId = uid) else transaction
                    if (transaction.userId.isEmpty()) container.transactionDao.update(transactionToSync)
                    remoteDataSource.syncTransaction(transactionToSync)
                }
            }
            
            Log.d("SyncWorker", "Đồng bộ hai chiều hoàn tất thành công.")
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Lỗi trong quá trình đồng bộ: ${e.message}")
            ListenableWorker.Result.retry()
        }
    }
}
