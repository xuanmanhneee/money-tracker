package com.finflow.moneytracker.ui.budget

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finflow.moneytracker.R
import com.finflow.moneytracker.databinding.ItemBudgetCategoryBinding
import java.text.NumberFormat
import java.util.Locale

class BudgetCategoryAdapter :
    ListAdapter<BudgetCategoryProgressUi, BudgetCategoryAdapter.BudgetCategoryViewHolder>(DiffCallback) {

    private var onCategoryClick: ((BudgetCategoryProgressUi) -> Unit)? = null

    fun setOnCategoryClickListener(listener: (BudgetCategoryProgressUi) -> Unit) {
        onCategoryClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetCategoryViewHolder {
        val binding = ItemBudgetCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetCategoryViewHolder, position: Int) {
        holder.bind(getItem(position), onCategoryClick)
    }

    class BudgetCategoryViewHolder(
        private val binding: ItemBudgetCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: BudgetCategoryProgressUi,
            onCategoryClick: ((BudgetCategoryProgressUi) -> Unit)?
        ) {
            binding.tvCategoryName.text = item.categoryName
            binding.tvCategoryAllocated.text = "Ngân sách: ${formatCurrency(item.allocated)}"
            binding.tvCategorySpent.text = "Đã chi: ${formatCurrency(item.spent)}"
            binding.tvCategoryRemaining.text = "Còn lại: ${formatCurrency(item.remaining)}"
            binding.tvCategoryUsagePercent.text = "${item.usagePercent}%"
            binding.root.setOnClickListener {
                onCategoryClick?.invoke(item)
            }

            binding.pbCategoryUsage.progress = item.usagePercent.coerceAtMost(100)
            binding.pbCategoryUsage.progressTintList = ColorStateList.valueOf(
                when (item.progressState) {
                    BudgetProgressState.SAFE -> ContextCompat.getColor(binding.root.context, R.color.status_income)
                    BudgetProgressState.WARNING -> ContextCompat.getColor(binding.root.context, R.color.status_warning)
                    BudgetProgressState.DANGER -> ContextCompat.getColor(binding.root.context, R.color.status_expense)
                }
            )
        }

        private fun formatCurrency(value: Long): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            return formatter.format(value).replace("₫", "").trim() + " ₫"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BudgetCategoryProgressUi>() {
        override fun areItemsTheSame(
            oldItem: BudgetCategoryProgressUi,
            newItem: BudgetCategoryProgressUi
        ): Boolean = oldItem.categoryId == newItem.categoryId

        override fun areContentsTheSame(
            oldItem: BudgetCategoryProgressUi,
            newItem: BudgetCategoryProgressUi
        ): Boolean = oldItem == newItem
    }
}
