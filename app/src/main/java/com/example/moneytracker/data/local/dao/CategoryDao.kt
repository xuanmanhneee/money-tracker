package com.example.moneytracker.data.local.dao

import androidx.room.*
import com.example.moneytracker.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    // Lấy toàn bộ danh mục
    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<Category>>

    // Lấy danh mục theo loại (0: Chi tiêu, 1: Thu nhập) để hiển thị lên UI cho đúng
    @Query("SELECT * FROM categories WHERE type = :type")
    fun getByType(type: Int): Flow<List<Category>>
}