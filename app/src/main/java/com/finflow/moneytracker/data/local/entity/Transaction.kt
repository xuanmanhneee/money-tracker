package com.finflow.moneytracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Wallet::class,
            parentColumns = ["id"],
            childColumns = ["wallet_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Wallet::class,
            parentColumns = ["id"],
            childColumns = ["to_wallet_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["wallet_id"]),
        Index(value = ["category_id"]),
        Index(value = ["to_wallet_id"])
    ]
)
data class Transaction(
    @PrimaryKey 
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    @ColumnInfo(name = "wallet_id") val walletId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    val amount: Long,
    val date: Long,
    val note: String? = null,
    @ColumnInfo(name = "receipt_image_path") val receiptImagePath: String? = null,
    @ColumnInfo(name = "to_wallet_id") val toWalletId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)