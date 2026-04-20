package com.finflow.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.ui.common.CategoryIconResolver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        val rbExpense = view.findViewById<RadioButton>(R.id.rbExpense)
        val rbIncome = view.findViewById<RadioButton>(R.id.rbIncome)
        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnAdd = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAdd)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()

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
            val newCategory = Category(
                name = categoryName,
                type = type,
                icon = CategoryIconResolver.inferCategoryIconKey(categoryName, type)
            )
            listener?.onCategoryAdded(newCategory)
            dismiss()
        }

        return dialog
    }
}