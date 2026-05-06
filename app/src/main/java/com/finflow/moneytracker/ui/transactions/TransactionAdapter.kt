package com.finflow.moneytracker.ui.transactions

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finflow.moneytracker.R
import com.finflow.moneytracker.ui.common.CategoryIconResolver
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class TransactionAdapter : ListAdapter<TransactionUiItem, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvPaymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory)

        fun bind(item: TransactionUiItem) {
            val transaction = item.transaction
            tvCategoryName.text = item.categoryName
            tvNote.text = transaction.note
            tvPaymentMethod.text = item.paymentMethodLabel
            applyPaymentChipStyle(item.paymentMethodLabel)
            imgCategory.setImageResource(
                CategoryIconResolver.resolveCategoryIconRes(
                    iconName = item.categoryIcon,
                    categoryName = item.categoryName,
                    categoryType = item.categoryType
                )
            )

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(transaction.date))

            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            // Sử dụng abs() để lấy giá trị tuyệt đối, tránh hiện dấu trừ của hệ thống
            val amountValue = abs(transaction.amount)
            val formattedAmount = "${formatter.format(amountValue)}đ"

            if (transaction.amount < 0) {
                // Chi tiêu: Màu đỏ, định dạng "- 50.000đ"
                tvAmount.text = "- $formattedAmount"
                tvAmount.setTextColor(Color.parseColor("#F44336"))
            } else {
                // Thu nhập: Màu xanh, định dạng "+ 15.000.000đ"
                tvAmount.text = "+ $formattedAmount"
                tvAmount.setTextColor(Color.parseColor("#4CAF50"))
            }
        }

        private fun applyPaymentChipStyle(paymentMethodLabel: String) {
            val normalized = paymentMethodLabel.trim().lowercase(Locale.getDefault())
            val (bgColorRes, textColorRes) = when {
                "ngân hàng" in normalized || "ngan hang" in normalized || "bank" in normalized -> {
                    R.color.chip_payment_bank_bg to R.color.chip_payment_bank_text
                }
                "tiền mặt" in normalized || "tien mat" in normalized || "cash" in normalized -> {
                    R.color.chip_payment_cash_bg to R.color.chip_payment_cash_text
                }
                else -> {
                    R.color.chip_payment_other_bg to R.color.chip_payment_other_text
                }
            }

            val context = itemView.context
            tvPaymentMethod.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, bgColorRes)
            )
            tvPaymentMethod.setTextColor(ContextCompat.getColor(context, textColorRes))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TransactionUiItem>() {
            override fun areItemsTheSame(oldItem: TransactionUiItem, newItem: TransactionUiItem): Boolean {
                return oldItem.transaction.id == newItem.transaction.id
            }

            override fun areContentsTheSame(oldItem: TransactionUiItem, newItem: TransactionUiItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}