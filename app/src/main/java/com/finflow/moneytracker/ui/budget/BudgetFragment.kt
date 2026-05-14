package com.finflow.moneytracker.ui.budget

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        categoryAdapter = BudgetCategoryAdapter(
            onItemClick = { item ->
                openEditBudgetSheet(item)
            }
        )

        binding.rvBudgetCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupActions() {
        binding.btnAddBudget.setOnClickListener {
            openCreateBudgetSheet()
        }
    }

    private fun openCreateBudgetSheet() {
        BudgetEditorBottomSheet
            .newCreateInstance(
                availableCategoriesProvider = {
                    viewModel.getAvailableCategories()
                },
                onSave = { categoryId: Long, limitAmount: Long ->
                    viewModel.setCategoryBudgetLimit(categoryId, limitAmount)
                }
            )
            .show(parentFragmentManager, "BudgetEditorBottomSheet")
    }

    private fun openEditBudgetSheet(item: BudgetCategoryProgressUi) {
        BudgetEditorBottomSheet
            .newEditInstance(
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                initialLimitAmount = item.allocated,
                onSave = { categoryId: Long, limitAmount: Long ->
                    viewModel.setCategoryBudgetLimit(categoryId, limitAmount)
                },
                onDelete = { categoryId: Long ->
                    viewModel.removeCategoryBudgetLimit(categoryId)
                }
            )
            .show(parentFragmentManager, "BudgetEditorBottomSheet")
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

        val remainingPercent = (100 - state.usagePercent).coerceAtLeast(0)

        binding.tvRemaining.text = if (state.remaining >= 0L) {
            "${formatCurrency(state.remaining)} ($remainingPercent%)"
        } else {
            "Lố ${formatCurrency(-state.remaining)}"
        }

        binding.tvRemaining.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                when {
                    state.remaining < 0L -> R.color.status_expense
                    state.usagePercent >= 80 -> R.color.status_warning
                    else -> R.color.status_income
                }
            )
        )

        binding.pbTotalUsage.progress = state.usagePercent.coerceAtMost(100)

        binding.pbTotalUsage.progressTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                when {
                    state.usagePercent >= 100 -> R.color.status_expense
                    state.usagePercent >= 80 -> R.color.status_warning
                    else -> R.color.status_income
                }
            )
        )

        categoryAdapter.submitList(state.categoryProgress)
    }

    private fun formatCurrency(value: Long): String {
        return NumberFormat
            .getCurrencyInstance(Locale("vi", "VN"))
            .format(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}