package com.finflow.moneytracker.ui.add_transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.finflow.moneytracker.ui.common.CurrencyInputFormatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.round
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddTransactionBottomSheet : BottomSheetDialogFragment(),
    CategorySelectionFragment.OnCategorySelectedListener,
    PaymentMethodSelectionFragment.OnPaymentMethodSelectedListener {

    companion object {
        private const val TYPE_EXPENSE = 0
        private const val DEFAULT_LOCAL_USER = "local_user"
        private const val CASH_WALLET_NAME = "Tiền mặt"
        private const val BANK_WALLET_NAME = "Ngân hàng"
        private const val LEGACY_DEFAULT_WALLET_NAME = "Ví mặc định"
    }

    private lateinit var ivBack: ImageView
    private lateinit var etAmountInput: EditText
    private lateinit var etNotes: EditText
    private lateinit var tvWorkHours: TextView
    private lateinit var txtDate: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnSelectCategory: LinearLayout
    private lateinit var tvCategory: TextView
    private lateinit var btnSelectPaymentMethod: LinearLayout
    private lateinit var tvPaymentMethod: TextView
    private lateinit var btnAddExpense: Button

    private var selectedCategory: Category? = null
    private var selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH

    private val calendar: Calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi"))

    // Lương mỗi giờ (tạm thời hardcode, sau sẽ lấy từ user settings)
    private val HOURLY_WAGE = 26000.0

    private val transactionRepository by lazy {
        (requireActivity().application as MoneyTrackerApplication).container.transactionRepository
    }

    private val walletRepository by lazy {
        (requireActivity().application as MoneyTrackerApplication).container.walletRepository
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.bottom_sheet_add_transaction,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivBack = view.findViewById(R.id.ivBack)
        etAmountInput = view.findViewById(R.id.etAmountInput)
        etNotes = view.findViewById(R.id.etNotes)
        tvWorkHours = view.findViewById(R.id.tvWorkHours)
        txtDate = view.findViewById(R.id.txtDate)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        btnSelectCategory = view.findViewById(R.id.btnSelectCategory)
        tvCategory = view.findViewById(R.id.tvCategory)
        btnSelectPaymentMethod = view.findViewById(R.id.btnSelectPaymentMethod)
        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod)
        btnAddExpense = view.findViewById(R.id.btnAddExpense)
        CurrencyInputFormatter.attach(etAmountInput)

        updateDate()

        // Nút quay lại
        ivBack.setOnClickListener {
            dismiss()
        }

        // Listener để tính số giờ làm việc khi nhập số tiền
        etAmountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateWorkHours()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnPrev.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            updateDate()
        }

        btnNext.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            updateDate()
        }

        txtDate.setOnClickListener {
            showDatePicker()
        }

        // Click listener để mở CategorySelectionFragment
        btnSelectCategory.setOnClickListener {
            showCategorySelectionFragment()
        }

        // Click listener để mở PaymentMethodSelectionFragment
        btnSelectPaymentMethod.setOnClickListener {
            showPaymentMethodSelectionFragment()
        }

        // Click listener để lưu giao dịch
        btnAddExpense.setOnClickListener {
            saveTransaction()
        }
    }

    private fun updateDate() {
        txtDate.text = sdf.format(calendar.time)
    }

    private fun calculateWorkHours() {
        val amount = CurrencyInputFormatter.parseAmountOrNull(etAmountInput)?.toDouble()
        if (amount == null || amount <= 0.0) {
            tvWorkHours.text = "≈ 0 giờ làm việc"
            return
        }

        val hours = amount / HOURLY_WAGE
        val roundedHours = round(hours * 100) / 100  // Làm tròn 2 chữ số thập phân

        tvWorkHours.text = "≈ $roundedHours giờ làm việc"
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
        val fragment = CategorySelectionFragment.newInstance(selectedCategory?.id)
        fragment.setOnCategorySelectedListener(this)
        fragment.show(parentFragmentManager, "CategorySelection")
    }

    override fun onCategorySelected(category: Category) {
        selectedCategory = category
        tvCategory.text = category.name
        tvCategory.setTextColor(resources.getColor(R.color.text_primary, null))
    }

    private fun showPaymentMethodSelectionFragment() {
        val fragment = PaymentMethodSelectionFragment.newInstance(selectedPaymentMethod)
        fragment.setOnPaymentMethodSelectedListener(this)
        fragment.show(parentFragmentManager, "PaymentMethodSelection")
    }

    override fun onPaymentMethodSelected(paymentMethod: PaymentMethod) {
        selectedPaymentMethod = paymentMethod
        tvPaymentMethod.text = paymentMethod.displayName
        tvPaymentMethod.setTextColor(resources.getColor(R.color.text_primary, null))
    }

    private fun saveTransaction() {
        // Kiểm tra validation
        if (!CurrencyInputFormatter.hasAnyDigit(etAmountInput)) {
            Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn nhóm", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = CurrencyInputFormatter.parseAmountOrNull(etAmountInput)
        if (amount == null || amount <= 0L) {
            Toast.makeText(requireContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show()
            return
        }

        val baseAmount = amount
        val isExpense = selectedCategory?.type == TYPE_EXPENSE
        val signedAmount = if (isExpense) -abs(baseAmount) else abs(baseAmount)

        lifecycleScope.launch {
            val selectedWallet = getOrCreateWalletForPaymentMethod(selectedPaymentMethod)

            // Tạo transaction với ví đang có trong hệ thống
            val transaction = Transaction(
                walletId = selectedWallet.id,
                categoryId = selectedCategory!!.id,
                amount = signedAmount,
                date = calendar.timeInMillis,
                note = etNotes.text.toString().trim(),
                receiptImagePath = null,
                toWalletId = null
            )

            transactionRepository.insertTransaction(transaction)

            // Thông báo thành công
            val message = "Lưu giao dịch thành công: ${selectedCategory!!.name}"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            // Đóng bottom sheet
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

    override fun getTheme(): Int = R.style.ThemeOverlay_MoneyTracker_BottomSheet
}
