package com.example.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.moneytracker.R
import com.example.moneytracker.data.model.Category
import com.example.moneytracker.data.model.TransactionType

class AddCategoryDialogFragment : DialogFragment() {

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

        val etCategoryName = view.findViewById<EditText>(R.id.etCategoryName)
        val rbExpense = view.findViewById<RadioButton>(R.id.rbExpense)
        val rbIncome = view.findViewById<RadioButton>(R.id.rbIncome)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Thêm") { _, _ ->
                val categoryName = etCategoryName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    val type = if (rbIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
                    val newId = System.currentTimeMillis().toInt()
                    val newCategory = Category(newId, categoryName, R.drawable.ic_category, type)
                    listener?.onCategoryAdded(newCategory)
                }
            }
            .setNegativeButton("Hủy", null)
            .create()
    }
}
