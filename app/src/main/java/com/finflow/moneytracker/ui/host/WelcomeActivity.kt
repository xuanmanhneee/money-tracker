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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()

        setupGoogleSignIn()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btn_login).setOnClickListener {
            signInWithGoogle()
        }

        findViewById<Button>(R.id.btn_guest).setOnClickListener {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        UserPrefs.saveUser(this, "Tài khoản khách", null, null)
                        goToLoading(enqueueSyncWorker())
                    } else {
                        // showLoading(false)
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
                    Toast.makeText(this, "Lỗi Google: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    updateUserCache(auth.currentUser)
                    goToLoading(enqueueSyncWorker())
                } else {
                    //showLoading(false)
                    Toast.makeText(this, "Lỗi xác thực: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun enqueueSyncWorker(): java.util.UUID {
        val syncRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>().build()
        WorkManager.getInstance(this).enqueue(syncRequest)
        return syncRequest.id
    }

    private fun updateUserCache(user: com.google.firebase.auth.FirebaseUser?) {
        if (user == null) return
        if (user.isAnonymous) {
            UserPrefs.saveUser(this, "Tài khoản khách", null, null)
        } else {
            UserPrefs.saveUser(this, user.displayName, user.email, user.photoUrl?.toString())
        }
    }

    private fun goToLoading(workId: java.util.UUID) {
        val intent = Intent(this, LoadingActivity::class.java).apply {
            putExtra("WORK_ID", workId.toString())
        }
        startActivity(intent)
        finish()
    }
}