package com.finflow.moneytracker.data.local.converter

import androidx.room.TypeConverter
import com.finflow.moneytracker.data.local.model.CategoryType

class CategoryTypeConverter {

    @TypeConverter
    fun fromType(type: CategoryType): Int {
        return when (type) {
            CategoryType.EXPENSE -> 0
            CategoryType.INCOME -> 1
        }
    }

    @TypeConverter
    fun toType(value: Int): CategoryType {
        return when (value) {
            0 -> CategoryType.EXPENSE
            1 -> CategoryType.INCOME
            else -> throw IllegalArgumentException("Unknown CategoryType: $value")
        }
    }
}