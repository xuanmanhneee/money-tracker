package com.finflow.moneytracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey 
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String,
    val type: Int, // 0: Chi tiêu, 1: Thu nhập
    val icon: String,
    @ColumnInfo(name = "monthly_budget_limit")
    val monthlyBudgetLimit: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)