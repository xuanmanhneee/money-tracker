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
            layoutManager = LinearLayoutManager(requireContext()) // ← thêm dòng này
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupActions() {
        // Hành động bấm nút (+) để thêm Budget từ Category
        binding.btnAddBudget.setOnClickListener {
            // TODO: Mở BottomSheet lọc danh mục chưa có limit
            // ví dụ: showCategorySelectionBottomSheet()
        }

        // Nếu bạn muốn người dùng bấm vào text tháng để chọn tháng khác (lối thoát phụ)
        binding.cardBudgetSummary.findViewById<View>(R.id.tvCurrentMonth)?.setOnClickListener {
            // TODO: Hiển thị Month Picker nếu thực sự cần
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: BudgetUiState) {
        binding.tvCurrentMonth.text = "Tháng ${state.periodLabel}"
        binding.tvTotalBudget.text = formatCurrency(state.plannedBudget)

        // Tính % còn lại
        val remainingPercent = (100 - state.usagePercent).coerceAtLeast(0)
        val remainingMoney = formatCurrency(state.remaining)

        // Hiển thị: 1.400.000 ₫ (94%)
        binding.tvRemaining.text = "$remainingMoney ($remainingPercent%)"

        // Thanh tiến độ vẫn thể hiện mức ĐÃ TIÊU để người dùng thấy thanh đang chạy từ trái sang phải
        binding.pbTotalUsage.progress = state.usagePercent.coerceAtMost(100)

        // Đổi màu thanh dựa trên % ĐÃ TIÊU
        binding.pbTotalUsage.progressTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), when {
                state.usagePercent >= 100 -> R.color.status_expense // Hết tiền (Đỏ)
                state.usagePercent >= 80 -> R.color.status_warning  // Sắp hết (Vàng)
                else -> R.color.status_income                       // An toàn (Xanh)
            })
        )

        // Danh sách danh mục bên dưới
        categoryAdapter.submitList(state.categoryProgress)
    }

    private fun formatCurrency(value: Long): String {
        return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}