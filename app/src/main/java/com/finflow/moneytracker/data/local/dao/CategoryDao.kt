package com.finflow.moneytracker.data.local.dao

import androidx.room.*
import com.finflow.moneytracker.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type AND isDeleted = 0")
    fun getByType(type: Int): Flow<List<Category>>
}