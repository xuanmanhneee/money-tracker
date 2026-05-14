package com.finflow.moneytracker.data.repository

import com.finflow.moneytracker.data.local.dao.CategoryDao
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategoriesStream(): Flow<List<Category>>
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    suspend fun updateMonthlyBudgetLimit(categoryId: Long, limit: Long?)
}

class DefaultCategoryRepository(
    private val categoryDao: CategoryDao,
    private val remoteDataSource: RemoteDataSource
) : CategoryRepository {

    override fun getAllCategoriesStream(): Flow<List<Category>> =
        categoryDao.getAll()

    override suspend fun insertCategory(category: Category) {
        val generatedId = categoryDao.insert(category)
        remoteDataSource.syncCategory(category.copy(id = generatedId))
    }

    override suspend fun updateCategory(category: Category) {
        val updatedCategory = category.copy(updatedAt = System.currentTimeMillis())
        categoryDao.update(updatedCategory)
        remoteDataSource.syncCategory(updatedCategory)
    }

    override suspend fun deleteCategory(category: Category) {
        val deletedCategory = category.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.update(deletedCategory)
        remoteDataSource.syncCategory(deletedCategory)
    }

    override suspend fun updateMonthlyBudgetLimit(categoryId: Long, limit: Long?) {
        val category = categoryDao.getById(categoryId) ?: return

        val updatedCategory = category.copy(
            monthlyBudgetLimit = limit,
            updatedAt = System.currentTimeMillis()
        )

        categoryDao.update(updatedCategory)
        remoteDataSource.syncCategory(updatedCategory)
    }
}
