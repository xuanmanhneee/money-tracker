package com.finflow.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey 
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "", // ID của user từ Firebase Auth
    val name: String,
    val balance: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)