package com.finflow.moneytracker.ui.host

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.finflow.moneytracker.R
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance()

        // Kiểm tra nếu đã đăng nhập rồi thì vào luôn MainActivity
        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnGuest = findViewById<Button>(R.id.btn_guest)

        btnLogin.setOnClickListener {
            // Sau này sẽ mở màn hình đăng nhập chi tiết (Google/Email)
            Toast.makeText(this, "Tính năng đăng nhập đang được cập nhật", Toast.LENGTH_SHORT).show()
        }

        btnGuest.setOnClickListener {
            // Đăng nhập ẩn danh để bắt đầu dùng
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        startMainActivity()
                    } else {
                        Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}