package com.example.moneytracker.ui.host

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.moneytracker.R
import com.example.moneytracker.ui.add_transaction.AddTransactionBottomSheet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Đọc cài đặt giao diện trước khi gọi super.onCreate và setContentView
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedMode)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_nav)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)

        // Xử lý window insets cho edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // CHỈ apply padding cho top, KHÔNG apply cho bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Apply bottom inset cho bottom navigation
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        viewPager.adapter = MainPaperAdapter(this)
        viewPager.offscreenPageLimit = 4

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_overview -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.menu_budget -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.menu_fake -> {
                    false
                }
                R.id.menu_transaction -> {
                    viewPager.currentItem = 2
                    true
                }
                R.id.menu_account -> {
                    viewPager.currentItem = 3
                    true
                }
                else -> false
            }
        }

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    when (position) {
                        0 -> bottomNav.selectedItemId = R.id.menu_overview
                        1 -> bottomNav.selectedItemId = R.id.menu_budget
                        2 -> bottomNav.selectedItemId = R.id.menu_transaction
                        3 -> bottomNav.selectedItemId = R.id.menu_account
                    }
                }
            }
        )

        fabAdd.setOnClickListener {
            AddTransactionBottomSheet().show(
                supportFragmentManager,
                "AddTransactionBottomSheet"
            )
        }
    }
}
