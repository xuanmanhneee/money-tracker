package com.finflow.moneytracker.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.finflow.moneytracker.MoneyTrackerApplication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class FirestoreSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        
        // KIỂM TRA QUYỀN RIÊNG TƯ: 
        // 1. Nếu chưa đăng nhập 
        // 2. HOẶC đang là tài khoản ẩn danh (Guest)
        // -> THÌ không đồng bộ lên Cloud, chỉ lưu Local.
        if (user == null || user.isAnonymous) {
            Log.d("SyncWorker", "User là Khách hoặc chưa đăng nhập. Bỏ qua đồng bộ Cloud để bảo mật.")
            return ListenableWorker.Result.success()
        }

        val db = FirebaseFirestore.getInstance()
        val repository = (applicationContext as MoneyTrackerApplication).container.transactionRepository
        
        return try {
            val transactions = repository.getAllTransactionsStream().first()
            
            for (transaction in transactions) {
                // Chỉ đồng bộ dữ liệu của chính User này và chỉ khi User đã đăng nhập chính thức
                if (transaction.userId == user.uid) {
                    db.collection("users")
                        .document(user.uid)
                        .collection("transactions")
                        .document(transaction.id)
                        .set(transaction)
                        .await()
                }
            }
            
            Log.d("SyncWorker", "Đã đồng bộ ${transactions.size} giao dịch lên Firestore cho User: ${user.email}")
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Lỗi đồng bộ: ${e.message}")
            ListenableWorker.Result.retry()
        }
    }
}