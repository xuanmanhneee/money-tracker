package com.finflow.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.ui.common.CategoryIconResolver
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CategorySelectionFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_SELECTED_CATEGORY_ID = "arg_selected_category_id"

        fun newInstance(selectedCategoryId: String?): CategorySelectionFragment {
            return CategorySelectionFragment().apply {
                arguments = Bundle().apply {
                    if (selectedCategoryId != null) {
                        putString(ARG_SELECTED_CATEGORY_ID, selectedCategoryId)
                    }
                }
            }
        }
    }

    private lateinit var gridCategories: GridLayout
    private lateinit var ivBackCategory: ImageView
    private var selectedCategoryId: String? = null

    private val categoryRepository by lazy {
        (requireActivity().application as MoneyTrackerApplication).container.categoryRepository
    }

    // Callback interface để gửi dữ liệu về AddTransactionBottomSheet
    interface OnCategorySelectedListener {
        fun onCategorySelected(category: Category)
    }

    private var listener: OnCategorySelectedListener? = null

    private var categoriesList: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_category_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gridCategories = view.findViewById(R.id.gridCategories)
        ivBackCategory = view.findViewById(R.id.ivBackCategory)
        selectedCategoryId = arguments
            ?.takeIf { it.containsKey(ARG_SELECTED_CATEGORY_ID) }
            ?.getString(ARG_SELECTED_CATEGORY_ID)

        // Tổ chức lại GridLayout
        gridCategories.columnCount = 3

        // Render categories
        observeCategories()

        // Nút quay lại
        ivBackCategory.setOnClickListener {
            dismiss()
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            categoryRepository.getAllCategoriesStream().collect { categories ->
                categoriesList = categories
                renderCategories()
            }
        }
    }

    private fun renderCategories() {
        gridCategories.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        // Thêm các nhóm vào grid
        categoriesList.forEach { category ->
            val categoryView = inflater.inflate(R.layout.item_category, gridCategories, false)

            val iconView = categoryView.findViewById<ImageView>(R.id.ivCategoryIcon)
            val nameView = categoryView.findViewById<TextView>(R.id.tvCategoryName)

            iconView.setImageResource(
                CategoryIconResolver.resolveCategoryIconRes(
                    iconName = category.icon,
                    categoryName = category.name,
                    categoryType = category.type
                )
            )
            nameView.text = category.name
            val isSelected = category.id == selectedCategoryId
            applySelectionState(categoryView, nameView, iconView, isSelected)

            categoryView.setOnClickListener {
                animateSelection(categoryView) {
                    selectedCategoryId = category.id
                    listener?.onCategorySelected(category)
                    dismiss()
                }
            }

            // Tạo LayoutParams cho GridLayout
            val params = GridLayout.LayoutParams()
            params.width = (resources.displayMetrics.widthPixels / 3) - 24
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.setMargins(8, 8, 8, 8)
            categoryView.layoutParams = params

            gridCategories.addView(categoryView)
            animateItemEntrance(categoryView, gridCategories.childCount - 1)
        }

        // Thêm nút "Thêm nhóm mới"
        addNewCategoryButton(inflater)
    }

    private fun addNewCategoryButton(inflater: LayoutInflater) {
        val addCategoryView = inflater.inflate(R.layout.item_category, gridCategories, false)

        val iconView = addCategoryView.findViewById<ImageView>(R.id.ivCategoryIcon)
        val nameView = addCategoryView.findViewById<TextView>(R.id.tvCategoryName)

        iconView.setImageResource(R.drawable.ic_add)
        nameView.text = "Thêm mới"

        addCategoryView.setOnClickListener {
            animateSelection(addCategoryView) {
                showAddCategoryDialog()
            }
        }

        applySelectionState(
            itemView = addCategoryView,
            nameView = nameView,
            iconView = iconView,
            isSelected = false
        )

        val params = GridLayout.LayoutParams()
        params.width = (resources.displayMetrics.widthPixels / 3) - 24
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.setMargins(8, 8, 8, 8)
        addCategoryView.layoutParams = params

        gridCategories.addView(addCategoryView)
        animateItemEntrance(addCategoryView, gridCategories.childCount - 1)
    }

    private fun showAddCategoryDialog() {
        val dialog = AddCategoryDialogFragment()
        dialog.setOnCategoryAddedListener(object : AddCategoryDialogFragment.OnCategoryAddedListener {
            override fun onCategoryAdded(category: Category) {
                viewLifecycleOwner.lifecycleScope.launch {
                    categoryRepository.insertCategory(category)
                }
            }
        })
        dialog.show(parentFragmentManager, "AddCategory")
    }

    fun setOnCategorySelectedListener(listener: OnCategorySelectedListener) {
        this.listener = listener
    }

    override fun getTheme(): Int = R.style.ThemeOverlay_MoneyTracker_BottomSheet

    private fun applySelectionState(
        itemView: View,
        nameView: TextView,
        iconView: ImageView,
        isSelected: Boolean
    ) {
        itemView.background = ContextCompat.getDrawable(
            requireContext(),
            if (isSelected) R.drawable.bg_selectable_item_selected else R.drawable.bg_selectable_item_default
        )
        nameView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.nav_item_active else R.color.text_primary
            )
        )
        iconView.alpha = if (isSelected) 1f else 0.9f
        itemView.scaleX = if (isSelected) 1.03f else 1f
        itemView.scaleY = if (isSelected) 1.03f else 1f
    }

    private fun animateSelection(target: View, onEnd: () -> Unit) {
        target.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(80)
            .withEndAction {
                target.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .withEndAction(onEnd)
                    .start()
            }
            .start()
    }

    private fun animateItemEntrance(target: View, index: Int) {
        target.alpha = 0f
        target.translationY = 12f
        target.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay((index * 22L).coerceAtMost(150L))
            .setDuration(180)
            .start()
    }
}
