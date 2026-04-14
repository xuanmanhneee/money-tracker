package com.finflow.moneytracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TransactionsViewModel(private val repository: TransactionRepository) : ViewModel() {

    // Lấy Stream từ Repo và biến nó thành StateFlow để UI dễ dàng đọc được
    val transactionsUiState: StateFlow<List<Transaction>> =
        repository.getAllTransactionsStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList() // Ban đầu danh sách rỗng
            )
}