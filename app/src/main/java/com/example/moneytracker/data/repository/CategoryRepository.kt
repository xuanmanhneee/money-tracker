package com.example.moneytracker.data.repository

import com.example.moneytracker.data.local.dao.CategoryDao
import com.example.moneytracker.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

// Interface
interface CategoryRepository {
    fun getAllCategoriesStream(): Flow<List<Category>>
    suspend fun insertCategory(category: Category)
}

// Implementation
class OfflineCategoryRepository(private val categoryDao: CategoryDao) : CategoryRepository {
    override fun getAllCategoriesStream(): Flow<List<Category>> = categoryDao.getAll()
    override suspend fun insertCategory(category: Category) = categoryDao.insert(category)
}