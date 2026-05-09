package com.finflow.moneytracker.ui.overview

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Wallet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class AddWalletBottomSheet : BottomSheetDialogFragment() {

    private val walletRepository by lazy {
        (requireActivity().application as MoneyTrackerApplication).container.walletRepository
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etWalletName = view.findViewById<EditText>(R.id.etWalletName)
        val etInitialBalance = view.findViewById<EditText>(R.id.etInitialBalance)
        val btnAddWallet = view.findViewById<Button>(R.id.btnAddWallet)

        etInitialBalance.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    etInitialBalance.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[.,]".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toDouble()
                        // Định dạng dấu chấm ngăn cách hàng nghìn (kiểu VN)
                        val formatted = DecimalFormat("#,###").format(parsed).replace(",", ".")
                        
                        current = formatted
                        etInitialBalance.setText(formatted)
                        etInitialBalance.setSelection(formatted.length)
                    } else {
                        current = ""
                    }

                    etInitialBalance.addTextChangedListener(this)
                }
            }
        })

        btnAddWallet.setOnClickListener {
            val name = etWalletName.text.toString().trim()
            val balanceStr = etInitialBalance.text.toString().trim().replace(".", "")

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên ví", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val balance = balanceStr.toLongOrNull() ?: 0L
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"

            lifecycleScope.launch {
                val newWallet = Wallet(
                    userId = userId,
                    name = name,
                    balance = balance
                )
                walletRepository.insertWallet(newWallet)
                Toast.makeText(requireContext(), "Đã thêm ví mới", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }
}