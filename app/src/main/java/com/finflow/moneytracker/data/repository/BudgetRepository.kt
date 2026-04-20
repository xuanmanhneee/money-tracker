package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.BudgetAllocationDao
import com.finflow.moneytracker.data.local.dao.BudgetDao
import com.finflow.moneytracker.data.local.dao.BudgetHistoryDao
import com.finflow.moneytracker.data.local.entity.Budget
import com.finflow.moneytracker.data.local.entity.BudgetAllocation
import com.finflow.moneytracker.data.local.entity.BudgetHistory
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetForDateStream(cycleType: Int, date: Long): Flow<Budget?>
    fun getBudgetsByPeriodStream(cycleType: Int, startDate: Long, endDate: Long): Flow<List<Budget>>
    fun getAllocationsByBudgetStream(budgetId: String): Flow<List<BudgetAllocation>>
    fun getAllBudgetHistoryStream(): Flow<List<BudgetHistory>>

    suspend fun insertBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun upsertAllocations(budgetId: String, allocations: List<BudgetAllocation>)
    suspend fun insertBudgetHistory(history: BudgetHistory)
    suspend fun getLatestClosedBudget(cycleType: Int, beforeDate: Long): Budget?
}

class OfflineBudgetRepository(
    private val budgetDao: BudgetDao,
    private val budgetAllocationDao: BudgetAllocationDao,
    private val budgetHistoryDao: BudgetHistoryDao
) : BudgetRepository {

    override fun getBudgetForDateStream(cycleType: Int, date: Long): Flow<Budget?> =
        budgetDao.getBudgetForDate(cycleType, date)

    override fun getBudgetsByPeriodStream(cycleType: Int, startDate: Long, endDate: Long): Flow<List<Budget>> =
        budgetDao.getBudgetsByPeriod(cycleType, startDate, endDate)

    override fun getAllocationsByBudgetStream(budgetId: String): Flow<List<BudgetAllocation>> =
        budgetAllocationDao.getByBudgetId(budgetId)

    override fun getAllBudgetHistoryStream(): Flow<List<BudgetHistory>> = budgetHistoryDao.getAll()

    override suspend fun insertBudget(budget: Budget) = budgetDao.insert(budget)

    override suspend fun updateBudget(budget: Budget) =
        budgetDao.update(budget.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun upsertAllocations(budgetId: String, allocations: List<BudgetAllocation>) {
        budgetAllocationDao.softDeleteByBudgetId(budgetId)
        budgetAllocationDao.insertAll(
            allocations.map {
                it.copy(updatedAt = System.currentTimeMillis(), isDeleted = false)
            }
        )
    }

    override suspend fun insertBudgetHistory(history: BudgetHistory) = budgetHistoryDao.insert(history)

    override suspend fun getLatestClosedBudget(cycleType: Int, beforeDate: Long): Budget? =
        budgetDao.getLatestClosedBudget(cycleType, beforeDate)
}
