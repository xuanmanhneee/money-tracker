package com.finflow.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Delete
    suspend fun delete(budget: Budget)

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
}
