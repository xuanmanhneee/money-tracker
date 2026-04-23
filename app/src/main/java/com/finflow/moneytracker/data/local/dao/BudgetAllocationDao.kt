package com.finflow.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finflow.moneytracker.data.local.entity.BudgetAllocation
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetAllocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(allocation: BudgetAllocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(allocations: List<BudgetAllocation>)

    @Update
    suspend fun update(allocation: BudgetAllocation)

    @Query("SELECT * FROM budget_allocations WHERE budget_id = :budgetId AND is_deleted = 0")
    fun getByBudgetId(budgetId: String): Flow<List<BudgetAllocation>>

    @Query("UPDATE budget_allocations SET is_deleted = 1, updated_at = :updatedAt WHERE budget_id = :budgetId")
    suspend fun softDeleteByBudgetId(budgetId: String, updatedAt: Long = System.currentTimeMillis())
}
