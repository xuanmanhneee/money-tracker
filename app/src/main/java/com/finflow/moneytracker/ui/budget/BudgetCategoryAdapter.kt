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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetCategoryViewHolder {
        return BudgetCategoryViewHolder(
            ItemBudgetCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: BudgetCategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BudgetCategoryViewHolder(private val binding: ItemBudgetCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BudgetCategoryProgressUi) {
            val ctx = binding.root.context

            val resId = item.iconEmoji
                ?.takeIf { it.isNotBlank() }
                ?.let { ctx.resources.getIdentifier(it, "drawable", ctx.packageName) }
                ?.takeIf { it != 0 }
                ?: R.drawable.ic_category_default

            binding.ivCategoryIcon.setImageResource(resId)

            // Tên danh mục
            binding.tvCategoryName.text = item.categoryName

            // Hàng số tiền: "450.000 / 800.000 ₫"
            binding.tvAmountSummary.text =
                "${formatCurrency(item.spent)} / ${formatCurrency(item.allocated)} ₫"

            // Hàng phần trăm: "Đã dùng 56%"
            binding.tvCategoryUsedPercent.text = "Đã dùng ${item.usagePercent}%"

            // Màu và text "Còn lại" / "Vượt"
            val (progressColor, remainingText) = when (item.progressState) {
                BudgetProgressState.SAFE -> Pair(
                    R.color.status_income,
                    "Còn ${formatCurrency(item.remaining)} ₫"
                )
                BudgetProgressState.WARNING -> Pair(
                    R.color.status_warning,
                    "Còn ${formatCurrency(item.remaining)} ₫"
                )
                BudgetProgressState.DANGER -> Pair(
                    R.color.status_expense,
                    "Vượt ${formatCurrency(item.spent - item.allocated)} ₫"
                )
            }

            val resolvedColor = ContextCompat.getColor(ctx, progressColor)

            binding.tvRemaining.text = remainingText
            binding.tvRemaining.setTextColor(resolvedColor)
            binding.pbCategoryUsage.progressTintList = ColorStateList.valueOf(resolvedColor)
            binding.pbCategoryUsage.progress = item.usagePercent.coerceAtMost(100)

            // Stroke card đỏ khi DANGER
            binding.root.strokeColor = if (item.progressState == BudgetProgressState.DANGER) {
                ContextCompat.getColor(ctx, R.color.status_expense)
            } else {
                ContextCompat.getColor(ctx, R.color.divider)
            }
        }

        private fun formatCurrency(value: Long): String {
            return NumberFormat.getNumberInstance(Locale("vi", "VN")).format(value)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BudgetCategoryProgressUi>() {
        override fun areItemsTheSame(old: BudgetCategoryProgressUi, new: BudgetCategoryProgressUi) =
            old.categoryId == new.categoryId
        override fun areContentsTheSame(old: BudgetCategoryProgressUi, new: BudgetCategoryProgressUi) =
            old == new
    }
}