package com.finflow.moneytracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Wallet
import java.text.NumberFormat
import java.util.Locale

class WalletAdapter : ListAdapter<Wallet, WalletAdapter.WalletViewHolder>(WalletDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet, parent, false) // Sử dụng file item_wallet.xml đã tạo
        return WalletViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WalletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvWalletName)
        private val tvBalance: TextView = itemView.findViewById(R.id.tvWalletBalance)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivWalletIcon)

        fun bind(wallet: Wallet) {
            tvName.text = wallet.name
            tvBalance.text = formatCurrency(wallet.balance.toDouble())
            // Bạn có thể xử lý đổi màu icon hoặc load ảnh ở đây nếu có
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            return formatter.format(amount).replace("₫", "").trim() + " ₫"
        }
    }

    // Lớp giúp RecyclerView nhận biết item nào thay đổi để cập nhật thay vì vẽ lại cả danh sách
    class WalletDiffCallback : DiffUtil.ItemCallback<Wallet>() {
        override fun areItemsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
            return oldItem == newItem
        }
    }
}