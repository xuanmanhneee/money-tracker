package com.finflow.moneytracker.ui.host

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Đã đăng nhập → vào LoadingActivity để load data + hiện GIF
            startActivity(Intent(this, LoadingActivity::class.java))
        } else {
            // Chưa đăng nhập → vào WelcomeActivity
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
        finish()
    }
}