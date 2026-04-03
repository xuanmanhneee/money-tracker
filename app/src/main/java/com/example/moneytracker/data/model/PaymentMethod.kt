package com.example.moneytracker.data.model

enum class PaymentMethod(val id: Int, val displayName: String) {
    CASH(1, "Tiền mặt"),
    BANK(2, "Ngân hàng")
}
