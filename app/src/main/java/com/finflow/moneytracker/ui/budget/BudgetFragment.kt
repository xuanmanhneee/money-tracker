package com.finflow.moneytracker.ui.budget

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.TextView
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
import com.finflow.moneytracker.ui.common.CurrencyInputFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
		observeUiState()
	}

	override fun onResume() {
		super.onResume()
		viewModel.refreshCurrentPeriod()
	}

	private fun setupRecyclerView() {
		categoryAdapter = BudgetCategoryAdapter()
		categoryAdapter.setOnCategoryClickListener { category ->
			showCategoryBudgetInputDialog(category)
		}
		binding.rvBudgetCategories.apply {
			layoutManager = LinearLayoutManager(requireContext())
			adapter = categoryAdapter
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
		binding.tvBudgetPeriod.text = "Kỳ ${state.periodLabel}"
		binding.tvBudgetName.text = state.budgetName.ifBlank { "Ngân sách" }
		binding.tvBudgetWallet.text = state.walletScopeLabel.ifBlank { "Phạm vi ví: ${state.walletName}" }
		binding.tvBudgetMode.text = "Tổng tự động từ ngân sách từng danh mục"
		binding.tvTotalBudget.text = formatCurrency(state.totalBudget)
		binding.tvSpent.text = formatCurrency(state.spent)
		binding.tvRemaining.text = formatCurrency(state.remaining)
		binding.tvUsagePercent.text = "${state.usagePercent}%"
		binding.btnCreateManual.visibility = View.GONE
		binding.btnEditBudget.visibility = View.GONE

		binding.pbBudgetUsage.progress = state.usagePercent.coerceAtMost(100)
		binding.pbBudgetUsage.progressTintList = ColorStateList.valueOf(
			when (state.progressState) {
				BudgetProgressState.SAFE -> ContextCompat.getColor(requireContext(), R.color.status_income)
				BudgetProgressState.WARNING -> ContextCompat.getColor(requireContext(), R.color.status_warning)
				BudgetProgressState.DANGER -> ContextCompat.getColor(requireContext(), R.color.status_expense)
			}
		)

		if (state.alertMessage.isNullOrBlank()) {
			binding.tvBudgetAlert.visibility = View.GONE
		} else {
			binding.tvBudgetAlert.visibility = View.VISIBLE
			val isDanger = state.progressState == BudgetProgressState.DANGER
			binding.tvBudgetAlert.setBackgroundColor(
				ContextCompat.getColor(
					requireContext(),
					if (isDanger) R.color.status_expense else R.color.status_warning
				)
			)
			binding.tvBudgetAlert.setTextColor(
				ContextCompat.getColor(
					requireContext(),
					if (isDanger) R.color.white else R.color.black
				)
			)
			binding.tvBudgetAlert.text = state.alertMessage
		}

		if (state.comparisonMessage.isNullOrBlank()) {
			binding.tvComparison.visibility = View.GONE
		} else {
			binding.tvComparison.visibility = View.VISIBLE
			binding.tvComparison.text = state.comparisonMessage
		}

		if (state.burnRateForecastMessage.isNullOrBlank()) {
			binding.tvForecast.visibility = View.GONE
		} else {
			binding.tvForecast.visibility = View.VISIBLE
			binding.tvForecast.text = state.burnRateForecastMessage
		}

		binding.tvEmptyState.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
		binding.groupContent.visibility = if (state.isEmpty) View.GONE else View.VISIBLE

		categoryAdapter.submitList(state.categoryProgress)
	}

	private fun showCategoryBudgetInputDialog(category: BudgetCategoryProgressUi) {
		val context = requireContext()
		val dialogView = layoutInflater.inflate(R.layout.dialog_budget_input, null)
		val dialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
		val currentBudgetLabel = dialogView.findViewById<TextView>(R.id.tvCurrentBudgetLabel)
		val currentBudgetValue = dialogView.findViewById<TextView>(R.id.tvCurrentBudgetValue)
		val inputLayout = dialogView.findViewById<TextInputLayout>(R.id.tilBudgetAmount)
		val input = dialogView.findViewById<TextInputEditText>(R.id.etBudgetAmount)
		val dialogHint = dialogView.findViewById<TextView>(R.id.tvDialogHint)
		CurrencyInputFormatter.attach(input)

		dialogTitle.text = "Ngân sách danh mục: ${category.categoryName}"
		currentBudgetLabel.text = "Ngân sách hiện tại"
		currentBudgetValue.text = formatCurrency(category.allocated)
		inputLayout.hint = "Ngân sách tháng mới"
		dialogHint.text = "Nhập 0 nếu muốn tạm thời không cấp ngân sách cho nhóm này."
		CurrencyInputFormatter.setAmount(input, category.allocated)

		val dialog = MaterialAlertDialogBuilder(context)
			.setView(dialogView)
			.setPositiveButton("Lưu", null)
			.setNegativeButton("Hủy", null)
			.create()

		dialog.setOnShowListener {
			dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				val value = CurrencyInputFormatter.parseAmountOrNull(input)
				if (value == null) {
					inputLayout.error = "Vui lòng nhập số tiền từ 0 trở lên"
					return@setOnClickListener
				}

				inputLayout.error = null
				viewModel.updateCategoryBudgetLimit(category.categoryId, value)
				dialog.dismiss()
			}
		}

		dialog.show()
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