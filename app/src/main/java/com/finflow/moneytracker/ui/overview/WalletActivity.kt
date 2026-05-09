package com.finflow.moneytracker.ui.overview

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.finflow.moneytracker.MoneyTrackerApplication
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.entity.Wallet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class WalletActivity : AppCompatActivity() {

    private val walletRepository by lazy {
        (application as MoneyTrackerApplication).container.walletRepository
    }

    private lateinit var walletAdapter: WalletAdapter
    private lateinit var tvTotalBalance: TextView
    private val formatter = DecimalFormat("#,###")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallet)

        val root = findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.wallet_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rvWallets = findViewById<RecyclerView>(R.id.rvWallets)
        val fabAddWallet = findViewById<FloatingActionButton>(R.id.fab_add_wallet)

        walletAdapter = WalletAdapter(
            onEditClick = { wallet ->
                EditWalletBottomSheet(wallet.id).show(supportFragmentManager, "EditWalletBottomSheet")
            },
            onDeleteClick = { wallet ->
                showDeleteConfirmation(wallet)
            }
        )
        rvWallets.layoutManager = LinearLayoutManager(this)
        rvWallets.adapter = walletAdapter

        btnBack.setOnClickListener {
            finish()
        }

        fabAddWallet.setOnClickListener {
            AddWalletBottomSheet().show(supportFragmentManager, "AddWalletBottomSheet")
        }

        lifecycleScope.launch {
            walletRepository.getWalletsStream().collect { wallets ->
                val activeWallets = wallets.filter { !it.isDeleted }
                walletAdapter.submitList(activeWallets)
                
                val total = activeWallets.sumOf { it.balance }
                tvTotalBalance.text = "${formatter.format(total).replace(",", ".")} ₫"
            }
        }
    }

    private fun showDeleteConfirmation(wallet: Wallet) {
        AlertDialog.Builder(this)
            .setTitle("Xóa ví")
            .setMessage("Bạn có chắc chắn muốn xóa ví '${wallet.name}' không?")
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    walletRepository.deleteWallet(wallet)
                    Toast.makeText(this@WalletActivity, "Đã xóa ví", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}