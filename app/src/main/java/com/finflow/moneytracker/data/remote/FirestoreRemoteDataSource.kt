package com.finflow.moneytracker.data.remote

import android.util.Log
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.local.entity.Wallet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreRemoteDataSource(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RemoteDataSource {
    private val TAG = "FirestoreRemote"

    private fun getUserId(): String? = auth.currentUser?.uid

    override suspend fun syncWallet(wallet: Wallet) = withContext(NonCancellable) {
        val uid = getUserId() ?: return@withContext
        try {
            val data = hashMapOf(
                "id" to wallet.id,
                "userId" to uid,
                "name" to wallet.name,
                "balance" to wallet.balance,
                "isDefault" to wallet.isDefault,
                "isDeleted" to wallet.isDeleted,
                "updatedAt" to wallet.updatedAt
            )
            firestore.collection("users").document(uid).collection("wallets")
                .document(wallet.id.toString()).set(data).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing wallet: ${e.message}")
        }
    }

    override suspend fun syncCategory(category: Category) = withContext(NonCancellable) {
        val uid = getUserId() ?: return@withContext
        try {
            val data = hashMapOf(
                "id" to category.id,
                "userId" to uid,
                "name" to category.name,
                "type" to category.type,
                "icon" to category.icon,
                "monthlyBudgetLimit" to category.monthlyBudgetLimit,
                "isDefault" to category.isDefault,
                "isDeleted" to category.isDeleted,
                "updatedAt" to category.updatedAt
            )
            firestore.collection("users").document(uid).collection("categories")
                .document(category.id.toString()).set(data).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing category: ${e.message}")
        }
    }

    override suspend fun syncTransaction(transaction: Transaction) = withContext(NonCancellable) {
        val uid = getUserId() ?: return@withContext
        try {
            val data = hashMapOf(
                "id" to transaction.id,
                "userId" to uid,
                "walletId" to transaction.walletId,
                "categoryId" to transaction.categoryId,
                "amount" to transaction.amount,
                "date" to transaction.date,
                "note" to transaction.note,
                "toWalletId" to transaction.toWalletId,
                "isDeleted" to transaction.isDeleted,
                "updatedAt" to transaction.updatedAt
            )
            firestore.collection("users").document(uid).collection("transactions")
                .document(transaction.id.toString()).set(data).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing transaction: ${e.message}")
        }
    }

    override suspend fun fetchWallets(): List<Wallet> {
        val uid = getUserId() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(uid).collection("wallets").get().await()
            snapshot.documents.map { doc ->
                Wallet(
                    id = doc.getLong("id") ?: 0L,
                    userId = doc.getString("userId") ?: uid,
                    name = doc.getString("name") ?: "",
                    balance = doc.getLong("balance") ?: 0L,
                    isDefault = doc.getBoolean("isDefault") ?: false,
                    isDeleted = doc.getBoolean("isDeleted") ?: false,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wallets: ${e.message}")
            emptyList()
        }
    }

    override suspend fun fetchCategories(): List<Category> {
        val uid = getUserId() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(uid).collection("categories").get().await()
            snapshot.documents.map { doc ->
                Category(
                    id = doc.getLong("id") ?: 0L,
                    userId = doc.getString("userId") ?: uid,
                    name = doc.getString("name") ?: "",
                    type = doc.getLong("type")?.toInt() ?: 0,
                    icon = doc.getString("icon") ?: "",
                    monthlyBudgetLimit = doc.getLong("monthlyBudgetLimit"),
                    isDefault = doc.getBoolean("isDefault") ?: false,
                    isDeleted = doc.getBoolean("isDeleted") ?: false,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories: ${e.message}")
            emptyList()
        }
    }

    override suspend fun fetchTransactions(): List<Transaction> {
        val uid = getUserId() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(uid).collection("transactions").get().await()
            snapshot.documents.map { doc ->
                Transaction(
                    id = doc.getLong("id") ?: 0L,
                    userId = doc.getString("userId") ?: uid,
                    walletId = doc.getLong("walletId") ?: 0L,
                    categoryId = doc.getLong("categoryId") ?: 0L,
                    amount = doc.getLong("amount") ?: 0L,
                    date = doc.getLong("date") ?: 0L,
                    note = doc.getString("note"),
                    toWalletId = doc.getLong("toWalletId"),
                    isDeleted = doc.getBoolean("isDeleted") ?: false,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions: ${e.message}")
            emptyList()
        }
    }
}
