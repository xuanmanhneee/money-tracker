package com.example.moneytracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: Int, // 0: Chi tiêu, 1: Thu nhập
    val icon: String,
    @ColumnInfo(name = "monthly_budget_limit")
    val monthlyBudgetLimit: Long? = null
)