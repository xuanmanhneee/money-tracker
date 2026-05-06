package com.finflow.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.ui.common.CategoryIconResolver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class AddCategoryDialogFragment : DialogFragment() {

    companion object {
        private const val TYPE_EXPENSE = 0
        private const val TYPE_INCOME = 1
        
        // Danh sách icon mặc định để người dùng chọn
        private val AVAILABLE_ICONS = listOf(
            "ic_category", "ic_money", "ic_budget", "ic_transaction",
            "ic_home", "ic_account", "ic_history"
        )
    }

    interface OnCategoryAddedListener {
        fun onCategoryAdded(category: Category)
    }

    private var listener: OnCategoryAddedListener? = null
    private var selectedIcon: String = "ic_category"

    fun setOnCategoryAddedListener(listener: OnCategoryAddedListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): androidx.appcompat.app.AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_add_category, null)

        val inputLayout = view.findViewById<TextInputLayout>(R.id.tilCategoryName)
        val etCategoryName = view.findViewById<EditText>(R.id.etCategoryName)
        val etMonthlyLimit = view.findViewById<EditText>(R.id.etMonthlyLimit)
        val rbExpense = view.findViewById<RadioButton>(R.id.rbExpense)
        val rbIncome = view.findViewById<RadioButton>(R.id.rbIncome)
        val rvIcons = view.findViewById<RecyclerView>(R.id.rvIcons)
        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnAdd = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAdd)

        // Setup Icon RecyclerView
        rvIcons.layoutManager = GridLayoutManager(requireContext(), 4)
        rvIcons.adapter = IconAdapter(AVAILABLE_ICONS) { iconKey ->
            selectedIcon = iconKey
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()

        btnCancel.setOnClickListener { dismiss() }

        btnAdd.setOnClickListener {
            val categoryName = etCategoryName.text.toString().trim()
            if (categoryName.isEmpty()) {
                inputLayout.error = "Vui lòng nhập tên nhóm"
                return@setOnClickListener
            }

            val limitText = etMonthlyLimit.text.toString().trim()
            val monthlyLimit = if (limitText.isNotEmpty()) limitText.toLongOrNull() else null

            inputLayout.error = null
            val type = if (rbIncome.isChecked) TYPE_INCOME else TYPE_EXPENSE
            
            val newCategory = Category(
                name = categoryName,
                type = type,
                icon = selectedIcon,
                monthlyBudgetLimit = monthlyLimit
            )
            listener?.onCategoryAdded(newCategory)
            dismiss()
        }

        return dialog
    }

    private inner class IconAdapter(
        private val icons: List<String>,
        private val onIconSelected: (String) -> Unit
    ) : RecyclerView.Adapter<IconAdapter.ViewHolder>() {

        private var selectedPos = 0

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.ivCategoryIcon)
            val container: View = view
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            v.findViewById<View>(R.id.tvCategoryName).visibility = View.GONE
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val iconKey = icons[position]
            holder.iconView.setImageResource(CategoryIconResolver.resolveCategoryIconRes(iconKey, ""))
            
            holder.container.alpha = if (selectedPos == position) 1.0f else 0.4f
            
            holder.container.setOnClickListener {
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    val oldPos = selectedPos
                    selectedPos = currentPos
                    notifyItemChanged(oldPos)
                    notifyItemChanged(selectedPos)
                    onIconSelected(iconKey)
                }
            }
        }

        override fun getItemCount() = icons.size
    }
}
