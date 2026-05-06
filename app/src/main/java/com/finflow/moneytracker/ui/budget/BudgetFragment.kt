package com.finflow.moneytracker.ui.budget

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.finflow.moneytracker.R
import com.finflow.moneytracker.databinding.FragmentBudgetBinding
import com.finflow.moneytracker.di.AppViewModelProvider
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetViewModel by viewModels {
        AppViewModelProvider.Factory
    }

    private lateinit var categoryAdapter: BudgetCategoryAdapter
    private var latestState: BudgetUiState = BudgetUiState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupActions()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCurrentPeriod()
    }

    private fun setupRecyclerView() {
        categoryAdapter = BudgetCategoryAdapter()
        binding.rvBudgetCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    latestState = state
                    renderState(state)
                }
            }
        }
    }

    private fun setupActions() {
        // Trong kiến trúc mới, ngân sách dựa trên hạn mức của từng category.
        // Các nút chỉnh sửa ngân sách tổng sẽ được ẩn hoặc dẫn tới màn hình quản lý category.
        binding.btnCreateManual.visibility = View.GONE
        binding.btnEditBudget.visibility = View.GONE
    }

    private fun renderState(state: BudgetUiState) {
        binding.tvBudgetPeriod.text = "Kỳ ${state.periodLabel}"
        binding.tvBudgetName.text = "Ngân sách tháng"
        binding.tvBudgetWallet.text = "Tất cả ví"
        binding.tvBudgetMode.text = "Dựa trên hạn mức nhóm"
        
        // Cập nhật các trường số tiền
        binding.tvTotalBudget.text = formatCurrency(state.plannedBudget)
        binding.tvSpent.text = formatCurrency(state.spent)
        binding.tvRemaining.text = formatCurrency(state.remaining)
        binding.tvUsagePercent.text = "${state.usagePercent}%"

        // Màu sắc thanh tiến độ
        binding.pbBudgetUsage.progress = state.usagePercent.coerceAtMost(100)
        binding.pbBudgetUsage.progressTintList = ColorStateList.valueOf(
            when (state.progressState) {
                BudgetProgressState.SAFE -> ContextCompat.getColor(requireContext(), R.color.status_income)
                BudgetProgressState.WARNING -> ContextCompat.getColor(requireContext(), R.color.status_warning)
                BudgetProgressState.DANGER -> ContextCompat.getColor(requireContext(), R.color.status_expense)
            }
        )

        // Hiển thị tin nhắn cảnh báo
        if (state.alertMessage.isNullOrBlank()) {
            binding.tvBudgetAlert.visibility = View.GONE
        } else {
            binding.tvBudgetAlert.visibility = View.VISIBLE
            binding.tvBudgetAlert.text = state.alertMessage
        }

        // Hiển thị so sánh với tháng trước
        if (state.comparisonMessage.isNullOrBlank()) {
            binding.tvComparison.visibility = View.GONE
        } else {
            binding.tvComparison.visibility = View.VISIBLE
            binding.tvComparison.text = state.comparisonMessage
        }

        // Trạng thái trống (nếu chưa có category nào đặt hạn mức)
        binding.tvEmptyState.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
        binding.groupContent.visibility = if (state.isEmpty) View.GONE else View.VISIBLE

        categoryAdapter.submitList(state.categoryProgress)
    }

    private fun formatCurrency(value: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(value).replace("₫", "").trim() + " ₫"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
