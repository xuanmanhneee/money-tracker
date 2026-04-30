package com.finflow.moneytracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.repository.CategoryRepository
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class BudgetProgressState {
    SAFE,
    WARNING,
    DANGER
}

data class BudgetCategoryProgressUi(
    val categoryId: String,
    val categoryName: String,
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
    val usagePercent: Int = 0,
    val progressState: BudgetProgressState = BudgetProgressState.SAFE,
    val alertMessage: String? = null,
    val comparisonMessage: String? = null,
    val categoryProgress: List<BudgetCategoryProgressUi> = emptyList()
)

class BudgetViewModel(
    walletRepository: WalletRepository,
    categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    companion object {
        private const val TYPE_EXPENSE = 0
    }

    private val currentDateFlow = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<BudgetUiState> = combine(
        currentDateFlow,
        walletRepository.getWalletsStream(),
        categoryRepository.getAllCategoriesStream(),
        transactionRepository.getAllTransactionsStream()
    ) { date, _, categories, transactions ->
        
        val (periodStart, periodEnd) = getMonthRange(date)
        
        val expenseCategories = categories.filter { it.type == TYPE_EXPENSE && !it.isDeleted }
        val expenseCategoryIds = expenseCategories.map { it.id }.toSet()

        // Lọc giao dịch chi tiêu trong tháng hiện tại
        val currentMonthTransactions = transactions.filter { tx ->
            !tx.isDeleted &&
            tx.date in periodStart..periodEnd &&
            tx.categoryId in expenseCategoryIds &&
            tx.amount < 0
        }

        val totalPlanned = expenseCategories.sumOf { it.monthlyBudgetLimit ?: 0L }
        
        if (totalPlanned == 0L) {
            return@combine BudgetUiState(
                isLoading = false,
                isEmpty = true,
                periodLabel = formatPeriodLabel(date)
            )
        }

        val totalSpent = currentMonthTransactions.sumOf { abs(it.amount) }
        val totalRemaining = totalPlanned - totalSpent
        val totalUsagePercent = percentage(totalSpent, totalPlanned)

        val spentByCategory = currentMonthTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { abs(it.amount) } }

        val categoryProgress = expenseCategories
            .filter { (it.monthlyBudgetLimit ?: 0L) > 0L }
            .map { category ->
                val allocated = category.monthlyBudgetLimit ?: 0L
                val spent = spentByCategory[category.id] ?: 0L
                val usage = percentage(spent, allocated)
                
                BudgetCategoryProgressUi(
                    categoryId = category.id.toString(),
                    categoryName = category.name,
                    allocated = allocated,
                    spent = spent,
                    remaining = allocated - spent,
                    usagePercent = usage,
                    progressState = mapUsageToState(usage)
                )
            }.sortedByDescending { it.spent }

        // Logic so sánh với tháng trước
        val (prevStart, prevEnd) = getPreviousMonthRange(periodStart)
        val prevSpent = transactions.filter { tx ->
            !tx.isDeleted &&
            tx.date in prevStart..prevEnd &&
            tx.categoryId in expenseCategoryIds &&
            tx.amount < 0
        }.sumOf { abs(it.amount) }

        val comparisonMessage = if (prevSpent > 0L) {
            val delta = totalSpent - prevSpent
            if (delta == 0L) "Chi tiêu bằng kỳ trước"
            else "Chi tiêu ${if (delta > 0) "tăng" else "giảm"} ${formatCurrency(abs(delta))} so với kỳ trước"
        } else null

        BudgetUiState(
            isLoading = false,
            isEmpty = false,
            periodLabel = formatPeriodLabel(date),
            plannedBudget = totalPlanned,
            spent = totalSpent,
            remaining = totalRemaining,
            usagePercent = totalUsagePercent,
            progressState = mapUsageToState(totalUsagePercent),
            alertMessage = buildAlertMessage(totalUsagePercent),
            comparisonMessage = comparisonMessage,
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

    private fun getPreviousMonthRange(currentPeriodStart: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentPeriodStart
            add(Calendar.MONTH, -1)
        }
        return getMonthRange(calendar.timeInMillis)
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
        return ((value * 100f) / total).toInt().coerceAtLeast(0)
    }

    private fun mapUsageToState(usagePercent: Int): BudgetProgressState {
        return when {
            usagePercent >= 100 -> BudgetProgressState.DANGER
            usagePercent >= 80 -> BudgetProgressState.WARNING
            else -> BudgetProgressState.SAFE
        }
    }

    private fun buildAlertMessage(usagePercent: Int): String? {
        return when {
            usagePercent >= 100 -> "Đã vượt ngân sách"
            usagePercent >= 80 -> "Sắp chạm hạn mức (80%)"
            else -> null
        }
    }

    private fun formatPeriodLabel(dateMillis: Long): String {
        return SimpleDateFormat("MM/yyyy", Locale("vi", "VN")).format(dateMillis)
    }

    private fun formatCurrency(value: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(value).replace("₫", "").trim() + " ₫"
    }
}
