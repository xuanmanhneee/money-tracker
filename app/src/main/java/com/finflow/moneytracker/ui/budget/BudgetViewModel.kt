package com.finflow.moneytracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

// --- UI Models ---

enum class BudgetProgressState {
    SAFE,
    WARNING,
    DANGER
}

data class BudgetCategoryProgressUi(
    val categoryId: String,
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
    val usagePercent: Int = 0,
    val progressState: BudgetProgressState = BudgetProgressState.SAFE,
    val alertMessage: String? = null,
    val categoryProgress: List<BudgetCategoryProgressUi> = emptyList()
)

// --- ViewModel ---

class BudgetViewModel(
    walletRepository: WalletRepository,
    categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val currentDateFlow = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<BudgetUiState> = combine(
        currentDateFlow,
        walletRepository.getWalletsStream(),
        categoryRepository.getAllCategoriesStream(),
        transactionRepository.getAllTransactionsStream()
    ) { date, _, categories, transactions ->

        val (periodStart, periodEnd) = getMonthRange(date)

        // Lọc các danh mục chi tiêu hợp lệ
        val expenseCategories = categories.filter {
            it.type == CategoryType.EXPENSE && !it.isDeleted
        }
        val expenseCategoryIds = expenseCategories.map { it.id }.toSet()

        // Lọc giao dịch trong tháng của các danh mục chi tiêu
        val currentMonthTransactions = transactions.filter { tx ->
            !tx.isDeleted &&
                    tx.date in periodStart..periodEnd &&
                    tx.categoryId in expenseCategoryIds &&
                    tx.toWalletId == null // Đảm bảo không phải giao dịch chuyển khoản
        }

        // Tính tổng hạn mức (planned) và tổng đã chi (spent)
        val totalPlanned = expenseCategories.sumOf { it.monthlyBudgetLimit ?: 0L }
        val totalSpent = currentMonthTransactions.sumOf { abs(it.amount) }

        // Tính chi tiêu theo từng Category ID
        val spentByCategory = currentMonthTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { abs(it.amount) } }

        // Mapping danh sách tiến độ từng hạng mục (Hiển thị tất cả, không filter hạn mức > 0)
        val categoryProgress = expenseCategories.map { category ->
            val allocated = category.monthlyBudgetLimit ?: 0L
            val spent = spentByCategory[category.id] ?: 0L
            val usage = percentage(spent, allocated)

            BudgetCategoryProgressUi(
                categoryId = category.id.toString(),
                categoryName = category.name,
                iconEmoji = category.icon,   // ← thêm dòng này, tên field tùy entity của bạn
                allocated = allocated,
                spent = spent,
                remaining = (allocated - spent).coerceAtLeast(0L),
                usagePercent = usage,
                progressState = mapUsageToState(usage)
            )
        }.sortedByDescending { it.spent } // Ưu tiên hiện mục tiêu nhiều tiền lên đầu

        // Tính toán các thông số tổng quát
        val totalRemaining = (totalPlanned - totalSpent).coerceAtLeast(0L)
        val totalUsagePercent = percentage(totalSpent, totalPlanned)

        BudgetUiState(
            isLoading = false,
            isEmpty = categoryProgress.isEmpty(),
            periodLabel = formatPeriodLabel(date),
            plannedBudget = totalPlanned,
            spent = totalSpent,
            remaining = totalRemaining,
            usagePercent = totalUsagePercent,
            progressState = mapUsageToState(totalUsagePercent),
            alertMessage = buildAlertMessage(totalUsagePercent),
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

    // --- Helper Functions ---

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

    private fun buildAlertMessage(usagePercent: Int): String? {
        return when {
            usagePercent >= 100 -> "Đã vượt ngân sách tháng"
            usagePercent >= 80 -> "Cảnh báo: Chi tiêu sắp chạm hạn mức"
            else -> null
        }
    }

    private fun formatPeriodLabel(dateMillis: Long): String {
        return SimpleDateFormat("MM/yyyy", Locale("vi", "VN")).format(dateMillis)
    }
}