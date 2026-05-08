package com.finflow.moneytracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "budget_history",
    foreignKeys = [
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["budget_id"])
    ]
)
data class BudgetHistory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "budget_id")
    val budgetId: String,
    @ColumnInfo(name = "period_start")
    val periodStart: Long,
    @ColumnInfo(name = "period_end")
    val periodEnd: Long,
    @ColumnInfo(name = "planned_amount")
    val plannedAmount: Long,
    @ColumnInfo(name = "spent_amount")
    val spentAmount: Long,
    @ColumnInfo(name = "remaining_amount")
    val remainingAmount: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
