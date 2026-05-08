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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Category
import com.finflow.moneytracker.data.local.entity.Transaction
import com.finflow.moneytracker.data.sync.FirestoreSyncWorker
import com.finflow.moneytracker.data.local.entity.Wallet
import com.finflow.moneytracker.data.repository.WalletRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToLong
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

        updateDate()

        ivBack.setOnClickListener { dismiss() }

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

        txtDate.setOnClickListener { showDatePicker() }
        btnSelectCategory.setOnClickListener { showCategorySelectionFragment() }
        btnSelectPaymentMethod.setOnClickListener { showPaymentMethodSelectionFragment() }
        btnAddExpense.setOnClickListener { saveTransaction() }
    }

    private fun updateDate() {
        txtDate.text = sdf.format(calendar.time)
    }

    private fun calculateWorkHours() {
        val amountText = etAmountInput.text.toString().trim()
        if (amountText.isEmpty()) {
            tvWorkHours.text = "≈ 0 giờ làm việc"
            return
        }
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val hours = amount / HOURLY_WAGE
        val roundedHours = round(hours * 100) / 100
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
        val fragment = CategorySelectionFragment()
        fragment.setOnCategorySelectedListener(this)
        fragment.show(parentFragmentManager, "CategorySelection")
    }

    override fun onCategorySelected(category: Category) {
        selectedCategory = category
        tvCategory.text = category.name
        tvCategory.setTextColor(resources.getColor(R.color.text_primary, null))
    }

    private fun showPaymentMethodSelectionFragment() {
        val fragment = PaymentMethodSelectionFragment()
        fragment.setOnPaymentMethodSelectedListener(this)
        fragment.show(parentFragmentManager, "PaymentMethodSelection")
    }

    override fun onPaymentMethodSelected(paymentMethod: PaymentMethod) {
        selectedPaymentMethod = paymentMethod
        tvPaymentMethod.text = paymentMethod.displayName
        tvPaymentMethod.setTextColor(resources.getColor(R.color.text_primary, null))
    }

    private fun saveTransaction() {
        val amountText = etAmountInput.text.toString().trim()
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

        val normalizedAmount = abs(baseAmount)
        
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: DEFAULT_LOCAL_USER
        val appContext = requireContext().applicationContext
        val txRepository = transactionRepository
        val walletRepo = walletRepository

        lifecycleScope.launch {
            val selectedWallet = getOrCreateWalletForPaymentMethod(
                walletRepo,
                selectedPaymentMethod,
                currentUserId
            )

            val transaction = Transaction(
                userId = currentUserId,
                walletId = selectedWallet.id,
                categoryId = selectedCategory!!.id,
                amount = normalizedAmount,
                date = calendar.timeInMillis,
                note = etNotes.text.toString().trim(),
                receiptImagePath = null,
                toWalletId = null
            )

            txRepository.insertTransaction(transaction)

            val message = if (calendar.timeInMillis > System.currentTimeMillis()) {
                "Bro đến từ tương lai à :v"
            } else {
                "Lưu giao dịch thành công"
            }
            if (isAdded) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getOrCreateWalletForPaymentMethod(
        walletRepository: WalletRepository,
        paymentMethod: PaymentMethod,
        currentUserId: String
    ): Wallet {
        val wallets = walletRepository.getWalletsStream().first()
        val targetWalletName = when (paymentMethod) {
            PaymentMethod.CASH -> CASH_WALLET_NAME
            PaymentMethod.BANK -> BANK_WALLET_NAME
        }

        val exactWallet = wallets.firstOrNull { it.name.equals(targetWalletName, ignoreCase = true) }
        if (exactWallet != null) return exactWallet

        val createdWallet = Wallet(
            userId = currentUserId,
            name = targetWalletName,
            balance = 0L
        )
        walletRepository.insertWallet(createdWallet)
        
        // Re-fetch để lấy ID tự tăng từ Room
        return walletRepository.getWalletsStream().first().first { it.name == targetWalletName }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = false
        }
    }
}
