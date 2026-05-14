package com.finflow.moneytracker.ui.host

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.finflow.moneytracker.R
import com.finflow.moneytracker.ui.add_transaction.AddTransactionBottomSheet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedMode)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_nav)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)

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
                    AddTransactionBottomSheet().show(
                        supportFragmentManager,
                        "AddTransactionBottomSheet"
                    )
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

                    bottomNav.selectedItemId = when (position) {
                        0 -> R.id.menu_overview
                        1 -> R.id.menu_budget
                        2 -> R.id.menu_transaction
                        3 -> R.id.menu_account
                        else -> R.id.menu_overview
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