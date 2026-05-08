package com.finflow.moneytracker.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

object CurrencyInputFormatter {

    private val locale = Locale("vi", "VN")

    fun attach(input: EditText) {
        val formatter = NumberFormat.getInstance(locale).apply {
            isGroupingUsed = true
            maximumFractionDigits = 0
        }

        var isFormatting = false
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) {
                    return
                }

                val currentText = s?.toString().orEmpty()
                val digits = extractDigits(currentText)
                if (digits.isEmpty()) {
                    return
                }

                val value = digits.toLongOrNull() ?: return
                val formatted = formatter.format(value)
                if (formatted == currentText) {
                    return
                }

                isFormatting = true
                input.setText(formatted)
                input.setSelection(formatted.length)
                isFormatting = false
            }
        })
    }

    fun parseAmountOrNull(input: EditText): Long? {
        val digits = extractDigits(input.text?.toString().orEmpty())
        if (digits.isEmpty()) {
            return null
        }
        return digits.toLongOrNull()
    }

    fun hasAnyDigit(input: EditText): Boolean {
        return extractDigits(input.text?.toString().orEmpty()).isNotEmpty()
    }

    fun setAmount(input: EditText, amount: Long) {
        val safeAmount = amount.coerceAtLeast(0L)
        val formatted = NumberFormat.getInstance(locale).format(safeAmount)
        input.setText(formatted)
        input.setSelection(formatted.length)
    }

    private fun extractDigits(source: String): String {
        return source.filter { it.isDigit() }
    }
}