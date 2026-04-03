package com.example.moneytracker.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.databinding.FragmentTransactionsBinding
import com.example.moneytracker.di.AppViewModelProvider
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {

    // Setup ViewBinding
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    // Lấy ViewModel từ Provider
    private val viewModel: TransactionsViewModel by viewModels {
        AppViewModelProvider.Factory
    }

    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            // Thêm chút khoảng trống ở cuối list để khi cuộn không bị dính vào cạnh dưới
            setPadding(0, 0, 0, 100)
            clipToPadding = false
        }
    }

    private fun observeData() {
        // Lắng nghe dữ liệu (StateFlow) từ ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.transactionsUiState.collect { transactionsList ->
                    // Mỗi khi DB thay đổi, ném list mới vào Adapter
                    transactionAdapter.submitList(transactionsList)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Tránh rò rỉ bộ nhớ khi Fragment bị hủy
        _binding = null
    }
}