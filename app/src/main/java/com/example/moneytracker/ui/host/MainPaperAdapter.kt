package com.example.moneytracker.ui.host

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.moneytracker.ui.account.AccountFragment
import com.example.moneytracker.ui.budget.BudgetFragment
import com.example.moneytracker.ui.overview.OverviewFragment
import com.example.moneytracker.ui.transactions.TransactionsFragment

class MainPaperAdapter(
    activity: FragmentActivity) : FragmentStateAdapter(activity){

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OverviewFragment()
            1 -> BudgetFragment()
            2 -> TransactionsFragment()
            3 -> AccountFragment()
            else -> OverviewFragment()
        }
    }


}

