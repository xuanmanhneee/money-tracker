package com.finflow.moneytracker.ui.transactions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.model.CategoryType
import com.finflow.moneytracker.ui.common.CategoryIconResolver
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class TransactionAdapter :
    ListAdapter<TransactionUiModel, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TransactionUiModel>() {
            override fun areItemsTheSame(
                oldItem: TransactionUiModel,
                newItem: TransactionUiModel
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: TransactionUiModel,
                newItem: TransactionUiModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvPaymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory)

        fun bind(item: TransactionUiModel) {

            val transaction = item

            // ---------------- CATEGORY ----------------
            val categoryName = when (val c = item.category) {
                is CategorySnapshot.Active -> c.name
                CategorySnapshot.Deleted -> "Danh mục đã xóa"
            }

            val categoryIcon = when (val c = item.category) {
                is CategorySnapshot.Active -> c.icon
                CategorySnapshot.Deleted -> "ic_category"
            }

            tvCategoryName.text = categoryName
            tvNote.text = item.note.orEmpty()

            imgCategory.setImageResource(
                CategoryIconResolver.resolveCategoryIconRes(
                    iconName = categoryIcon,
                    categoryType = (item.category as? CategorySnapshot.Active)?.type
                )
            )

            // ---------------- DATE ----------------
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(item.date))

            // ---------------- AMOUNT ----------------
            val isExpense = when (val c = item.category) {
                is CategorySnapshot.Active -> c.type == CategoryType.EXPENSE
                CategorySnapshot.Deleted -> true
            }

            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            val amountValue = abs(item.amount)
            val formattedAmount = "${formatter.format(amountValue)}đ"

            if (isExpense) {
                tvAmount.text = "- $formattedAmount"
                tvAmount.setTextColor(Color.parseColor("#F44336"))
            } else {
                tvAmount.text = "+ $formattedAmount"
                tvAmount.setTextColor(Color.parseColor("#4CAF50"))
            }

            // ---------------- WALLET ----------------
            val walletName = when (val w = item.wallet) {
                is WalletSnapshot.Active -> w.name
                WalletSnapshot.Unknown -> "Không xác định"
            }

            tvPaymentMethod.text = walletName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}