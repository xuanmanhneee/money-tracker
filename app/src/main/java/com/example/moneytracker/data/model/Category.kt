package com.example.moneytracker.data.model

data class Category(
    val id: Int,
    val name: String,
    val icon: Int,
    val type: TransactionType
)
