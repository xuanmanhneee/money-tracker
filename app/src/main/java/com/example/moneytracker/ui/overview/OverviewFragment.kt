package com.example.moneytracker.ui.overview

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.moneytracker.R
import com.github.mikephil.charting.charts.LineChart
import java.text.NumberFormat
import java.util.Locale
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis

class OverviewFragment : Fragment(R.layout.fragment_overview){
    private lateinit var tvTotalBalance: TextView
    private lateinit var ivTotalBalance: ImageView
    private lateinit var btnSearch: ImageButton
    private lateinit var btnNotification: ImageButton
    private lateinit var radioGroupPeriod: RadioGroup
    private lateinit var layoutExpense: LinearLayout
    private lateinit var layoutIncome: LinearLayout
    private lateinit var viewDividerLeft: View
    private lateinit var viewDividerRight: View
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var lineChart: LineChart

    private var isBalanceVisible = true
    private var actualBalance = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        loadData()
    }

    private fun initViews(view: View) {
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance)
        ivTotalBalance = view.findViewById(R.id.ivTotalBalance)
        btnSearch = view.findViewById(R.id.btnSearch)
        btnNotification = view.findViewById(R.id.btnNotification)
        radioGroupPeriod = view.findViewById(R.id.radioGroupPeriod)
        layoutExpense = view.findViewById(R.id.LayoutExpense)
        layoutIncome = view.findViewById(R.id.LayoutIncome)
        viewDividerLeft = view.findViewById(R.id.viewDividerLeft)
        viewDividerRight = view.findViewById(R.id.viewDividerRight)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        lineChart = view.findViewById(R.id.lineChart)
    }

    private fun setupListeners() {
        ivTotalBalance.setOnClickListener { toggleBalanceVisibility() }
        setupReportTabListeners()
    }

    private fun getHiddenBalance(balance: Double): String {
        val digitCount = Math.abs(balance.toLong()).toString().length
        return "*".repeat(digitCount) + " ₫"
    }

    private fun toggleBalanceVisibility() {
        isBalanceVisible = !isBalanceVisible
        tvTotalBalance.animate().alpha(0f).setDuration(150).withEndAction {
            if (isBalanceVisible) {
                tvTotalBalance.text = formatCurrency(actualBalance)
                ivTotalBalance.setImageResource(R.drawable.view)
            } else {
                tvTotalBalance.text = getHiddenBalance(actualBalance)
                ivTotalBalance.setImageResource(R.drawable.close_view)
            }
            tvTotalBalance.animate().alpha(1f).setDuration(150).start()
        }.start()
    }

    private fun loadData() {
        actualBalance = 4775000.0
        updateUI(actualBalance, 0.0, 472725055.0)
    }

    private fun updateUI(balance: Double, expense: Double, income: Double) {
        actualBalance = balance
        tvTotalBalance.text = if (isBalanceVisible) formatCurrency(balance) else getHiddenBalance(balance)
        ivTotalBalance.setImageResource(if (isBalanceVisible) R.drawable.view else R.drawable.close_view)
        tvTotalExpense.text = formatCurrency(expense)
        tvTotalIncome.text = formatCurrency(income)
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(amount).replace("₫", "").trim() + " ₫"
    }

    private fun setupReportTabListeners() {
        selectExpenseTab()
        layoutExpense.setOnClickListener { selectExpenseTab() }
        layoutIncome.setOnClickListener { selectIncomeTab() }
    }

    private fun selectExpenseTab() {
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.divider_inactive)
        animateDividerColor(viewDividerLeft, ContextCompat.getColor(requireContext(), R.color.red))
        animateDividerColor(viewDividerRight, inactiveColor)
        setupChart(isExpense = true)
    }

    private fun selectIncomeTab() {
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.divider_inactive)
        animateDividerColor(viewDividerLeft, inactiveColor)
        animateDividerColor(viewDividerRight, ContextCompat.getColor(requireContext(), R.color.blue))
        setupChart(isExpense = false)
    }

    private fun animateDividerColor(targetView: View, colorTo: Int) {
        val colorFrom = (targetView.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = 250
            addUpdateListener { animator -> targetView.setBackgroundColor(animator.animatedValue as Int) }
            start()
        }
    }

    private fun setupChart(isExpense: Boolean) {
        val entries = if (isExpense) {
            listOf(Entry(1f, 0f), Entry(5f, 320000f), Entry(10f, 450000f), Entry(20f, 390000f), Entry(31f, 0f))
        } else {
            listOf(Entry(1f, 0f), Entry(15f, 472725055f), Entry(31f, 472725055f))
        }

        val chartColor = ContextCompat.getColor(requireContext(), if (isExpense) R.color.red else R.color.blue)
        
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val textColor = typedValue.data

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

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                this.textColor = textColor
                setDrawGridLines(false)
                axisMinimum = 1f
                axisMaximum = 31f
            }

            axisLeft.apply {
                this.textColor = textColor
                setDrawGridLines(true)
                gridColor = if (textColor == Color.WHITE) Color.parseColor("#444444") else Color.parseColor("#DDDDDD")
                gridLineWidth = 0.5f
            }

            axisRight.isEnabled = false
            animateX(600)
            invalidate()
        }
    }
}
