    package com.finflow.moneytracker.data.local.entity

    import androidx.room.ColumnInfo
    import androidx.room.Entity
    import androidx.room.ForeignKey
    import androidx.room.Index
    import androidx.room.PrimaryKey

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
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        @ColumnInfo(name = "user_id")
        val userId: String = "",
        @ColumnInfo(name = "wallet_id")
        val walletId: Long,
        @ColumnInfo(name = "category_id")
        val categoryId: Long?,
        val amount: Long,
        val date: Long,
        val note: String? = null,
        @ColumnInfo(name = "receipt_image_path")
        val receiptImagePath: String? = null,
        @ColumnInfo(name = "to_wallet_id")
        val toWalletId: Long? = null,
        @ColumnInfo(name = "is_deleted")
        val isDeleted: Boolean = false,
        @ColumnInfo(name = "updated_at")
        val updatedAt: Long = System.currentTimeMillis()
    )
