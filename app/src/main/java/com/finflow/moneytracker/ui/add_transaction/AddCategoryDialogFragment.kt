package com.finflow.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.ui.common.CategoryIconResolver
import com.finflow.moneytracker.ui.common.CurrencyInputFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddCategoryDialogFragment : DialogFragment() {

    companion object {
        private const val TYPE_EXPENSE = 0
        private const val TYPE_INCOME = 1
    }

    interface OnCategoryAddedListener {
        fun onCategoryAdded(category: Category)
    }

    private var listener: OnCategoryAddedListener? = null

    fun setOnCategoryAddedListener(listener: OnCategoryAddedListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): androidx.appcompat.app.AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_add_category, null)

        val inputLayout = view.findViewById<TextInputLayout>(R.id.tilCategoryName)
        val etCategoryName = view.findViewById<EditText>(R.id.etCategoryName)
        val budgetLayout = view.findViewById<TextInputLayout>(R.id.tilCategoryBudget)
        val etCategoryBudget = view.findViewById<TextInputEditText>(R.id.etCategoryBudget)
        val rgCategoryType = view.findViewById<RadioGroup>(R.id.rgCategoryType)
        val rbExpense = view.findViewById<RadioButton>(R.id.rbExpense)
        val rbIncome = view.findViewById<RadioButton>(R.id.rbIncome)
        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnAdd = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAdd)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
        CurrencyInputFormatter.attach(etCategoryBudget)

        fun updateBudgetInputVisibility() {
            val isExpense = rbExpense.isChecked
            budgetLayout.visibility = if (isExpense) android.view.View.VISIBLE else android.view.View.GONE
            if (!isExpense) {
                budgetLayout.error = null
                etCategoryBudget.setText("")
            }
        }

        rgCategoryType.setOnCheckedChangeListener { _, _ ->
            updateBudgetInputVisibility()
        }
        updateBudgetInputVisibility()

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnAdd.setOnClickListener {
            val categoryName = etCategoryName.text.toString().trim()
            if (categoryName.isEmpty()) {
                inputLayout.error = "Vui lòng nhập tên nhóm"
                return@setOnClickListener
            }

            inputLayout.error = null
            val type = if (rbIncome.isChecked) TYPE_INCOME else TYPE_EXPENSE
            val monthlyBudgetLimit = if (type == TYPE_EXPENSE) {
                if (!CurrencyInputFormatter.hasAnyDigit(etCategoryBudget)) {
                    0L
                } else {
                    val parsedBudget = CurrencyInputFormatter.parseAmountOrNull(etCategoryBudget)
                    if (parsedBudget == null) {
                        budgetLayout.error = "Ngân sách không hợp lệ"
                        return@setOnClickListener
                    }
                    parsedBudget
                }
            } else {
                null
            }

            budgetLayout.error = null
            val newCategory = Category(
                name = categoryName,
                type = type,
                icon = CategoryIconResolver.inferCategoryIconKey(categoryName, type),
                monthlyBudgetLimit = monthlyBudgetLimit
            )
            listener?.onCategoryAdded(newCategory)
            dismiss()
        }

        return dialog
    }
}