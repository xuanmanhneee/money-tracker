package com.finflow.moneytracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finflow.moneytracker.data.local.model.CategoryType
import com.finflow.moneytracker.data.repository.CategoryRepository
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.Normalizer
import java.util.Calendar

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
        is WalletSnapshot.Active -> name
        WalletSnapshot.Unknown -> "Không xác định"
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

sealed class TransactionFilter {
    data object Today : TransactionFilter()
    data object ThisWeek : TransactionFilter()
    data object ThisMonth : TransactionFilter()
    data object ThisYear : TransactionFilter()

    data class CustomRange(
        val startMillis: Long,
        val endMillis: Long
    ) : TransactionFilter()
}

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<TransactionFilter>(TransactionFilter.ThisMonth)
    private val searchQuery = MutableStateFlow("")

    val transactionsUiState: StateFlow<List<TransactionUiModel>> =
        combine(
            transactionRepository.getAllTransactionsStream(),
            categoryRepository.getAllCategoriesStream(),
            walletRepository.getWalletsStream(),
            selectedFilter,
            searchQuery
        ) { transactions, categories, wallets, filter, query ->

            val categoryById = categories.associateBy { it.id }
            val walletById = wallets.associateBy { it.id }
            val normalizedQuery = query.normalizeForSearch()

            transactions
                .filter { matchesFilter(it.date, filter) }
                .filter { transaction ->
                    normalizedQuery.isBlank() ||
                            transaction.note
                                .orEmpty()
                                .normalizeForSearch()
                                .contains(normalizedQuery)
                }
                .sortedByDescending { it.date }
                .map { transaction ->

                    val categorySnapshot =
                        categoryById[transaction.categoryId]?.let { category ->
                            CategorySnapshot.Active(
                                name = category.name,
                                icon = category.icon,
                                type = category.type
                            )
                        } ?: CategorySnapshot.Deleted

                    val walletSnapshot =
                        walletById[transaction.walletId]?.let { wallet ->
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

    fun filterToday() {
        selectedFilter.value = TransactionFilter.Today
    }

    fun filterThisWeek() {
        selectedFilter.value = TransactionFilter.ThisWeek
    }

    fun filterThisMonth() {
        selectedFilter.value = TransactionFilter.ThisMonth
    }

    fun filterThisYear() {
        selectedFilter.value = TransactionFilter.ThisYear
    }

    fun filterCustomRange(startMillis: Long, endMillis: Long) {
        selectedFilter.value = TransactionFilter.CustomRange(
            startMillis = startOfDay(startMillis),
            endMillis = endOfDay(endMillis)
        )
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    private fun matchesFilter(
        transactionTime: Long,
        filter: TransactionFilter
    ): Boolean {
        val now = Calendar.getInstance()

        val transactionCalendar = Calendar.getInstance().apply {
            timeInMillis = transactionTime
        }

        return when (filter) {
            TransactionFilter.Today -> {
                now.get(Calendar.YEAR) == transactionCalendar.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == transactionCalendar.get(Calendar.DAY_OF_YEAR)
            }

            TransactionFilter.ThisWeek -> {
                now.get(Calendar.YEAR) == transactionCalendar.get(Calendar.YEAR) &&
                        now.get(Calendar.WEEK_OF_YEAR) == transactionCalendar.get(Calendar.WEEK_OF_YEAR)
            }

            TransactionFilter.ThisMonth -> {
                now.get(Calendar.YEAR) == transactionCalendar.get(Calendar.YEAR) &&
                        now.get(Calendar.MONTH) == transactionCalendar.get(Calendar.MONTH)
            }

            TransactionFilter.ThisYear -> {
                now.get(Calendar.YEAR) == transactionCalendar.get(Calendar.YEAR)
            }

            is TransactionFilter.CustomRange -> {
                transactionTime in filter.startMillis..filter.endMillis
            }
        }
    }

    private fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun endOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}

private fun String.normalizeForSearch(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("đ", "d")
        .replace("Đ", "D")
        .lowercase()
        .trim()
}