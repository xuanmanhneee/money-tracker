package com.finflow.moneytracker.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.databinding.BottomSheetBudgetEditorBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BudgetEditorBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBudgetEditorBinding? = null
    private val binding get() = _binding!!

    private var availableCategoriesProvider: (() -> List<Category>)? = null
    private var categories: List<Category> = emptyList()

    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String = ""

    private var initialLimitAmount: Long = 0L
    private var isEditMode: Boolean = false

    private var onSave: ((Long, Long) -> Unit)? = null
    private var onDelete: ((Long) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBudgetEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUi()
        setupActions()
    }

    private fun setupUi() {
        if (isEditMode) {
            setupEditMode()
        } else {
            setupCreateMode()
        }
    }

    private fun setupCreateMode() {
        binding.tvTitle.text = "Thêm giới hạn"
        binding.actvCategory.visibility = View.VISIBLE
        binding.tvSelectedCategory.visibility = View.GONE
        binding.btnDelete.visibility = View.GONE

        categories = availableCategoriesProvider?.invoke().orEmpty()

        if (categories.isEmpty()) {
            binding.actvCategory.isEnabled = false
            binding.actvCategory.setText("Không còn danh mục nào")
            binding.btnSave.isEnabled = false
            return
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.name }
        )

        binding.actvCategory.setAdapter(adapter)

        binding.actvCategory.setOnClickListener {
            binding.actvCategory.showDropDown()
        }

        binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
        }
    }

    private fun setupEditMode() {
        binding.tvTitle.text = "Sửa giới hạn"
        binding.actvCategory.visibility = View.GONE
        binding.tvSelectedCategory.visibility = View.VISIBLE
        binding.tvSelectedCategory.text = selectedCategoryName
        binding.etLimitAmount.setText(initialLimitAmount.toString())
        binding.btnDelete.visibility = View.VISIBLE
    }

    private fun setupActions() {
        binding.btnSave.setOnClickListener {
            val categoryId = selectedCategoryId ?: return@setOnClickListener

            val amount = binding.etLimitAmount.text
                ?.toString()
                ?.trim()
                ?.toLongOrNull()
                ?: return@setOnClickListener

            if (amount <= 0L) return@setOnClickListener

            onSave?.invoke(categoryId, amount)
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            val categoryId = selectedCategoryId ?: return@setOnClickListener

            onDelete?.invoke(categoryId)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newCreateInstance(
            availableCategoriesProvider: () -> List<Category>,
            onSave: (categoryId: Long, limitAmount: Long) -> Unit
        ): BudgetEditorBottomSheet {
            return BudgetEditorBottomSheet().apply {
                this.availableCategoriesProvider = availableCategoriesProvider
                this.onSave = onSave
                this.isEditMode = false
            }
        }

        fun newEditInstance(
            categoryId: Long,
            categoryName: String,
            initialLimitAmount: Long,
            onSave: (categoryId: Long, limitAmount: Long) -> Unit,
            onDelete: (categoryId: Long) -> Unit
        ): BudgetEditorBottomSheet {
            return BudgetEditorBottomSheet().apply {
                this.selectedCategoryId = categoryId
                this.selectedCategoryName = categoryName
                this.initialLimitAmount = initialLimitAmount
                this.onSave = onSave
                this.onDelete = onDelete
                this.isEditMode = true
            }
        }
    }
}