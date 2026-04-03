package com.example.moneytracker.ui.transactions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.R
import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.ui.add_transaction.TransactionDetailFragment

class TransactionsFragment : Fragment(R.layout.fragment_transactions) {

    private lateinit var rvTransactions: RecyclerView
    private val transactionRepository = TransactionRepository.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTransactions = view.findViewById(R.id.rvTransactions)
        
        // Hiển thị danh sách transactions
        updateTransactionsList()
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật danh sách khi quay lại screen
        updateTransactionsList()
    }

    private fun updateTransactionsList() {
        val transactions = transactionRepository.getAllTransactions()
        val adapter = TransactionAdapter(transactions) { transaction ->
            showTransactionDetail(transaction)
        }
        rvTransactions.adapter = adapter
    }

    private fun showTransactionDetail(transaction: Transaction) {
        val fragment = TransactionDetailFragment(transaction)
        fragment.show(parentFragmentManager, "TransactionDetail")
    }
}