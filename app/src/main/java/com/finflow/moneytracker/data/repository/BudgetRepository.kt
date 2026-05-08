package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.BudgetAllocationDao
import com.finflow.moneytracker.data.local.dao.BudgetDao
import com.finflow.moneytracker.data.local.dao.BudgetHistoryDao
import com.finflow.moneytracker.data.local.entity.Budget
import com.finflow.moneytracker.data.local.entity.BudgetAllocation
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetForDateStream(cycleType: Int, date: Long): Flow<Budget?>
    fun getAllocationsByBudgetStream(budgetId: String): Flow<List<BudgetAllocation>>
    suspend fun insertBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun upsertAllocations(budgetId: String, allocations: List<BudgetAllocation>)
}

class OfflineBudgetRepository(
    private val budgetDao: BudgetDao,
    private val budgetAllocationDao: BudgetAllocationDao,
    private val budgetHistoryDao: BudgetHistoryDao
) : BudgetRepository {
    override fun getBudgetForDateStream(cycleType: Int, date: Long): Flow<Budget?> =
        budgetDao.getBudgetForDate(cycleType, date)

    override fun getAllocationsByBudgetStream(budgetId: String): Flow<List<BudgetAllocation>> =
        budgetAllocationDao.getByBudgetId(budgetId)

    override suspend fun insertBudget(budget: Budget) {
        budgetDao.insert(budget.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.update(budget.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun upsertAllocations(
        budgetId: String,
        allocations: List<BudgetAllocation>
    ) {
        val normalized = allocations.map { allocation ->
            allocation.copy(
                budgetId = budgetId,
                updatedAt = System.currentTimeMillis(),
                isDeleted = false
            )
        }
        budgetAllocationDao.deleteByBudgetId(budgetId)
        if (normalized.isNotEmpty()) {
            budgetAllocationDao.insertAll(normalized)
        }
    }
}
