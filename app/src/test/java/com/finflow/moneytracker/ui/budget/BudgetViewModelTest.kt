//package com.finflow.moneytracker.ui.budget
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.finflow.moneytracker.data.local.entity.Category
//import com.finflow.moneytracker.data.local.entity.Transaction
//import com.finflow.moneytracker.data.local.model.CategoryType
//import com.finflow.moneytracker.data.repository.CategoryRepository
//import com.finflow.moneytracker.data.repository.TransactionRepository
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Locale
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import kotlin.math.abs
//
//class BudgetViewModel(
//    private val categoryRepository: CategoryRepository,
//    private val transactionRepository: TransactionRepository
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(BudgetUiState())
//    val uiState: StateFlow<BudgetUiState> = _uiState
//
//    private var latestCategories: List<Category> = emptyList()
//
//    init {
//        observeBudget()
//    }
//
//    private fun observeBudget() {
//        val now = System.currentTimeMillis()
//        val monthRange = getCurrentMonthRange(now)
//
//        viewModelScope.launch {
//            combine(
//                categoryRepository.getAllCategoriesStream(),
//                transactionRepository.getTransactionsByDateRangeStream(
//                    monthRange.start,
//                    monthRange.end
//                )
//            ) { categories, transactions ->
//
//                latestCategories = categories
//
//                buildUiState(
//                    categories = categories,
//                    transactions = transactions,
//                    monthStart = monthRange.start
//                )
//            }.collect { state ->
//                _uiState.value = state
//            }
//        }
//    }
//
//    fun getAvailableCategories(): List<Category> {
//        return latestCategories.filter { category ->
//            category.type == CategoryType.EXPENSE &&
//                    category.monthlyBudgetLimit == null &&
//                    !category.isDeleted
//        }
//    }
//
//    fun setCategoryBudgetLimit(categoryId: Long, limitAmount: Long) {
//        if (limitAmount <= 0L) return
//
//        viewModelScope.launch {
//            categoryRepository.updateMonthlyBudgetLimit(
//                categoryId = categoryId,
//                limit = limitAmount
//            )
//        }
//    }
//
//    fun removeCategoryBudgetLimit(categoryId: Long) {
//        viewModelScope.launch {
//            categoryRepository.updateMonthlyBudgetLimit(
//                categoryId = categoryId,
//                limit = null
//            )
//        }
//    }
//
//    fun refreshCurrentPeriod() {
//        // Không cần làm gì nếu đang observe theo Flow.
//        // Giữ hàm này nếu Fragment đang gọi onResume().
//    }
//
//    private fun buildUiState(
//        categories: List<Category>,
//        transactions: List<Transaction>,
//        monthStart: Long
//    ): BudgetUiState {
//        val expenseTransactions = transactions.filter { it.amount < 0 }
//
//        val spentByCategory = expenseTransactions
//            .groupBy { it.categoryId }
//            .mapValues { (_, txs) ->
//                txs.sumOf { abs(it.amount) }
//            }
//
//        val progress = categories
//            .filter { category ->
//                category.type == CategoryType.EXPENSE &&
//                        category.monthlyBudgetLimit != null &&
//                        !category.isDeleted
//            }
//            .map { category ->
//                val limit = category.monthlyBudgetLimit ?: 0L
//                val spent = spentByCategory[category.id] ?: 0L
//                val remaining = limit - spent
//
//                BudgetCategoryProgressUi(
//                    categoryId = category.id,
//                    categoryName = category.name,
//                    icon = category.icon,
//                    limitAmount = limit,
//                    spentAmount = spent,
//                    remainingAmount = remaining,
//                    usagePercent = if (limit > 0L) {
//                        ((spent * 100) / limit).toInt()
//                    } else {
//                        0
//                    }
//                )
//            }
//
//        val totalLimit = progress.sumOf { it.limitAmount }
//        val totalSpent = progress.sumOf { it.spentAmount }
//        val totalRemaining = totalLimit - totalSpent
//
//        return BudgetUiState(
//            isLoading = false,
//            periodLabel = formatMonthLabel(monthStart),
//            plannedBudget = totalLimit,
//            spent = totalSpent,
//            remaining = totalRemaining,
//            usagePercent = if (totalLimit > 0L) {
//                ((totalSpent * 100) / totalLimit).toInt()
//            } else {
//                0
//            },
//            savingAmount = totalRemaining.coerceAtLeast(0L),
//            overspentAmount = (-totalRemaining).coerceAtLeast(0L),
//            categoryProgress = progress
//        )
//    }
//
//    private fun getCurrentMonthRange(now: Long): MonthRange {
//        val calendar = Calendar.getInstance().apply {
//            timeInMillis = now
//            set(Calendar.DAY_OF_MONTH, 1)
//            set(Calendar.HOUR_OF_DAY, 0)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//
//        val start = calendar.timeInMillis
//
//        calendar.add(Calendar.MONTH, 1)
//        calendar.add(Calendar.MILLISECOND, -1)
//
//        return MonthRange(
//            start = start,
//            end = calendar.timeInMillis
//        )
//    }
//
//    private fun formatMonthLabel(timestamp: Long): String {
//        return SimpleDateFormat("MM/yyyy", Locale("vi", "VN"))
//            .format(timestamp)
//    }
//}
//
//private data class MonthRange(
//    val start: Long,
//    val end: Long
//)