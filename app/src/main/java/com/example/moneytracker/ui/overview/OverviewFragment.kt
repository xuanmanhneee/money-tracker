package com.example.moneytracker.ui.overview

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R
import com.example.moneytracker.data.model.PaymentMethod
import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.ui.add_transaction.TransactionDetailFragment
import com.example.moneytracker.ui.transactions.TransactionAdapter
import java.text.DecimalFormat

class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private lateinit var tvTotalBalance: TextView
    private lateinit var tvCashBalance: TextView
    private lateinit var tvBankBalance: TextView
    private lateinit var rvRecentTransactions: RecyclerView
    
    private val transactionRepository = TransactionRepository.getInstance()
    private val decimalFormat = DecimalFormat("#,##0")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalBalance = view.findViewById(R.id.tvTotalBalance)
        tvCashBalance = view.findViewById(R.id.tvCashBalance)
        tvBankBalance = view.findViewById(R.id.tvBankBalance)
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions)

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật UI khi quay lại screen
        updateUI()
    }

    private fun updateUI() {
        // Cập nhật số dư
        val totalBalance = transactionRepository.getTotalBalance()
        val cashBalance = transactionRepository.getWalletBalance(PaymentMethod.CASH)
        val bankBalance = transactionRepository.getWalletBalance(PaymentMethod.BANK)

        tvTotalBalance.text = "${decimalFormat.format(totalBalance)} ₫"
        tvCashBalance.text = "${decimalFormat.format(cashBalance)} ₫"
        tvBankBalance.text = "${decimalFormat.format(bankBalance)} ₫"

        // Hiển thị 5 giao dịch gần đây
        val allTransactions = transactionRepository.getAllTransactions()
        val recentTransactions = allTransactions.take(5)
        val adapter = TransactionAdapter(recentTransactions) { transaction ->
            showTransactionDetail(transaction)
        }
        rvRecentTransactions.adapter = adapter
    }

    private fun showTransactionDetail(transaction: Transaction) {
        val fragment = TransactionDetailFragment(transaction)
        fragment.show(parentFragmentManager, "TransactionDetail")
    }
}