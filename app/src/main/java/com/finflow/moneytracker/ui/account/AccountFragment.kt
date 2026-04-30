package com.finflow.moneytracker.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.AppDatabase
import com.finflow.moneytracker.data.sync.FirestoreSyncWorker
import com.finflow.moneytracker.ui.host.WelcomeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountFragment : Fragment(R.layout.fragment_account) {

    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 9001

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val layoutUserDetails = view.findViewById<View>(R.id.layout_user_details)
        val btnGoogleAction = view.findViewById<Button>(R.id.btn_google_signin)
        val itemLogout = view.findViewById<View>(R.id.item_logout_action)
        val tvUserName = view.findViewById<TextView>(R.id.tv_display_name) 
        val tvUserEmail = view.findViewById<TextView>(R.id.tv_display_email)
        val tvUserUid = view.findViewById<TextView>(R.id.tv_user_uid)
        
        val itemTheme = view.findViewById<View>(R.id.item_theme_action)
        val tvThemeStatus = view.findViewById<TextView>(R.id.tv_theme_val)

        fun refreshUI() {
            val user = auth.currentUser
            if (user != null) {
                if (user.isAnonymous) {
                    layoutUserDetails?.visibility = View.VISIBLE
                    btnGoogleAction?.visibility = View.VISIBLE
                    btnGoogleAction?.text = "Liên kết tài khoản Google"
                    itemLogout?.visibility = View.VISIBLE
                    tvUserName?.text = "Khách hàng"
                    tvUserEmail?.text = "Chưa đồng bộ (Dữ liệu local)"
                } else {
                    layoutUserDetails?.visibility = View.VISIBLE
                    btnGoogleAction?.visibility = View.GONE
                    itemLogout?.visibility = View.VISIBLE
                    tvUserName?.text = user.displayName ?: "Người dùng"
                    tvUserEmail?.text = user.email
                }
                tvUserUid?.text = "UID: ${user.uid}"
            } else {
                layoutUserDetails?.visibility = View.GONE
                btnGoogleAction?.visibility = View.VISIBLE
                btnGoogleAction?.text = "Đăng nhập với Google"
                itemLogout?.visibility = View.GONE
            }
        }

        refreshUI()

        btnGoogleAction?.setOnClickListener { signInWithGoogle() }

        itemLogout?.setOnClickListener {
            val user = auth.currentUser
            val message = if (user?.isAnonymous == true) {
                "CẢNH BÁO: Dữ liệu của bạn chưa được liên kết. Nếu đăng xuất, TOÀN BỘ dữ liệu sẽ bị xóa. Bạn có chắc chắn không?"
            } else {
                "Bạn có muốn đăng xuất và xóa dữ liệu tạm trên máy không?"
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất & Xóa dữ liệu") { _, _ ->
                    performLogoutAndClearData()
                }
                .show()
        }

        val sharedPref = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        tvThemeStatus?.text = getThemeName(savedMode)
        itemTheme?.setOnClickListener { showThemeSelectionDialog(tvThemeStatus!!) }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)!!
                linkOrSignInWithCredential(account.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun linkOrSignInWithCredential(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.currentUser

        if (user != null && user.isAnonymous) {
            user.linkWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Liên kết thành công! Đang đồng bộ...", Toast.LENGTH_LONG).show()
                        triggerSync() // KÍCH HOẠT SYNC NGAY
                        requireActivity().recreate()
                    } else {
                        Toast.makeText(context, "Lỗi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        triggerSync()
                        requireActivity().recreate()
                    }
                }
        }
    }

    private fun triggerSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(requireContext().applicationContext)
            .enqueue(syncRequest)
    }

    private fun performLogoutAndClearData() {
        lifecycleScope.launch {
            auth.signOut()
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).clearAllTables()
            }
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
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

    private fun getThemeName(mode: Int): String = when (mode) {
        AppCompatDelegate.MODE_NIGHT_NO -> "Sáng"
        AppCompatDelegate.MODE_NIGHT_YES -> "Tối"
        else -> "Hệ thống"
    }
}
