package com.finflow.moneytracker.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.ui.budget.BudgetViewModel
import com.finflow.moneytracker.ui.transactions.TransactionsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            TransactionsViewModel(
                transactionRepository = moneyTrackerApplication().container.transactionRepository,
                categoryRepository = moneyTrackerApplication().container.categoryRepository,
                walletRepository = moneyTrackerApplication().container.walletRepository
            )
        }

        initializer {
            BudgetViewModel(
                walletRepository = moneyTrackerApplication().container.walletRepository,
                categoryRepository = moneyTrackerApplication().container.categoryRepository,
                transactionRepository = moneyTrackerApplication().container.transactionRepository
            )
        }
    }
}

// Hàm hỗ trợ để lấy được class Application
fun CreationExtras.moneyTrackerApplication(): MoneyTrackerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as MoneyTrackerApplication)