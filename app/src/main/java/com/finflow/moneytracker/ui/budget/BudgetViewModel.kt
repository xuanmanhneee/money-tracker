package com.finflow.moneytracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.model.CategoryType
import com.finflow.moneytracker.data.repository.CategoryRepository
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BudgetProgressState {
    SAFE,
    WARNING,
    DANGER
}

data class BudgetCategoryProgressUi(
    val categoryId: Long,
    val categoryName: String,
    val iconEmoji: String?,
    val allocated: Long,
    val spent: Long,
    val remaining: Long,
    val usagePercent: Int,
    val progressState: BudgetProgressState
)

data class BudgetUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val periodLabel: String = "",
    val plannedBudget: Long = 0L,
    val spent: Long = 0L,
    val remaining: Long = 0L,
    val savingAmount: Long = 0L,
    val overspentAmount: Long = 0L,
    val usagePercent: Int = 0,
    val progressState: BudgetProgressState = BudgetProgressState.SAFE,
    val alertMessage: String? = null,
    val categoryProgress: List<BudgetCategoryProgressUi> = emptyList()
)

class BudgetViewModel(
    walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val currentDateFlow = MutableStateFlow(System.currentTimeMillis())

    private var latestCategories: List<Category> = emptyList()

    val uiState: StateFlow<BudgetUiState> = combine(
        currentDateFlow,
        walletRepository.getWalletsStream(),
        categoryRepository.getAllCategoriesStream(),
        transactionRepository.getAllTransactionsStream()
    ) { date, _, categories, transactions ->

        latestCategories = categories

        val (periodStart, periodEnd) = getMonthRange(date)

        val expenseCategories = categories.filter { category ->
            category.type == CategoryType.EXPENSE && !category.isDeleted
        }

        val limitedCategories = expenseCategories.filter { category ->
            category.monthlyBudgetLimit != null
        }

        val expenseCategoryIds = expenseCategories
            .map { it.id }
            .toSet()

        val currentMonthTransactions = transactions.filter { tx ->
            !tx.isDeleted &&
                    tx.date in periodStart..periodEnd &&
                    tx.categoryId in expenseCategoryIds &&
                    tx.toWalletId == null
        }

        val spentByCategory = currentMonthTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, txs) ->
                txs.sumOf { abs(it.amount) }
            }

        val categoryProgress = limitedCategories
            .map { category ->
                val allocated = category.monthlyBudgetLimit ?: 0L
                val spent = spentByCategory[category.id] ?: 0L
                val rawRemaining = allocated - spent
                val usage = percentage(spent, allocated)

                BudgetCategoryProgressUi(
                    categoryId = category.id,
                    categoryName = category.name,
                    iconEmoji = category.icon,
                    allocated = allocated,
                    spent = spent,
                    remaining = rawRemaining,
                    usagePercent = usage,
                    progressState = mapUsageToState(usage)
                )
            }
            .sortedByDescending { it.spent }

        val totalPlanned = categoryProgress.sumOf { it.allocated }
        val totalSpent = categoryProgress.sumOf { it.spent }
        val rawRemaining = totalPlanned - totalSpent
        val totalUsagePercent = percentage(totalSpent, totalPlanned)

        BudgetUiState(
            isLoading = false,
            isEmpty = categoryProgress.isEmpty(),
            periodLabel = formatPeriodLabel(date),
            plannedBudget = totalPlanned,
            spent = totalSpent,
            remaining = rawRemaining,
            savingAmount = rawRemaining.coerceAtLeast(0L),
            overspentAmount = (-rawRemaining).coerceAtLeast(0L),
            usagePercent = totalUsagePercent,
            progressState = mapUsageToState(totalUsagePercent),
            alertMessage = buildAlertMessage(rawRemaining, totalUsagePercent),
            categoryProgress = categoryProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = BudgetUiState()
    )

    fun refreshCurrentPeriod() {
        currentDateFlow.value = System.currentTimeMillis()
    }

    fun getAvailableCategories(): List<Category> {
        return latestCategories.filter { category ->
            category.type == CategoryType.EXPENSE &&
                    category.monthlyBudgetLimit == null &&
                    !category.isDeleted
        }
    }

    fun setCategoryBudgetLimit(categoryId: Long, limitAmount: Long) {
        if (limitAmount <= 0L) return

        viewModelScope.launch {
            categoryRepository.updateMonthlyBudgetLimit(
                categoryId = categoryId,
                limit = limitAmount
            )
        }
    }

    fun removeCategoryBudgetLimit(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.updateMonthlyBudgetLimit(
                categoryId = categoryId,
                limit = null
            )
        }
    }

    private fun getMonthRange(timestamp: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)

        return start to calendar.timeInMillis
    }

    private fun percentage(value: Long, total: Long): Int {
        if (total <= 0L) return 0
        return ((value * 100f) / total).toInt()
    }

    private fun mapUsageToState(usagePercent: Int): BudgetProgressState {
        return when {
            usagePercent >= 100 -> BudgetProgressState.DANGER
            usagePercent >= 80 -> BudgetProgressState.WARNING
            else -> BudgetProgressState.SAFE
        }
    }

    private fun buildAlertMessage(
        remaining: Long,
        usagePercent: Int
    ): String? {
        return when {
            remaining < 0L -> "Đã dùng lố ${abs(remaining)} so với mức đã đặt"
            usagePercent >= 80 -> "Cảnh báo: Chi tiêu sắp chạm hạn mức"
            else -> "Đang tiết kiệm được $remaining so với mức đã đặt"
        }
    }

    private fun formatPeriodLabel(dateMillis: Long): String {
        return SimpleDateFormat("MM/yyyy", Locale("vi", "VN")).format(dateMillis)
    }
}