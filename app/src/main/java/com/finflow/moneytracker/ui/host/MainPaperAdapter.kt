package com.finflow.moneytracker.ui.host

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.finflow.moneytracker.ui.account.AccountFragment
import com.finflow.moneytracker.ui.budget.BudgetFragment
import com.finflow.moneytracker.ui.overview.OverviewFragment
import com.finflow.moneytracker.ui.transactions.TransactionsFragment

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

