package com.finflow.moneytracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.finflow.moneytracker.data.local.model.CategoryType

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
    val name: String,
    val type: CategoryType,
    @ColumnInfo(name = "monthly_budget_limit")
    val monthlyBudgetLimit: Long? = null,
    val icon: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
