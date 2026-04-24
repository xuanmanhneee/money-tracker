package com.finflow.moneytracker.ui.common

import com.finflow.moneytracker.R

object CategoryIconResolver {

    private const val TYPE_EXPENSE = 0
    private const val TYPE_INCOME = 1

    fun inferCategoryIconKey(categoryName: String, categoryType: Int): String {
        val normalized = normalize(categoryName)
        return when {
            hasAny(normalized, BILL_KEYWORDS) -> "ic_budget"
            hasAny(normalized, TRANSPORT_KEYWORDS) -> "ic_transport"
            hasAny(normalized, INCOME_KEYWORDS) || categoryType == TYPE_INCOME -> "ic_money"
            hasAny(normalized, FOOD_KEYWORDS) -> "ic_food"
            else -> if (categoryType == TYPE_INCOME) "ic_money" else "ic_category"
        }
    }

    fun resolveCategoryIconRes(
        iconName: String?,
        categoryName: String?,
        categoryType: Int? = null
    ): Int {
        val iconKey = iconName?.trim()?.lowercase().orEmpty()
        if (iconKey.isNotEmpty()) {
            when (iconKey) {
                "ic_money", "ic_salary" -> return R.drawable.ic_money
                "ic_transport", "ic_transaction" -> return R.drawable.ic_transaction
                "ic_budget", "ic_bill", "ic_utility" -> return R.drawable.ic_budget
                "ic_food", "ic_category" -> {
                    // Continue below for keyword-based override when available.
                }
            }
        }

        val normalizedName = normalize(categoryName.orEmpty())
        return when {
            hasAny(normalizedName, BILL_KEYWORDS) -> R.drawable.ic_budget
            hasAny(normalizedName, TRANSPORT_KEYWORDS) -> R.drawable.ic_transaction
            hasAny(normalizedName, INCOME_KEYWORDS) || categoryType == TYPE_INCOME -> R.drawable.ic_money
            hasAny(normalizedName, FOOD_KEYWORDS) -> R.drawable.ic_category
            else -> if (categoryType == TYPE_INCOME) R.drawable.ic_money else R.drawable.ic_category
        }
    }

    private fun normalize(text: String): String = text.trim().lowercase()

    private fun hasAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }

    private val FOOD_KEYWORDS = listOf(
        "an", "ăn", "uong", "uống", "food", "meal", "do an", "đồ ăn"
    )

    private val TRANSPORT_KEYWORDS = listOf(
        "di chuyen", "di chuyển", "xang", "xe", "grab", "bus", "taxi", "transport"
    )

    private val INCOME_KEYWORDS = listOf(
        "luong", "lương", "thu nhap", "thu nhập", "salary", "income", "bonus", "thuong", "thưởng"
    )

    private val BILL_KEYWORDS = listOf(
        "hoa don", "hóa đơn", "dien", "điện", "nuoc", "nước", "wifi", "internet", "tien nha", "tiền nhà", "bill"
    )
}
