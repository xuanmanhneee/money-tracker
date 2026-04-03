package com.example.moneytracker.ui.transactions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R
import com.example.moneytracker.data.local.entity.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback) {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory)

        fun bind(transaction: Transaction) {
            // Hiển thị text cơ bản
            tvCategoryName.text = "Danh mục" // Tạm thời để text, sau này map với CategoryDB
            tvNote.text = transaction.note

            // Format ngày tháng (từ Long timestamp sang text)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(transaction.date))

            // Format tiền tệ (VD: 50000 -> 50.000đ)
            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            val formattedAmount = "${formatter.format(transaction.amount)}đ"

            // Đổi màu tiền (Đỏ cho chi tiêu, Xanh cho thu nhập)
            if (transaction.amount < 0) {
                tvAmount.text = formattedAmount
                tvAmount.setTextColor(Color.parseColor("#F44336")) // Đỏ
            } else {
                tvAmount.text = "+$formattedAmount"
                tvAmount.setTextColor(Color.parseColor("#4CAF50")) // Xanh
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

    // Công cụ giúp RecyclerView biết item nào mới để tạo hiệu ứng mượt mà
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