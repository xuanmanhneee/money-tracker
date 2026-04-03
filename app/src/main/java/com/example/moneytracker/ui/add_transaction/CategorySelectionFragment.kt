package com.example.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.moneytracker.R
import com.example.moneytracker.data.model.Category
import com.example.moneytracker.data.model.TransactionType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CategorySelectionFragment : BottomSheetDialogFragment() {

    private lateinit var gridCategories: GridLayout
    private lateinit var ivBackCategory: ImageView

    // Callback interface để gửi dữ liệu về AddTransactionBottomSheet
    interface OnCategorySelectedListener {
        fun onCategorySelected(category: Category)
    }

    private var listener: OnCategorySelectedListener? = null
    
    // List để lưu trữ categories động
    private val categoriesList = mutableListOf(
        Category(1, "Ăn uống", R.drawable.ic_category, TransactionType.EXPENSE),
        Category(2, "Giao thông", R.drawable.ic_category, TransactionType.EXPENSE),
        Category(3, "Mua sắm", R.drawable.ic_category, TransactionType.EXPENSE),
        Category(4, "Giải trí", R.drawable.ic_category, TransactionType.EXPENSE),
        Category(5, "Sức khỏe", R.drawable.ic_category, TransactionType.EXPENSE),
        Category(6, "Giáo dục", R.drawable.ic_category, TransactionType.EXPENSE),
        Category(7, "Nhà ở", R.drawable.ic_category, TransactionType.EXPENSE),
        Category(8, "Lương", R.drawable.ic_category, TransactionType.INCOME),
        Category(9, "Freelance", R.drawable.ic_category, TransactionType.INCOME),
        Category(10, "Khác", R.drawable.ic_category, TransactionType.EXPENSE)
    )

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

        // Tổ chức lại GridLayout
        gridCategories.columnCount = 3

        // Render categories
        renderCategories()

        // Nút quay lại
        ivBackCategory.setOnClickListener {
            dismiss()
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

            iconView.setImageResource(category.icon)
            nameView.text = category.name

            categoryView.setOnClickListener {
                listener?.onCategorySelected(category)
                dismiss()
            }

            // Tạo LayoutParams cho GridLayout
            val params = GridLayout.LayoutParams()
            params.width = (resources.displayMetrics.widthPixels / 3) - 24
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.setMargins(8, 8, 8, 8)
            categoryView.layoutParams = params

            gridCategories.addView(categoryView)
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
            showAddCategoryDialog()
        }
        
        val params = GridLayout.LayoutParams()
        params.width = (resources.displayMetrics.widthPixels / 3) - 24
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.setMargins(8, 8, 8, 8)
        addCategoryView.layoutParams = params
        
        gridCategories.addView(addCategoryView)
    }

    private fun showAddCategoryDialog() {
        val dialog = AddCategoryDialogFragment()
        dialog.setOnCategoryAddedListener(object : AddCategoryDialogFragment.OnCategoryAddedListener {
            override fun onCategoryAdded(category: Category) {
                categoriesList.add(category)
                renderCategories()
            }
        })
        dialog.show(parentFragmentManager, "AddCategory")
    }

    fun setOnCategorySelectedListener(listener: OnCategorySelectedListener) {
        this.listener = listener
    }
}
