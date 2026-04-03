package com.example.moneytracker.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R
import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onTransactionClick: (Transaction) -> Unit = {}
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view, onTransactionClick)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    class TransactionViewHolder(
        itemView: android.view.View,
        private val onTransactionClick: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon = itemView.findViewById<ImageView>(R.id.ivTransactionIcon)
        private val tvCategory = itemView.findViewById<TextView>(R.id.tvTransactionCategory)
        private val tvAmount = itemView.findViewById<TextView>(R.id.tvTransactionAmount)
        private val tvDate = itemView.findViewById<TextView>(R.id.tvTransactionDate)
        private val tvPaymentMethod = itemView.findViewById<TextView>(R.id.tvTransactionPaymentMethod)
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi"))
        private var currentTransaction: Transaction? = null

        init {
            itemView.setOnClickListener {
                currentTransaction?.let { onTransactionClick(it) }
            }
        }

        fun bind(transaction: Transaction) {
            currentTransaction = transaction
            
            ivIcon.setImageResource(transaction.category.icon)
            tvCategory.text = transaction.category.name
            
            // Hiển thị số tiền với dấu +/- dựa trên loại giao dịch
            val amountText = if (transaction.category.type == TransactionType.INCOME) {
                "+${String.format("%,.0f", transaction.amount)}"
            } else {
                "-${String.format("%,.0f", transaction.amount)}"
            }
            tvAmount.text = amountText
            
            // Đổi màu dựa trên loại giao dịch
            if (transaction.category.type == TransactionType.INCOME) {
                tvAmount.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                tvAmount.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }
            
            tvDate.text = dateFormat.format(transaction.date)
            tvPaymentMethod.text = transaction.paymentMethod.displayName
        }
    }
}
