package com.finflow.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finflow.moneytracker.data.local.entity.BudgetHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: BudgetHistory)

    @Update
    suspend fun update(history: BudgetHistory)

    @Delete
    suspend fun delete(history: BudgetHistory)

    @Query(
        "SELECT * FROM budget_history WHERE budget_id = :budgetId AND is_deleted = 0 ORDER BY updated_at DESC"
    )
    fun getByBudgetId(budgetId: String): Flow<List<BudgetHistory>>
}
