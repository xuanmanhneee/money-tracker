package com.example.moneytracker.ui.account

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.moneytracker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AccountFragment : Fragment(R.layout.fragment_account) {

    // Trạng thái đăng nhập giả lập (Mặc định là FALSE để người dùng dùng tự do)
    private var isLoggedIn = false 

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Các thành phần thông tin người dùng
        val layoutUserDetails = view.findViewById<View>(R.id.layout_user_details)
        val btnGoogleSignIn = view.findViewById<Button>(R.id.btn_google_signin)
        val itemLogout = view.findViewById<View>(R.id.item_logout_action)
        
        // Các thành phần cài đặt luôn hiện
        val itemTheme = view.findViewById<View>(R.id.item_theme_action)
        val tvThemeStatus = view.findViewById<TextView>(R.id.tv_theme_val)

        // Hàm cập nhật trạng thái hiển thị (Frontend Logic)
        fun refreshUI() {
            if (isLoggedIn) {
                // Đã đăng nhập: Hiện tên/email và nút đăng xuất, ẩn nút đăng nhập Google
                layoutUserDetails?.visibility = View.VISIBLE
                btnGoogleSignIn?.visibility = View.GONE
                itemLogout?.visibility = View.VISIBLE
            } else {
                // Chưa đăng nhập: Hiện nút đăng nhập Google, ẩn tên/email và nút đăng xuất
                layoutUserDetails?.visibility = View.GONE
                btnGoogleSignIn?.visibility = View.VISIBLE
                itemLogout?.visibility = View.GONE
            }
        }

        // Khởi tạo giao diện lần đầu
        refreshUI()

        // Xử lý nút Đăng nhập Google
        btnGoogleSignIn?.setOnClickListener {
            isLoggedIn = true
            refreshUI()
            Toast.makeText(context, "Chào mừng bạn đã đăng nhập!", Toast.LENGTH_SHORT).show()
        }

        // Xử lý nút Đăng xuất
        itemLogout?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có muốn đăng xuất khỏi tài khoản Google không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất") { _, _ ->
                    isLoggedIn = false
                    refreshUI()
                    Toast.makeText(context, "Đã quay về chế độ dùng tự do", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Logic chọn Giao diện (Luôn hoạt động dù đăng nhập hay chưa)
        val sharedPref = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        tvThemeStatus?.text = getThemeName(savedMode)

        itemTheme?.setOnClickListener {
            if (tvThemeStatus != null) {
                showThemeSelectionDialog(tvThemeStatus)
            }
        }
    }

    private fun showThemeSelectionDialog(tvCurrentTheme: TextView) {
        val themes = arrayOf("Sáng", "Tối", "Hệ thống")
        val sharedPref = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        val checkedItem = when (savedMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chọn giao diện")
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val mode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_NO
                    1 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                sharedPref.edit().putInt("theme_mode", mode).apply()
                AppCompatDelegate.setDefaultNightMode(mode)
                tvCurrentTheme.text = themes[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun getThemeName(mode: Int): String {
        return when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "Sáng"
            AppCompatDelegate.MODE_NIGHT_YES -> "Tối"
            else -> "Hệ thống"
        }
    }
}
