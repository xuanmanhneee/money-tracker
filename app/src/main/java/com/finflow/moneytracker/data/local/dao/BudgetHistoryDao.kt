package com.finflow.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finflow.moneytracker.data.local.entity.BudgetHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: BudgetHistory)

    @Query("SELECT * FROM budget_history WHERE budget_id = :budgetId ORDER BY period_end DESC")
    fun getByBudgetId(budgetId: String): Flow<List<BudgetHistory>>

    @Query("SELECT * FROM budget_history ORDER BY period_end DESC")
    fun getAll(): Flow<List<BudgetHistory>>
}
