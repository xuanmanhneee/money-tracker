package com.finflow.moneytracker.ui.host

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.sync.FirestoreSyncWorker
import com.finflow.moneytracker.utils.UserPrefs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var progressBar: ProgressBar // Nên có để báo hiệu đang sync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()
        progressBar = findViewById(R.id.progress_loading) // Đảm bảo ID này có trong layout

        // 1. Kiểm tra trạng thái đăng nhập tự động
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUserCache(currentUser)

            // Hiện loading và đợi đồng bộ xong mới cho vào app
            showLoading(true)
            triggerInitialSync {
                startMainActivity()
            }
            return
        }

        setupGoogleSignIn()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnGuest = findViewById<Button>(R.id.btn_guest)

        btnLogin.setOnClickListener {
            signInWithGoogle()
        }

        btnGuest.setOnClickListener {
            showLoading(true)
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        UserPrefs.saveUser(this, "Tài khoản khách", null, null)
                        // Khách thường không có dữ liệu cloud cũ, nhưng vẫn sync để chắc chắn
                        triggerInitialSync { startMainActivity() }
                    } else {
                        showLoading(false)
                        Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    showLoading(false)
                    Toast.makeText(this, "Lỗi Google: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signInWithGoogle() {
        showLoading(true)
        googleSignInClient.signOut().addOnCompleteListener {
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUserCache(user)

                    Toast.makeText(this, "Đang đồng bộ dữ liệu...", Toast.LENGTH_SHORT).show()
                    triggerInitialSync {
                        startMainActivity()
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Lỗi xác thực: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Đồng bộ dữ liệu Firestore và đợi kết quả
     */
    private fun triggerInitialSync(onComplete: () -> Unit) {
        val syncRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>().build()
        val workManager = WorkManager.getInstance(this)

        workManager.enqueue(syncRequest)

        // Quan sát trạng thái Worker thông qua LiveData
        workManager.getWorkInfoByIdLiveData(syncRequest.id).observe(this) { workInfo ->
            if (workInfo != null && workInfo.state.isFinished) {
                onComplete()
            }
        }
    }

    /**
     * Cập nhật thông tin vào SharedPreferences để các màn hình sau dùng ngay
     */
    private fun updateUserCache(user: com.google.firebase.auth.FirebaseUser?) {
        if (user == null) return
        if (user.isAnonymous) {
            UserPrefs.saveUser(this, "Tài khoản khách", null, null)
        } else {
            UserPrefs.saveUser(
                this,
                user.displayName,
                user.email,
                user.photoUrl?.toString()
            )
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Vô hiệu hóa nút để tránh bấm nhiều lần
        findViewById<Button>(R.id.btn_login).isEnabled = !isLoading
        findViewById<Button>(R.id.btn_guest).isEnabled = !isLoading
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}