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

class BudgetCategoryAdapter(
    private val onItemClick: (BudgetCategoryProgressUi) -> Unit = {}
) : ListAdapter<BudgetCategoryProgressUi, BudgetCategoryAdapter.BudgetCategoryViewHolder>(
    DiffCallback
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BudgetCategoryViewHolder {
        val binding = ItemBudgetCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: BudgetCategoryViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position), onItemClick)
    }

    class BudgetCategoryViewHolder(
        private val binding: ItemBudgetCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: BudgetCategoryProgressUi,
            onItemClick: (BudgetCategoryProgressUi) -> Unit
        ) {
            binding.root.setOnClickListener {
                onItemClick(item)
            }

            binding.tvCategoryName.text = item.categoryName
            binding.tvAmountSummary.text =
                "${formatCurrency(item.spent)} / ${formatCurrency(item.allocated)}"

            binding.tvRemaining.text = if (item.remaining >= 0L) {
                "Còn ${formatCurrency(item.remaining)}"
            } else {
                "Lố ${formatCurrency(-item.remaining)}"
            }

            binding.pbCategoryUsage.progress = item.usagePercent.coerceAtMost(100)

            val colorRes = when (item.progressState) {
                BudgetProgressState.SAFE -> R.color.status_income
                BudgetProgressState.WARNING -> R.color.status_warning
                BudgetProgressState.DANGER -> R.color.status_expense
            }

            val color = ContextCompat.getColor(binding.root.context, colorRes)

            binding.pbCategoryUsage.progressTintList = ColorStateList.valueOf(color)
            binding.tvRemaining.setTextColor(color)
        }

        private fun formatCurrency(value: Long): String {
            return NumberFormat
                .getCurrencyInstance(Locale("vi", "VN"))
                .format(value)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<BudgetCategoryProgressUi>() {
            override fun areItemsTheSame(
                oldItem: BudgetCategoryProgressUi,
                newItem: BudgetCategoryProgressUi
            ): Boolean {
                return oldItem.categoryId == newItem.categoryId
            }

            override fun areContentsTheSame(
                oldItem: BudgetCategoryProgressUi,
                newItem: BudgetCategoryProgressUi
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}