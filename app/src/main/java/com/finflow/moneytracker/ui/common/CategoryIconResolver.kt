package com.finflow.moneytracker.ui.common

import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.model.CategoryType

object CategoryIconResolver {

    fun resolveCategoryIconRes(
        iconName: String?,
        categoryType: CategoryType? = null
    ): Int {

        // 1. Ưu tiên icon user đã chọn
        val iconKey = iconName?.trim()?.lowercase().orEmpty()

        if (iconKey.isNotEmpty()) {
            return when (iconKey) {
                "ic_money", "ic_salary" -> R.drawable.ic_money
                "ic_transport" -> R.drawable.ic_transaction
                "ic_budget" -> R.drawable.ic_budget
                "ic_food" -> R.drawable.ic_category
                else -> R.drawable.ic_category
            }
        }

        // 2. fallback theo type (nếu thiếu icon)
        return when (categoryType) {
            CategoryType.INCOME -> R.drawable.ic_money
            CategoryType.EXPENSE -> R.drawable.ic_category
            null -> R.drawable.ic_category
        }
    }
}