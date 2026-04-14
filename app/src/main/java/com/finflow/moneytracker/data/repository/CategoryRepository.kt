package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.CategoryDao
import com.finflow.moneytracker.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategoriesStream(): Flow<List<Category>>
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}

class OfflineCategoryRepository(private val categoryDao: CategoryDao) : CategoryRepository {
    override fun getAllCategoriesStream(): Flow<List<Category>> = categoryDao.getAll()
    override suspend fun insertCategory(category: Category) = categoryDao.insert(category)
    override suspend fun updateCategory(category: Category) = categoryDao.update(category.copy(updatedAt = System.currentTimeMillis()))
    
    override suspend fun deleteCategory(category: Category) {
        categoryDao.update(category.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
    }
}