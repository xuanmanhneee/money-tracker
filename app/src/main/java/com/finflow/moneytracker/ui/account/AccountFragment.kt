package com.finflow.moneytracker.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.local.AppDatabase
import com.finflow.moneytracker.data.sync.FirestoreSyncWorker
import com.finflow.moneytracker.ui.host.WelcomeActivity
import com.finflow.moneytracker.utils.UserPrefs // Import cache
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
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

        // 1. Ánh xạ View
        val layoutProfile = view.findViewById<View>(R.id.layout_profile)
        val ivAvatar = view.findViewById<ImageView>(R.id.iv_avatar_main)
        val tvUserName = view.findViewById<TextView>(R.id.tv_display_name)
        val btnGoogleAction = view.findViewById<MaterialButton>(R.id.btn_google_signin)
        val layoutAccountInfo = view.findViewById<View>(R.id.layout_account_info)
        val tvUserEmail = view.findViewById<TextView>(R.id.tv_display_email)
        val tvUserUid = view.findViewById<TextView>(R.id.tv_user_uid)
        val itemLogout = view.findViewById<View>(R.id.item_logout_action)

        // 2. Hàm cập nhật giao diện (Ưu tiên Cache -> Cập nhật Firebase)
        fun refreshUI() {
            val user = auth.currentUser
            val context = requireContext()

            // --- BƯỚC 1: ĐỌC TỪ CACHE (HIỂN THỊ TỨC THÌ) ---
            val cachedName = UserPrefs.getName(context)
            val cachedPhoto = UserPrefs.getPhoto(context)
            val cachedEmail = UserPrefs.getEmail(context)

            if (!cachedName.isNullOrBlank()) tvUserName?.text = cachedName
            if (!cachedEmail.isNullOrBlank()) tvUserEmail?.text = cachedEmail
            if (!cachedPhoto.isNullOrBlank()) {
                Glide.with(this).load(cachedPhoto).circleCrop().placeholder(R.drawable.user).into(ivAvatar!!)
            }

            // --- BƯỚC 2: LOGIC FIREBASE (CẬP NHẬT THỰC TẾ) ---
            layoutProfile?.visibility = View.VISIBLE

            if (user == null) {
                tvUserName?.text = "Chưa đăng nhập"
                ivAvatar?.setImageResource(R.drawable.user)
                btnGoogleAction?.visibility = View.VISIBLE
                layoutAccountInfo?.visibility = View.GONE
                itemLogout?.visibility = View.GONE
            } else if (user.isAnonymous) {
                tvUserName?.text = "Tài khoản khách"
                ivAvatar?.setImageResource(R.drawable.user)
                btnGoogleAction?.visibility = View.VISIBLE
                btnGoogleAction?.text = "Liên kết tài khoản"
                layoutAccountInfo?.visibility = View.GONE
                itemLogout?.visibility = View.VISIBLE
            } else {
                btnGoogleAction?.visibility = View.GONE
                layoutAccountInfo?.visibility = View.VISIBLE
                itemLogout?.visibility = View.VISIBLE

                // Tìm thông tin "tươi" nhất từ Firebase
                var finalName: String? = user.displayName
                var finalPhotoUrl = user.photoUrl

                for (profile in user.providerData) {
                    if (finalName == null) finalName = profile.displayName
                    if (finalPhotoUrl == null) finalPhotoUrl = profile.photoUrl
                }

                // Cập nhật UI nếu có thay đổi so với Cache
                val nameToShow = if (finalName.isNullOrBlank()) "Người dùng" else finalName
                tvUserName?.text = nameToShow
                tvUserEmail?.text = user.email
                tvUserUid?.text = "UID: ${user.uid}"

                if (finalPhotoUrl != null) {
                    Glide.with(this).load(finalPhotoUrl).circleCrop().into(ivAvatar!!)
                }

                // ĐỒNG BỘ NGƯỢC LẠI CACHE (Nếu thông tin trên Cloud có thay đổi)
                UserPrefs.saveUser(context, nameToShow, user.email, finalPhotoUrl?.toString())
            }
        }

        refreshUI()

        // 3. Xử lý Click Events
        btnGoogleAction?.setOnClickListener { signInWithGoogle() }

        itemLogout?.setOnClickListener {
            val user = auth.currentUser
            val message = if (user?.isAnonymous == true) {
                "CẢNH BÁO: Dữ liệu chưa liên kết sẽ bị mất vĩnh viễn. Bạn có chắc muốn đăng xuất?"
            } else {
                "Bạn có muốn đăng xuất và xóa dữ liệu tạm trên máy không?"
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đăng xuất & Xóa") { _, _ ->
                    performLogoutAndClearData()
                }
                .show()
        }
    }

    // --- LOGIC ĐĂNG NHẬP GOOGLE ---
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
            user.linkWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Liên kết thành công!", Toast.LENGTH_SHORT).show()
                    triggerSync()
                    requireActivity().recreate()
                } else {
                    Toast.makeText(context, "Lỗi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    triggerSync()
                    requireActivity().recreate()
                }
            }
        }
    }

    private fun triggerSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(requireContext().applicationContext).enqueue(syncRequest)
    }

    private fun performLogoutAndClearData() {
        lifecycleScope.launch {
            auth.signOut()

            // XÓA CACHE TRƯỚC KHI RỜI ĐI
            UserPrefs.clear(requireContext())

            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).clearAllTables()
            }

            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}