package com.finflow.moneytracker.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.finflow.moneytracker.databinding.FragmentTransactionsBinding
import com.finflow.moneytracker.di.AppViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels {
        AppViewModelProvider.Factory
    }

    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilter()
        setupSearch()
        observeData()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            setPadding(0, 0, 0, 100)
            clipToPadding = false
        }
    }

    private fun setupFilter() {
        binding.btnFilter.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                menu.add("Hôm nay")
                menu.add("Tuần này")
                menu.add("Tháng này")
                menu.add("Năm nay")
                menu.add("Tùy chọn...")

                setOnMenuItemClickListener { item ->
                    when (item.title.toString()) {
                        "Hôm nay" -> {
                            binding.tvCurrentFilter.text = "Hôm nay"
                            viewModel.filterToday()
                        }

                        "Tuần này" -> {
                            binding.tvCurrentFilter.text = "Tuần này"
                            viewModel.filterThisWeek()
                        }

                        "Tháng này" -> {
                            binding.tvCurrentFilter.text = "Tháng này"
                            viewModel.filterThisMonth()
                        }

                        "Năm nay" -> {
                            binding.tvCurrentFilter.text = "Năm nay"
                            viewModel.filterThisYear()
                        }

                        "Tùy chọn..." -> {
                            showDateRangePicker()
                        }
                    }

                    true
                }

                show()
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Chọn khoảng thời gian")
            .setSelection(
                androidx.core.util.Pair(
                    MaterialDatePicker.thisMonthInUtcMilliseconds(),
                    MaterialDatePicker.todayInUtcMilliseconds()
                )
            )
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val startMillis = selection.first
            val endMillis = selection.second

            if (startMillis != null && endMillis != null) {
                viewModel.filterCustomRange(startMillis, endMillis)

                binding.tvCurrentFilter.text =
                    "${startMillis.formatDate()} - ${endMillis.formatDate()}"
            }
        }

        picker.show(parentFragmentManager, "TransactionDateRangePicker")
    }

    private fun setupSearch() {
        binding.edtSearch.doAfterTextChanged { text ->
            viewModel.updateSearchQuery(text?.toString().orEmpty())
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transactionsUiState.collect { transactions ->
                    transactionAdapter.submitList(transactions)
                }
            }
        }
    }

    private fun Long.formatDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(Date(this))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}