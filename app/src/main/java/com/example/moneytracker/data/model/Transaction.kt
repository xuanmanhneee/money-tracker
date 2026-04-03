package com.example.moneytracker.data.model

import java.util.Date

data class Transaction(
    val id: Int = System.currentTimeMillis().toInt(),
    val amount: Double,
    val category: Category,
    val paymentMethod: PaymentMethod,
    val note: String,
    val date: Date,
    val workHours: Double = 0.0
)
