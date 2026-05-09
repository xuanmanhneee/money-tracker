package com.finflow.moneytracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finflow.moneytracker.data.local.model.CategoryType
import com.finflow.moneytracker.data.repository.CategoryRepository
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository
import com.finflow.moneytracker.ui.transactions.WalletSnapshot.Active
import com.finflow.moneytracker.ui.transactions.WalletSnapshot.Unknown
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.Long

sealed class CategorySnapshot {
    data object Deleted : CategorySnapshot()
    data class Active(
        val name: String,
        val icon: String,
        val type: CategoryType
    ) : CategorySnapshot()
}

sealed class WalletSnapshot {
    data object Unknown : WalletSnapshot()

    data class Active(
        val id: Long,
        val name: String
    ) : WalletSnapshot()
}

fun WalletSnapshot.displayName(): String {
    return when (this) {
        is Active -> name
        Unknown -> "Không xác định"
    }
}

data class TransactionUiModel(
    val id: Long,
    val amount: Long,
    val date: Long,
    val note: String?,
    val category: CategorySnapshot,
    val wallet: WalletSnapshot
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    val transactionsUiState: StateFlow<List<TransactionUiModel>> =
        combine(
            transactionRepository.getAllTransactionsStream(),
            categoryRepository.getAllCategoriesStream(),
            walletRepository.getWalletsStream()
        ) { transactions, categories, wallets ->

            val categoryById = categories.associateBy { it.id }
            val walletById = wallets.associateBy { it.id }

            transactions.map { transaction ->

                // Category snapshot
                val categorySnapshot = categoryById[transaction.categoryId]?.let { category ->
                    CategorySnapshot.Active(
                        name = category.name,
                        icon = category.icon,
                        type = category.type
                    )
                } ?: CategorySnapshot.Deleted

                // Wallet snapshot
                val walletSnapshot = walletById[transaction.walletId]?.let { wallet ->
                    WalletSnapshot.Active(
                        id = wallet.id,
                        name = wallet.name
                    )
                } ?: WalletSnapshot.Unknown

                TransactionUiModel(
                    id = transaction.id,
                    amount = transaction.amount,
                    date = transaction.date,
                    note = transaction.note,
                    category = categorySnapshot,
                    wallet = walletSnapshot
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )
}