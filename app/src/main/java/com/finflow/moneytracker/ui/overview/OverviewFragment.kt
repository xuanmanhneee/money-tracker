package com.finflow.moneytracker.ui.overview

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class OverviewFragment : Fragment(R.layout.fragment_overview) {
    private lateinit var tvTotalBalance: TextView
    private lateinit var ivTotalBalanceVisibility: ImageView
    private lateinit var btnViewDetails: TextView
    private lateinit var radioGroupPeriod: RadioGroup
    private lateinit var layoutExpense: LinearLayout
    private lateinit var layoutIncome: LinearLayout
    private lateinit var viewDividerLeft: View
    private lateinit var viewDividerRight: View
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var lineChart: LineChart

    private lateinit var viewModel: OverviewViewModel

    private var isBalanceVisible = true
    private var actualBalance = 0.0
    private var isExpenseTabSelected = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = (requireActivity().application as MoneyTrackerApplication).container

        // Khởi tạo ViewModel qua Factory
        val factory = OverviewViewModelFactory(
            container.walletRepository,
            container.transactionRepository
        )

        viewModel = ViewModelProvider(this, factory)[OverviewViewModel::class.java]

        // Ánh xạ View và thiết lập sự kiện
        initViews(view)
        setupListeners()

        // Quan sát dữ liệu (Dữ liệu từ Local sẽ tự chảy về đây qua Flow)
        observeViewModel()
    }

    private fun initViews(view: View) {
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance)
        ivTotalBalanceVisibility = view.findViewById(R.id.ivTotalBalanceVisibility)
        btnViewDetails = view.findViewById(R.id.btnViewDetails)
        radioGroupPeriod = view.findViewById(R.id.radioGroupPeriod)
        layoutExpense = view.findViewById(R.id.LayoutExpense)
        layoutIncome = view.findViewById(R.id.LayoutIncome)
        viewDividerLeft = view.findViewById(R.id.viewDividerLeft)
        viewDividerRight = view.findViewById(R.id.viewDividerRight)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        lineChart = view.findViewById(R.id.lineChart)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Cập nhật Tổng số dư
                launch {
                    viewModel.totalBalance.collect { balance: Double ->
                        actualBalance = balance
                        updateBalanceUI()
                    }
                }

                // 2. Cập nhật Thu/Chi & Biểu đồ
                launch {
                    viewModel.overviewStats.collect { stats ->
                        tvTotalExpense.text = formatCurrency(stats.expense.toDouble())
                        tvTotalIncome.text = formatCurrency(stats.income.toDouble())
                        setupChart(isExpenseTabSelected, getCurrentMaxAxis())
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        ivTotalBalanceVisibility.setOnClickListener { toggleBalanceVisibility() }
        btnViewDetails.setOnClickListener {
            startActivity(Intent(requireContext(), WalletActivity::class.java))
        }

        radioGroupPeriod.setOnCheckedChangeListener { _, checkedId ->
            val period = when (checkedId) {
                R.id.rbThisWeek -> "WEEK"
                R.id.rbThisYear -> "YEAR"
                else -> "MONTH"
            }
            viewModel.setPeriod(period) // Update ViewModel để Repo lấy data mới
        }

        layoutExpense.setOnClickListener { selectExpenseTab() }
        layoutIncome.setOnClickListener { selectIncomeTab() }
    }

    private fun updateBalanceUI() {
        if (isBalanceVisible) {
            tvTotalBalance.text = formatCurrency(actualBalance)
            ivTotalBalanceVisibility.setImageResource(R.drawable.view)
        } else {
            tvTotalBalance.text = "******** ₫"
            ivTotalBalanceVisibility.setImageResource(R.drawable.close_view)
        }
    }

    private fun toggleBalanceVisibility() {
        isBalanceVisible = !isBalanceVisible
        tvTotalBalance.animate().alpha(0f).setDuration(150).withEndAction {
            updateBalanceUI()
            tvTotalBalance.animate().alpha(1f).setDuration(150).start()
        }.start()
    }

    private fun selectExpenseTab() {
        isExpenseTabSelected = true
        animateDividerColor(viewDividerLeft, ContextCompat.getColor(requireContext(), R.color.red))
        animateDividerColor(viewDividerRight, ContextCompat.getColor(requireContext(), R.color.divider_inactive))
        setupChart(true, getCurrentMaxAxis())
    }

    private fun selectIncomeTab() {
        isExpenseTabSelected = false
        animateDividerColor(viewDividerLeft, ContextCompat.getColor(requireContext(), R.color.divider_inactive))
        animateDividerColor(viewDividerRight, ContextCompat.getColor(requireContext(), R.color.blue))
        setupChart(false, getCurrentMaxAxis())
    }

    private fun getCurrentMaxAxis(): Float {
        return when (radioGroupPeriod.checkedRadioButtonId) {
            R.id.rbThisWeek -> 7f
            R.id.rbThisYear -> 12f
            else -> 31f
        }
    }

    private fun animateDividerColor(targetView: View, colorTo: Int) {
        val colorFrom = (targetView.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = 250
            addUpdateListener { animator -> targetView.setBackgroundColor(animator.animatedValue as Int) }
            start()
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(amount).replace("₫", "").trim() + " ₫"
    }

    private fun setupChart(isExpense: Boolean, maxAxisValue: Float) {
        // Ở đây bạn nên lấy dữ liệu thực tế từ ViewModel để vẽ Entry thay vì data mẫu
        val entries = listOf(Entry(1f, 0f), Entry(maxAxisValue, 0f))

        val chartColor = ContextCompat.getColor(requireContext(), if (isExpense) R.color.red else R.color.blue)
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        val dataSet = LineDataSet(entries, "").apply {
            color = chartColor
            setCircleColor(chartColor)
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawFilled(true)
            fillColor = chartColor
            fillAlpha = 40
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)

            setViewPortOffsets(35f, 0f, 35f, 0f)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                this.textColor = textColor
                setDrawGridLines(false)
                setDrawAxisLine(false)
                axisMinimum = entries.first().x
                axisMaximum = entries.last().x
                axisMinimum = 1f
                axisMaximum = maxAxisValue
            }



            axisLeft.apply {
                this.textColor = textColor
                setDrawGridLines(true)
                gridColor = Color.GRAY
            }
            axisRight.isEnabled = false
            animateX(500)

            // Vô hiệu hóa tương tác nếu chỉ muốn hiển thị tĩnh
            setTouchEnabled(false)

            animateX(600)
            invalidate()

        }
    }
}