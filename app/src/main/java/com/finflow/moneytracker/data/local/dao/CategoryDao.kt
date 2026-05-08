package com.finflow.moneytracker.data.local.dao

import androidx.room.*
import com.finflow.moneytracker.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE is_deleted = 0")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type AND is_deleted = 0")
    fun getByType(type: Int): Flow<List<Category>>
}
