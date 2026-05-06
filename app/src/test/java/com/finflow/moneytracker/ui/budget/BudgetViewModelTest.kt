package com.finflow.moneytracker.ui.budget

import com.finflow.moneytracker.data.local.entity.BudgetCycleType
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.local.entity.Wallet
import com.finflow.moneytracker.data.repository.BudgetRepository
import com.finflow.moneytracker.data.repository.CategoryRepository
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_spent_onlyCountsTransactionsFromBudgetWallet() = runTest {
        val walletA = Wallet(id = 1L, name = "Wallet A")
        val walletB = Wallet(id = 2L, name = "Wallet B")
        val expenseCategory = Category(
            id = 10L,
            name = "Food",
            type = 0,
            icon = "ic_food",
            monthlyBudgetLimit = 100_000L
        )

        val (start, end) = monthRange(System.currentTimeMillis())
        val budget = Budget(
            id = "budget-1",
            walletId = walletA.id,
            name = "Budget",
            cycleType = BudgetCycleType.MONTHLY,
            periodStart = start,
            periodEnd = end,
            plannedAmount = 100_000L
        )

        val transactions = listOf(
            Transaction(
                id = 1L,
                walletId = walletA.id,
                categoryId = expenseCategory.id,
                amount = -1_000L,
                date = start + 1_000L
            ),
            Transaction(
                id = 2L,
                walletId = walletB.id,
                categoryId = expenseCategory.id,
                amount = -9_000L,
                date = start + 2_000L
            )
        )

        val budgetRepository = FakeBudgetRepository(
            budgets = listOf(budget),
            allocations = listOf(
                BudgetAllocation(
                    id = "alloc-1",
                    budgetId = budget.id,
                    categoryId = expenseCategory.id,
                    allocatedAmount = 100_000L
                )
            )
        )

        val viewModel = BudgetViewModel(
            budgetRepository = budgetRepository,
            walletRepository = FakeWalletRepository(listOf(walletA, walletB)),
            categoryRepository = FakeCategoryRepository(listOf(expenseCategory)),
            transactionRepository = FakeTransactionRepository(transactions)
        )

        val state = awaitLoadedState(viewModel)

        assertEquals(1_000L, state.spent)
        assertEquals(99_000L, state.remaining)
        assertEquals(1, state.categoryProgress.size)
        assertEquals(1_000L, state.categoryProgress.first().spent)
    }

    @Test
    fun editCurrentBudget_rebalancesExistingAllocationsByRatio() = runTest {
        val wallet = Wallet(id = 1L, name = "Main")
        val catFood = Category(
            id = 10L,
            name = "Food",
            type = 0,
            icon = "ic_food",
            monthlyBudgetLimit = 100_000L
        )
        val catTransport = Category(
            id = 11L,
            name = "Transport",
            type = 0,
            icon = "ic_transport",
            monthlyBudgetLimit = 200_000L
        )

        val (start, end) = monthRange(System.currentTimeMillis())
        val budget = Budget(
            id = "budget-main",
            walletId = wallet.id,
            name = "Budget",
            cycleType = BudgetCycleType.MONTHLY,
            periodStart = start,
            periodEnd = end,
            plannedAmount = 300_000L
        )

        val budgetRepository = FakeBudgetRepository(
            budgets = listOf(budget),
            allocations = listOf(
                BudgetAllocation(
                    id = "alloc-1",
                    budgetId = budget.id,
                    categoryId = catFood.id,
                    allocatedAmount = 100_000L
                ),
                BudgetAllocation(
                    id = "alloc-2",
                    budgetId = budget.id,
                    categoryId = catTransport.id,
                    allocatedAmount = 200_000L
                )
            )
        )

        val viewModel = BudgetViewModel(
            budgetRepository = budgetRepository,
            walletRepository = FakeWalletRepository(listOf(wallet)),
            categoryRepository = FakeCategoryRepository(listOf(catFood, catTransport)),
            transactionRepository = FakeTransactionRepository(emptyList())
        )

        awaitLoadedState(viewModel)
        viewModel.editCurrentBudget(900_000L)
        advanceUntilIdle()

        val updatedBudget = budgetRepository.currentBudgetForDate(BudgetCycleType.MONTHLY, System.currentTimeMillis())
        val updatedAllocations = budgetRepository.currentAllocationsForBudget(budget.id)
            .sortedBy { it.categoryId }

        assertEquals(900_000L, updatedBudget?.plannedAmount)
        assertTrue(updatedBudget?.manualOverride == true)
        assertEquals(900_000L, updatedAllocations.sumOf { it.allocatedAmount })

        val foodAmount = updatedAllocations.first { it.categoryId == catFood.id }.allocatedAmount
        val transportAmount = updatedAllocations.first { it.categoryId == catTransport.id }.allocatedAmount
        assertEquals(300_000L, foodAmount)
        assertEquals(600_000L, transportAmount)
    }

    @Test
    fun uiState_comparisonMessage_isBuiltFromPreviousMonthTransactions() = runTest {
        val wallet = Wallet(id = 1L, name = "Main")
        val expenseCategory = Category(
            id = 10L,
            name = "Food",
            type = 0,
            icon = "ic_food",
            monthlyBudgetLimit = 100_000L
        )

        val now = System.currentTimeMillis()
        val (currentStart, currentEnd) = monthRange(now)
        val (previousStart, previousEnd) = monthRange(
            Calendar.getInstance().run {
                timeInMillis = currentStart
                add(Calendar.MONTH, -1)
                timeInMillis
            }
        )

        val budget = Budget(
            id = "budget-main",
            walletId = wallet.id,
            name = "Budget",
            cycleType = BudgetCycleType.MONTHLY,
            periodStart = currentStart,
            periodEnd = currentEnd,
            plannedAmount = 100_000L
        )

        val transactions = listOf(
            Transaction(
                id = 1L,
                walletId = wallet.id,
                categoryId = expenseCategory.id,
                amount = -4_000L,
                date = currentStart + 5_000L
            ),
            Transaction(
                id = 2L,
                walletId = wallet.id,
                categoryId = expenseCategory.id,
                amount = -2_500L,
                date = previousEnd - 5_000L
            )
        )

        val viewModel = BudgetViewModel(
            budgetRepository = FakeBudgetRepository(budgets = listOf(budget)),
            walletRepository = FakeWalletRepository(listOf(wallet)),
            categoryRepository = FakeCategoryRepository(listOf(expenseCategory)),
            transactionRepository = FakeTransactionRepository(transactions)
        )

        val state = awaitLoadedState(viewModel)

        assertTrue(state.comparisonMessage?.contains("so với kỳ trước") == true)
        assertTrue(state.comparisonMessage?.contains("tăng") == true)
    }

    private suspend fun awaitLoadedState(viewModel: BudgetViewModel): BudgetUiState {
        return viewModel.uiState.first { !it.isLoading }
    }

    private fun monthRange(timestamp: Long): Pair<Long, Long> {
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
        val end = calendar.timeInMillis
        return start to end
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeBudgetRepository(
    budgets: List<Budget> = emptyList(),
    allocations: List<BudgetAllocation> = emptyList(),
    histories: List<BudgetHistory> = emptyList()
) : BudgetRepository {

    private val budgetsState = MutableStateFlow(budgets)
    private val allocationsState = MutableStateFlow(allocations)
    private val historiesState = MutableStateFlow(histories)

    override fun getBudgetForDateStream(cycleType: Int, date: Long): Flow<Budget?> {
        return budgetsState.map { budgets ->
            budgets.firstOrNull { budget ->
                budget.cycleType == cycleType &&
                    budget.periodStart <= date &&
                    budget.periodEnd >= date &&
                    !budget.isDeleted
            }
        }
    }

    override fun getBudgetsByPeriodStream(cycleType: Int, startDate: Long, endDate: Long): Flow<List<Budget>> {
        return budgetsState.map { budgets ->
            budgets.filter { budget ->
                budget.cycleType == cycleType &&
                    budget.periodStart in startDate..endDate &&
                    !budget.isDeleted
            }
        }
    }

    override fun getAllocationsByBudgetStream(budgetId: String): Flow<List<BudgetAllocation>> {
        return allocationsState.map { allocations ->
            allocations.filter { allocation ->
                allocation.budgetId == budgetId && !allocation.isDeleted
            }
        }
    }

    override fun getAllBudgetHistoryStream(): Flow<List<BudgetHistory>> = historiesState

    override suspend fun insertBudget(budget: Budget) {
        budgetsState.value = budgetsState.value.filterNot { it.id == budget.id } + budget
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetsState.value = budgetsState.value.map { current ->
            if (current.id == budget.id) budget else current
        }
    }

    override suspend fun upsertAllocations(budgetId: String, allocations: List<BudgetAllocation>) {
        allocationsState.value = allocationsState.value.filterNot { it.budgetId == budgetId } + allocations
    }

    override suspend fun insertBudgetHistory(history: BudgetHistory) {
        historiesState.value = listOf(history) + historiesState.value
    }

    override suspend fun getLatestClosedBudget(cycleType: Int, beforeDate: Long): Budget? {
        return budgetsState.value
            .filter { budget ->
                budget.cycleType == cycleType &&
                    budget.periodEnd < beforeDate &&
                    !budget.isDeleted
            }
            .maxByOrNull { it.periodEnd }
    }

    fun currentBudgetForDate(cycleType: Int, date: Long): Budget? {
        return budgetsState.value.firstOrNull { budget ->
            budget.cycleType == cycleType &&
                budget.periodStart <= date &&
                budget.periodEnd >= date &&
                !budget.isDeleted
        }
    }

    fun currentAllocationsForBudget(budgetId: String): List<BudgetAllocation> {
        return allocationsState.value.filter { it.budgetId == budgetId && !it.isDeleted }
    }
}

private class FakeWalletRepository(
    wallets: List<Wallet>
) : WalletRepository {
    private val walletsState = MutableStateFlow(wallets)

    override fun getWalletsStream(): Flow<List<Wallet>> = walletsState

    override fun getWalletStream(id: Long): Flow<Wallet?> {
        return walletsState.map { wallets -> wallets.firstOrNull { it.id == id } }
    }

    override suspend fun insertWallet(wallet: Wallet) {
        walletsState.value = walletsState.value + wallet
    }

    override suspend fun updateWallet(wallet: Wallet) {
        walletsState.value = walletsState.value.map { current ->
            if (current.id == wallet.id) wallet else current
        }
    }

    override suspend fun deleteWallet(wallet: Wallet) {
        walletsState.value = walletsState.value.filterNot { it.id == wallet.id }
    }
}

private class FakeCategoryRepository(
    categories: List<Category>
) : CategoryRepository {
    private val categoriesState = MutableStateFlow(categories)

    override fun getAllCategoriesStream(): Flow<List<Category>> = categoriesState

    override suspend fun insertCategory(category: Category) {
        categoriesState.value = categoriesState.value + category
    }

    override suspend fun updateCategory(category: Category) {
        categoriesState.value = categoriesState.value.map { current ->
            if (current.id == category.id) category else current
        }
    }

    override suspend fun deleteCategory(category: Category) {
        categoriesState.value = categoriesState.value.filterNot { it.id == category.id }
    }
}

private class FakeTransactionRepository(
    transactions: List<Transaction>
) : TransactionRepository {
    private val transactionsState = MutableStateFlow(transactions)

    override fun getAllTransactionsStream(): Flow<List<Transaction>> = transactionsState

    override fun getTransactionStream(id: Long): Flow<Transaction?> {
        return transactionsState.map { list -> list.firstOrNull { it.id == id } }
    }

    override fun getTransactionsByDateRangeStream(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionsState.map { list ->
            list.filter { it.date in startDate..endDate }
        }
    }

    override fun getTransactionsByWalletStream(walletId: Long): Flow<List<Transaction>> {
        return transactionsState.map { list -> list.filter { it.walletId == walletId } }
    }

    override fun getTotalAmountStream(type: Int, startDate: Long, endDate: Long): Flow<Long?> {
        return flowOf(null)
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        transactionsState.value = transactionsState.value + transaction
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionsState.value = transactionsState.value.map { current ->
            if (current.id == transaction.id) transaction else current
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionsState.value = transactionsState.value.filterNot { it.id == transaction.id }
    }

    override suspend fun transferMoney(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Long,
        note: String?,
        date: Long,
        transferCategoryId: Long
    ) {
        val nextId = (transactionsState.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val transferTx = Transaction(
            id = nextId,
            walletId = fromWalletId,
            toWalletId = toWalletId,
            categoryId = transferCategoryId,
            amount = amount,
            date = date,
            note = note
        )
        transactionsState.value = transactionsState.value + transferTx
    }
}
