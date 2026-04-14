package com.example.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.moneytracker.R
import com.example.moneytracker.data.local.entity.Category

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

        val etCategoryName = view.findViewById<EditText>(R.id.etCategoryName)
        val rbIncome = view.findViewById<RadioButton>(R.id.rbIncome)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Thêm") { _, _ ->
                val categoryName = etCategoryName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    val type = if (rbIncome.isChecked) TYPE_INCOME else TYPE_EXPENSE
                    val newCategory = Category(
                        name = categoryName,
                        type = type,
                        icon = "ic_category"
                    )
                    listener?.onCategoryAdded(newCategory)
                }
            }
            .setNegativeButton("Hủy", null)
            .create()
    }
}
