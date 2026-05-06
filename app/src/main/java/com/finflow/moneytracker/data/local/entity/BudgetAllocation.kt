package com.finflow.moneytracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "budget_allocations",
    foreignKeys = [
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["budget_id"]),
        Index(value = ["category_id"])
    ]
)
data class BudgetAllocation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "budget_id")
    val budgetId: String,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "allocated_amount")
    val allocatedAmount: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
