package com.finflow.moneytracker.ui.overview

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class EditWalletBottomSheet(private val walletId: Long) : BottomSheetDialogFragment() {

    private val walletRepository by lazy {
        (requireActivity().application as MoneyTrackerApplication).container.walletRepository
    }

    private lateinit var etWalletName: EditText
    private lateinit var etBalance: EditText
    private val formatter = DecimalFormat("#,###")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvTitle)?.setText(R.string.edit_wallet_title)
        etWalletName = view.findViewById(R.id.etWalletName)
        etBalance = view.findViewById(R.id.etInitialBalance)
        val btnSave = view.findViewById<Button>(R.id.btnAddWallet)
        btnSave.setText(R.string.save_changes)

        // Load current data
        lifecycleScope.launch {
            val wallet = walletRepository.getWalletStream(walletId).first()
            wallet?.let {
                etWalletName.setText(it.name)
                val formattedBalance = formatter.format(it.balance).replace(",", ".")
                etBalance.setText(formattedBalance)
            }
        }

        etBalance.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    etBalance.removeTextChangedListener(this)
                    val cleanString = s.toString().replace("[.,]".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toDouble()
                        val formatted = formatter.format(parsed).replace(",", ".")
                        current = formatted
                        etBalance.setText(formatted)
                        etBalance.setSelection(formatted.length)
                    } else {
                        current = ""
                    }
                    etBalance.addTextChangedListener(this)
                }
            }
        })

        btnSave.setOnClickListener {
            val name = etWalletName.text.toString().trim()
            val balanceStr = etBalance.text.toString().trim().replace(".", "")

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_empty_wallet_name, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val balance = balanceStr.toLongOrNull() ?: 0L

            lifecycleScope.launch {
                val currentWallet = walletRepository.getWalletStream(walletId).first()
                currentWallet?.let {
                    val updatedWallet = it.copy(
                        name = name,
                        balance = balance
                    )
                    walletRepository.updateWallet(updatedWallet)
                    Toast.makeText(requireContext(), R.string.wallet_updated, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }
}