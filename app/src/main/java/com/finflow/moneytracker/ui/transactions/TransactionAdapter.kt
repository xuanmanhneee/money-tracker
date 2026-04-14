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
import com.finflow.moneytracker.data.local.entity.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory)

        fun bind(transaction: Transaction) {
            tvCategoryName.text = "Danh mục" 
            tvNote.text = transaction.note

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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
                return oldItem == newItem
            }
        }
    }
}