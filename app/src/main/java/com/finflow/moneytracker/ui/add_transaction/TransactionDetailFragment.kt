package com.finflow.moneytracker.ui.add_transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.local.entity.Wallet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TransactionDetailFragment(private val transaction: Transaction) : BottomSheetDialogFragment(),
    CategorySelectionFragment.OnCategorySelectedListener,
    PaymentMethodSelectionFragment.OnPaymentMethodSelectedListener {

    companion object {
        private const val TYPE_EXPENSE = 0
        private const val DEFAULT_LOCAL_USER = "local_user"
        private const val CASH_WALLET_NAME = "Tiền mặt"
        private const val BANK_WALLET_NAME = "Ngân hàng"
        private const val LEGACY_DEFAULT_WALLET_NAME = "Ví mặc định"
    }

    private lateinit var ivBackDetail: ImageView
    private lateinit var ivDelete: ImageView
    private lateinit var etDetailAmount: EditText
    private lateinit var etDetailNotes: EditText
    private lateinit var tvDetailWorkHours: TextView
    private lateinit var txtDetailDate: TextView
    private lateinit var btnDetailPrev: ImageButton
    private lateinit var btnDetailNext: ImageButton
    private lateinit var btnDetailSelectCategory: LinearLayout
    private lateinit var tvDetailCategory: TextView
    private lateinit var btnDetailSelectPaymentMethod: LinearLayout
    private lateinit var tvDetailPaymentMethod: TextView
    private lateinit var btnSaveTransaction: android.widget.Button

    private var selectedCategory: Category? = null
    private var selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH

    private val calendar: Calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi"))
    private val HOURLY_WAGE = 26000.0

    private val appContainer by lazy {
        (requireActivity().application as MoneyTrackerApplication).container
    }

    private val transactionRepository by lazy { appContainer.transactionRepository }
    private val categoryRepository by lazy { appContainer.categoryRepository }
    private val walletRepository by lazy { appContainer.walletRepository }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_transaction_detail,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivBackDetail = view.findViewById(R.id.ivBackDetail)
        ivDelete = view.findViewById(R.id.ivDelete)
        etDetailAmount = view.findViewById(R.id.etDetailAmount)
        etDetailNotes = view.findViewById(R.id.etDetailNotes)
        tvDetailWorkHours = view.findViewById(R.id.tvDetailWorkHours)
        txtDetailDate = view.findViewById(R.id.txtDetailDate)
        btnDetailPrev = view.findViewById(R.id.btnDetailPrev)
        btnDetailNext = view.findViewById(R.id.btnDetailNext)
        btnDetailSelectCategory = view.findViewById(R.id.btnDetailSelectCategory)
        tvDetailCategory = view.findViewById(R.id.tvDetailCategory)
        btnDetailSelectPaymentMethod = view.findViewById(R.id.btnDetailSelectPaymentMethod)
        tvDetailPaymentMethod = view.findViewById(R.id.tvDetailPaymentMethod)
        btnSaveTransaction = view.findViewById(R.id.btnSaveTransaction)

        calendar.timeInMillis = transaction.date
        updateDate()

        etDetailAmount.setText(abs(transaction.amount).toString())
        etDetailNotes.setText(transaction.note.orEmpty())
        calculateWorkHours()
        loadInitialData()

        ivBackDetail.setOnClickListener {
            dismiss()
        }

        ivDelete.setOnClickListener {
            deleteTransaction()
        }

        etDetailAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateWorkHours()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnDetailPrev.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            updateDate()
        }

        btnDetailNext.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            updateDate()
        }

        txtDetailDate.setOnClickListener {
            showDatePicker()
        }

        btnDetailSelectCategory.setOnClickListener {
            showCategorySelectionFragment()
        }

        btnDetailSelectPaymentMethod.setOnClickListener {
            showPaymentMethodSelectionFragment()
        }

        btnSaveTransaction.setOnClickListener {
            updateTransaction()
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            val categories = categoryRepository.getAllCategoriesStream().first()
            selectedCategory = categories.firstOrNull { it.id == transaction.categoryId }
            tvDetailCategory.text = selectedCategory?.name ?: "Danh mục #${transaction.categoryId}"

            val wallets = walletRepository.getWalletsStream().first()
            val selectedWallet = wallets.firstOrNull { it.id == transaction.walletId }
            selectedPaymentMethod = when {
                selectedWallet?.name.equals(BANK_WALLET_NAME, ignoreCase = true) -> PaymentMethod.BANK
                else -> PaymentMethod.CASH
            }
            tvDetailPaymentMethod.text = selectedPaymentMethod.displayName
        }
    }

    private fun updateDate() {
        txtDetailDate.text = sdf.format(calendar.time)
    }

    private fun calculateWorkHours() {
        val amountText = etDetailAmount.text.toString().trim()

        if (amountText.isEmpty()) {
            tvDetailWorkHours.text = "≈ 0 giờ làm việc"
            return
        }

        val amount = amountText.toDoubleOrNull() ?: 0.0
        val hours = amount / HOURLY_WAGE
        val roundedHours = round(hours * 100) / 100

        tvDetailWorkHours.text = "≈ $roundedHours giờ làm việc"
    }

    private fun showDatePicker() {
        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                updateDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun showCategorySelectionFragment() {
        val fragment = CategorySelectionFragment()
        fragment.setOnCategorySelectedListener(this)
        fragment.show(parentFragmentManager, "CategorySelection")
    }

    override fun onCategorySelected(category: Category) {
        selectedCategory = category
        tvDetailCategory.text = category.name
        tvDetailCategory.setTextColor(resources.getColor(R.color.text_primary, null))
    }

    private fun showPaymentMethodSelectionFragment() {
        val fragment = PaymentMethodSelectionFragment()
        fragment.setOnPaymentMethodSelectedListener(this)
        fragment.show(parentFragmentManager, "PaymentMethodSelection")
    }

    override fun onPaymentMethodSelected(paymentMethod: PaymentMethod) {
        selectedPaymentMethod = paymentMethod
        tvDetailPaymentMethod.text = paymentMethod.displayName
        tvDetailPaymentMethod.setTextColor(resources.getColor(R.color.text_primary, null))
    }

    private fun updateTransaction() {
        val amountText = etDetailAmount.text.toString().trim()
        if (amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn nhóm", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            Toast.makeText(requireContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show()
            return
        }

        val baseAmount = amount.roundToLong()
        val isExpense = selectedCategory?.type == TYPE_EXPENSE
        val signedAmount = if (isExpense) -abs(baseAmount) else abs(baseAmount)

        lifecycleScope.launch {
            val selectedWallet = getOrCreateWalletForPaymentMethod(selectedPaymentMethod)

            val updatedTransaction = Transaction(
                id = transaction.id,
                walletId = selectedWallet.id,
                categoryId = selectedCategory!!.id,
                amount = signedAmount,
                date = calendar.timeInMillis,
                note = etDetailNotes.text.toString().trim(),
                receiptImagePath = transaction.receiptImagePath,
                toWalletId = transaction.toWalletId
            )

            transactionRepository.updateTransaction(updatedTransaction)
            Toast.makeText(requireContext(), "Cập nhật giao dịch thành công", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private suspend fun getOrCreateWalletForPaymentMethod(paymentMethod: PaymentMethod): Wallet {
        val wallets = walletRepository.getWalletsStream().first()
        val targetWalletName = when (paymentMethod) {
            PaymentMethod.CASH -> CASH_WALLET_NAME
            PaymentMethod.BANK -> BANK_WALLET_NAME
        }

        val exactWallet = wallets.firstOrNull { wallet ->
            wallet.name.equals(targetWalletName, ignoreCase = true)
        }
        if (exactWallet != null) {
            return exactWallet
        }

        if (paymentMethod == PaymentMethod.CASH) {
            val legacyWallet = wallets.firstOrNull { wallet ->
                wallet.name.equals(LEGACY_DEFAULT_WALLET_NAME, ignoreCase = true)
            }
            if (legacyWallet != null) {
                return legacyWallet
            }

            val firstWallet = wallets.firstOrNull()
            if (firstWallet != null) {
                return firstWallet
            }
        }

        val createdWallet = Wallet(
            userId = DEFAULT_LOCAL_USER,
            name = targetWalletName,
            balance = 0L
        )
        walletRepository.insertWallet(createdWallet)
        return createdWallet
    }

    private fun deleteTransaction() {
        lifecycleScope.launch {
            transactionRepository.deleteTransaction(transaction)
            Toast.makeText(requireContext(), "Xóa giao dịch thành công", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = false
        }
    }
}
