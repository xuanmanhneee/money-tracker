package com.example.moneytracker.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moneytracker.MoneyTrackerApplication
import com.example.moneytracker.ui.transactions.TransactionsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            TransactionsViewModel(
                moneyTrackerApplication().container.transactionRepository
            )
        }
    }
}

// Hàm hỗ trợ để lấy được class Application
fun CreationExtras.moneyTrackerApplication(): MoneyTrackerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as MoneyTrackerApplication)