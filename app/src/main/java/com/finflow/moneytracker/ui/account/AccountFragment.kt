package com.finflow.moneytracker.ui.account

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.finflow.moneytracker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment(R.layout.fragment_account) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Các thành phần giao diện
        val layoutUserDetails = view.findViewById<View>(R.id.layout_user_details)
        val btnGoogleSignIn = view.findViewById<Button>(R.id.btn_google_signin)
        val itemLogout = view.findViewById<View>(R.id.item_logout_action)
        val tvUserName = view.findViewById<TextView>(R.id.tv_display_name) 
        val tvUserEmail = view.findViewById<TextView>(R.id.tv_display_email)
        
        val itemTheme = view.findViewById<View>(R.id.item_theme_action)
        val tvThemeStatus = view.findViewById<TextView>(R.id.tv_theme_val)

        // Hàm cập nhật UI dựa trên Firebase User thật
        fun refreshUI() {
            val user = auth.currentUser
            if (user != null) {
                layoutUserDetails?.visibility = View.VISIBLE
                btnGoogleSignIn?.visibility = View.GONE
                itemLogout?.visibility = View.VISIBLE
                
                // Hiển thị thông tin user từ Firebase
                tvUserName?.text = user.displayName ?: "Người dùng mới"
                tvUserEmail?.text = user.email ?: "Đã đăng nhập ẩn danh"
            } else {
                layoutUserDetails?.visibility = View.GONE
                btnGoogleSignIn?.visibility = View.VISIBLE
                itemLogout?.visibility = View.GONE
            }
        }

        refreshUI()

        // Xử lý Đăng nhập
        btnGoogleSignIn?.setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        refreshUI()
                        Toast.makeText(context, "Chào mừng bạn!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Lỗi đăng nhập: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Xử lý Đăng xuất
        itemLogout?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Dữ liệu của bạn sẽ được lưu an toàn trên Cloud. Bạn có muốn đăng xuất không?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất") { _, _ ->
                    auth.signOut()
                    refreshUI()
                    Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Logic Theme
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