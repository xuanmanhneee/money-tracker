package com.finflow.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finflow.moneytracker.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Update
    suspend fun update(budget: Budget)

    @Query("SELECT * FROM budgets WHERE id = :id AND is_deleted = 0")
    fun getById(id: String): Flow<Budget?>

    @Query(
        """
        SELECT * FROM budgets
        WHERE cycle_type = :cycleType
        AND period_start <= :date
        AND period_end >= :date
        AND is_deleted = 0
        LIMIT 1
        """
    )
    fun getBudgetForDate(cycleType: Int, date: Long): Flow<Budget?>

    @Query(
        """
        SELECT * FROM budgets
        WHERE cycle_type = :cycleType
        AND period_start BETWEEN :startDate AND :endDate
        AND is_deleted = 0
        ORDER BY period_start DESC
        """
    )
    fun getBudgetsByPeriod(cycleType: Int, startDate: Long, endDate: Long): Flow<List<Budget>>

    @Query(
        """
        SELECT * FROM budgets
        WHERE cycle_type = :cycleType
        AND period_end < :beforeDate
        AND is_deleted = 0
        ORDER BY period_end DESC
        LIMIT 1
        """
    )
    suspend fun getLatestClosedBudget(cycleType: Int, beforeDate: Long): Budget?
}
