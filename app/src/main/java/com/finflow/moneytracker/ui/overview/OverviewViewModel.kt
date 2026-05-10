package com.finflow.moneytracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finflow.moneytracker.data.local.dao.TransactionDao
import com.finflow.moneytracker.data.repository.TransactionRepository
import com.finflow.moneytracker.data.repository.WalletRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class OverviewViewModel(
    private val walletRepo: WalletRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {


    // Lọc theo mốc thời gian (Tuần, Tháng, Năm)
    private val _selectedPeriod = MutableStateFlow("MONTH")
    val selectedPeriod = _selectedPeriod.asStateFlow()

    // 1. Tính tổng số dư từ tất cả các ví
    val totalBalance: StateFlow<Double> = walletRepo.getWalletsStream()
        .map { wallets ->
            // Ép kiểu kết quả cuối cùng sang Double
            wallets.sumOf { it.balance }.toDouble()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // 2. Lấy danh sách ví (để đổ vào Popup)
    val allWallets = walletRepo.getWalletsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Tính Tổng Thu/Chi dựa trên mốc thời gian
    val overviewStats = _selectedPeriod.flatMapLatest { period ->
        val range = getTimeRange(period)
        combine(
            transactionRepo.getTotalAmountStream(0, range.first, range.second), // 0: Chi
            transactionRepo.getTotalAmountStream(1, range.first, range.second)  // 1: Thu
        ) { expense, income ->
            Stats(expense ?: 0L, income ?: 0L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats(0, 0))

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    // Thêm vào OverviewViewModel, sau overviewStats

    // Tab đang chọn (expense/income) — Fragment sẽ update
    private val _isExpenseTab = MutableStateFlow(true)

    fun setExpenseTab(isExpense: Boolean) {
        _isExpenseTab.value = isExpense
    }

    // Flow chính: tự động re-emit khi đổi period hoặc đổi tab
    val chartData: StateFlow<List<TransactionDao.ChartPoint>> = combine(
        _selectedPeriod,
        _isExpenseTab
    ) { period, isExpense -> period to isExpense }
        .flatMapLatest { (period, isExpense) ->
            val range = getTimeRange(period)
            val type = if (isExpense) 0 else 1  // 0: Chi, 1: Thu — khớp với CategoryType
            transactionRepo.getChartDataStream(type, period, range.first, range.second)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun getTimeRange(period: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        when (period) {
            "WEEK" -> cal.add(Calendar.DAY_OF_YEAR, -7)
            "MONTH" -> cal.add(Calendar.MONTH, -1)
            "YEAR" -> cal.add(Calendar.YEAR, -1)
        }
        val startTime = cal.timeInMillis
        return Pair(startTime, endTime)
    }

    data class Stats(val expense: Long, val income: Long)
}