package com.finflow.moneytracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository

class OverviewViewModelFactory(
    private val walletRepo: WalletRepository,
    private val transactionRepo: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OverviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OverviewViewModel(walletRepo, transactionRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}