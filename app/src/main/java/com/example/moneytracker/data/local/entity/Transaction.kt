package com.example.moneytracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        // 1. Khóa ngoại liên kết với Ví nguồn
        ForeignKey(
            entity = Wallet::class,
            parentColumns = ["id"],
            childColumns = ["wallet_id"],
            onDelete = ForeignKey.CASCADE // Nếu xóa Ví -> Xóa luôn các giao dịch của ví đó
        ),
        // 2. Khóa ngoại liên kết với Danh mục
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT // Nếu Danh mục đang có giao dịch -> KHÔNG cho phép xóa Danh mục
        ),
        // 3. Khóa ngoại liên kết với Ví đích (Chuyển tiền nội bộ)
        ForeignKey(
            entity = Wallet::class,
            parentColumns = ["id"],
            childColumns = ["to_wallet_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Đánh Index cho các cột khóa ngoại để tăng tốc độ truy vấn (Room khuyên dùng)
    indices = [
        Index(value = ["wallet_id"]),
        Index(value = ["category_id"]),
        Index(value = ["to_wallet_id"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "wallet_id") val walletId: Int,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    val amount: Long,
    val date: Long,
    val note: String? = null, // Đã đổi thành số nhiều cho khớp ERD
    @ColumnInfo(name = "receipt_image_path") val receiptImagePath: String? = null,
    @ColumnInfo(name = "to_wallet_id") val toWalletId: Int? = null
)