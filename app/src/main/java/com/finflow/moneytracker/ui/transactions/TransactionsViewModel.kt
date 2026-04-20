package com.finflow.moneytracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.repository.CategoryRepository
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TransactionUiItem(
    val transaction: Transaction,
    val categoryName: String,
    val categoryIcon: String,
    val categoryType: Int,
    val paymentMethodLabel: String
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    // Lấy Stream từ Repo và biến nó thành StateFlow để UI dễ dàng đọc được
    val transactionsUiState: StateFlow<List<TransactionUiItem>> =
        combine(
            transactionRepository.getAllTransactionsStream(),
            categoryRepository.getAllCategoriesStream(),
            walletRepository.getWalletsStream()
        ) { transactions, categories, wallets ->
            val categoryById = categories.associateBy { it.id }
            val walletById = wallets.associateBy { it.id }
            transactions.map { transaction ->
                val category = categoryById[transaction.categoryId]
                val walletName = walletById[transaction.walletId]?.name.orEmpty().lowercase()
                val paymentMethodLabel = when {
                    walletName.contains("ngân hàng") || walletName.contains("bank") -> "Ngân hàng"
                    else -> "Tiền mặt"
                }

                TransactionUiItem(
                    transaction = transaction,
                    categoryName = category?.name ?: "Danh mục đã xóa",
                    categoryIcon = category?.icon ?: "ic_category",
                    categoryType = category?.type ?: 0,
                    paymentMethodLabel = paymentMethodLabel
                )
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList() // Ban đầu danh sách rỗng
            )
}