package com.finflow.moneytracker.ui.overview

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.dao.TransactionDao
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
                launch {
                    viewModel.totalBalance.collect { balance ->
                        actualBalance = balance
                        updateBalanceUI()
                    }
                }

                launch {
                    viewModel.overviewStats.collect { stats ->
                        tvTotalExpense.text = formatCurrency(stats.expense.toDouble())
                        tvTotalIncome.text = formatCurrency(stats.income.toDouble())
                    }
                }

                // Observe chart data — tự cập nhật khi period/tab thay đổi
                launch {
                    viewModel.chartData.collect { points ->
                        setupChart(points)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        ivTotalBalanceVisibility.setOnClickListener { toggleBalanceVisibility() }
        btnViewDetails.setOnClickListener { showWalletManagementPopup() }

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

    private fun showWalletManagementPopup() {
        val dialog = Dialog(requireContext())
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_wallet_management, null)
            dialog.setContentView(dialogView)

            val rvWallets = dialogView.findViewById<RecyclerView>(R.id.rvWallets)
            val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

            // Thiết lập Adapter cho danh sách ví
            val adapter = WalletAdapter()
            rvWallets.layoutManager = LinearLayoutManager(context)
            rvWallets.adapter = adapter

            // Lắng nghe danh sách ví từ ViewModel
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.allWallets.collect { wallets ->
                    adapter.submitList(wallets)
                }
            }

            btnClose?.setOnClickListener { dialog.dismiss() }

            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi hiển thị danh sách ví", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectExpenseTab() {
        isExpenseTabSelected = true
        viewModel.setExpenseTab(true)
        animateDividerColor(viewDividerLeft, ContextCompat.getColor(requireContext(), R.color.red))
        animateDividerColor(viewDividerRight, ContextCompat.getColor(requireContext(), R.color.divider_inactive))
    }

    private fun selectIncomeTab() {
        isExpenseTabSelected = false
        viewModel.setExpenseTab(false)
        animateDividerColor(viewDividerLeft, ContextCompat.getColor(requireContext(), R.color.divider_inactive))
        animateDividerColor(viewDividerRight, ContextCompat.getColor(requireContext(), R.color.blue))
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

    // Xóa setupChart cũ, thay bằng cái này
    private fun setupChart(points: List<TransactionDao.ChartPoint>) {
        val maxAxis = getCurrentMaxAxis()
        val chartColor = ContextCompat.getColor(
            requireContext(),
            if (isExpenseTabSelected) R.color.red else R.color.blue
        )
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        // Map ChartPoint → Entry, thêm điểm đầu/cuối để chart không bị cắt
        val entries = buildList {
            add(Entry(1f, 0f))  // điểm neo đầu
            points.forEach { add(Entry(it.xValue.toFloat(), it.yValue.toFloat())) }
            add(Entry(maxAxis, 0f))  // điểm neo cuối
        }.sortedBy { it.x }

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
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                this.textColor = textColor
                setDrawGridLines(false)
                axisMinimum = 1f
                axisMaximum = maxAxis
            }
            axisLeft.apply {
                this.textColor = textColor
                setDrawGridLines(true)
                gridColor = Color.GRAY
            }
            axisRight.isEnabled = false
            animateX(500)
            invalidate()
        }
    }
}