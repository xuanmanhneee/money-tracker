package com.example.moneytracker.ui.overview


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
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
import androidx.core.widget.TextViewCompat
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
    private lateinit var  radioGroupPeriod: RadioGroup

    private lateinit var layoutExpense: LinearLayout

    private lateinit var layoutIncome: LinearLayout

    private lateinit var viewDividerLeft: View
    private lateinit var viewDividerRight: View

    // Thêm khai báo
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

    private fun setupListeners()
    {
        btnSearch.setOnClickListener {
            // Xử lý khi người dùng nhấn nút Search
        }

        btnNotification.setOnClickListener {
            // Xử lý khi người dùng nhấn nút Notification
        }

        radioGroupPeriod.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbThisMonth -> {
                    // Xử lý khi người dùng chọn This Month
                }

                R.id.rbLastMonth -> {
                    // Xử lý khi người dùng chọn Last Month
                }
            }
        }

        ivTotalBalance.setOnClickListener {
            toggleBalanceVisibility()

        }

        setupReportTabListeners();

    }



// Hàm mới: Tạo chuỗi dấu sao tương ứng với số tiền
private fun getHiddenBalance(balance: Double): String {
    // 1. Chuyển số tiền thành số nguyên để loại bỏ phần thập phân (.0)
    // Dùng Math.abs để phòng trường hợp số âm thì không bị đếm nhầm dấu trừ
    val digitCount = Math.abs(balance.toLong()).toString().length

    // 2. Tạo ra một chuỗi dấu '*' có số lượng bằng đúng số chữ số vừa đếm được
    val stars = "*".repeat(digitCount)
    return "$stars ₫"
}

    // Cập nhật lại logic chuyển đổi hiển thị
    private fun toggleBalanceVisibility() {
        isBalanceVisible = !isBalanceVisible

        tvTotalBalance.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                if (isBalanceVisible) {
                    tvTotalBalance.text = formatCurrency(actualBalance)
                    ivTotalBalance.setImageResource(R.drawable.view)
                } else {

                    tvTotalBalance.text = getHiddenBalance(actualBalance)
                    ivTotalBalance.setImageResource(R.drawable.close_view)
                }

                tvTotalBalance.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun loadData()
    {
        actualBalance = 4775000.0 // Lấy từ database
        val totalExpense = 0.0
        val totalIncome = 472725055.0

        updateUI(actualBalance, totalExpense, totalIncome)
    }


    private fun updateUI(balance: Double, expense: Double, income: Double) {
        // Format tiền tệ Việt Nam
        actualBalance = balance
        if (isBalanceVisible) {
            tvTotalBalance.text = formatCurrency(balance)
            ivTotalBalance.setImageResource(R.drawable.view)
        } else{
            tvTotalBalance.text = getHiddenBalance(balance)
            ivTotalBalance.setImageResource(R.drawable.close_view)
        }

        tvTotalExpense.text = formatCurrency(expense)
        tvTotalIncome.text = formatCurrency(income)

    }

    @SuppressLint("SuspiciousIndentation")
    private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(amount).replace("₫", "").trim() + " ₫"
    }


    private fun setupReportTabListeners() {
        // Mặc định: chọn Tổng chi
        selectExpenseTab()

        layoutExpense.setOnClickListener {
            selectExpenseTab()
        }

        layoutIncome.setOnClickListener {
            selectIncomeTab()
        }
    }

    private fun selectExpenseTab() {
        // Nửa trái -> đỏ, nửa phải -> trắng
        animateDividerColor(viewDividerLeft, R.color.red)
        animateDividerColor(viewDividerRight, R.color.white)
        setupChart(isExpense = true)
    }

    private fun selectIncomeTab() {
        // Nửa trái -> trắng, nửa phải -> xanh
        animateDividerColor(viewDividerLeft, R.color.white)
        animateDividerColor(viewDividerRight, R.color.blue)
        setupChart(isExpense = false)
    }

    private fun animateDividerColor(targetView: View, colorRes: Int) {
        val colorFrom = (targetView.background as? ColorDrawable)?.color
            ?: ContextCompat.getColor(requireContext(), R.color.white)
        val colorTo = ContextCompat.getColor(requireContext(), colorRes)

        ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = 250 // ms
            addUpdateListener { animator ->
                targetView.setBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }
    }


    private fun loadCurrentMonthData()
    {
        // TODO: Load dữ liệu tháng hiện tại
        loadData()
    }

    private fun loadLastMonthData() {
        // TODO: Load dữ liệu tháng trước
        // Có thể dùng Calendar để tính toán
    }

    private fun onSearchClicked() {
        // TODO: search screen

    }

    private fun onNotificationClicked() {
        // TODO: notification screen
    }


    private fun setupChart(isExpense: Boolean) {
        // Dữ liệu mẫu theo ngày trong tháng
        val expenseEntries = listOf(
            Entry(1f, 0f),
            Entry(3f, 150000f),
            Entry(5f, 320000f),
            Entry(8f, 200000f),
            Entry(10f, 450000f),
            Entry(15f, 180000f),
            Entry(20f, 390000f),
            Entry(25f, 210000f),
            Entry(28f, 500000f),
            Entry(31f, 0f)
        )

        val incomeEntries = listOf(
            Entry(1f, 0f),
            Entry(3f, 472725055f),
            Entry(10f, 472725055f),
            Entry(20f, 472725055f),
            Entry(31f, 472725055f)
        )

        val entries = if (isExpense) expenseEntries else incomeEntries
        val color = if (isExpense)
            ContextCompat.getColor(requireContext(), R.color.red)
        else
            ContextCompat.getColor(requireContext(), R.color.blue)

        val dataSet = LineDataSet(entries, "").apply {
            this.color = color
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 3f
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 30
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setDrawGridBackground(false)
            setBackgroundColor(android.graphics.Color.parseColor("#2E2E2E"))

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = android.graphics.Color.WHITE
                setDrawGridLines(false)
                granularity = 1f
                axisMinimum = 1f
                axisMaximum = 31f
            }

            axisLeft.apply {
                textColor = android.graphics.Color.WHITE
                setDrawGridLines(true)
                gridColor = android.graphics.Color.parseColor("#444444")
            }

            axisRight.isEnabled = false
            animateX(500)
            invalidate()
        }
    }



}