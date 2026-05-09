package com.finflow.moneytracker.ui.overview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Wallet
import java.text.DecimalFormat

class WalletAdapter(
    private val onEditClick: (Wallet) -> Unit,
    private val onDeleteClick: (Wallet) -> Unit,
) : ListAdapter<Wallet, WalletAdapter.WalletViewHolder>(WalletDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallet, parent, false)
        return WalletViewHolder(view, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WalletViewHolder(
        itemView: View,
        private val onEditClick: (Wallet) -> Unit,
        private val onDeleteClick: (Wallet) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvWalletName: TextView = itemView.findViewById(R.id.tvWalletName)
        private val tvWalletBalance: TextView = itemView.findViewById(R.id.tvWalletBalance)
        private val btnEditContainer: View = itemView.findViewById(R.id.btnEditWalletContainer)
        private val btnDeleteContainer: View = itemView.findViewById(R.id.btnDeleteWalletContainer)
        private val formatter = DecimalFormat("#,###")

        fun bind(wallet: Wallet) {
            tvWalletName.text = wallet.name
            val formattedBalance = formatter.format(wallet.balance).replace(",", ".")
            tvWalletBalance.text = itemView.context.getString(R.string.balance_format, formattedBalance)
            
            btnEditContainer.setOnClickListener {
                onEditClick(wallet)
            }
            
            btnDeleteContainer.setOnClickListener {
                onDeleteClick(wallet)
            }
        }
    }

    class WalletDiffCallback : DiffUtil.ItemCallback<Wallet>() {
        override fun areItemsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
            return oldItem == newItem
        }
    }
}