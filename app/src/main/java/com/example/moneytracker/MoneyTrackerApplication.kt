package com.example.moneytracker

import android.app.Application
import com.example.moneytracker.di.AppContainer
import com.example.moneytracker.di.DefaultAppContainer

// Import AppContainer của bạn vào đây

class MoneyTrackerApplication : Application() {

    // Khai báo Container chứa các dependency (Repository, Database, Network...)
    lateinit var container: AppContainer

    // Hàm này chạy ĐẦU TIÊN khi app vừa khởi động
    override fun onCreate() {
        super.onCreate()

        // Khởi tạo Container 1 lần duy nhất tại đây
        container = DefaultAppContainer(this)
    }
}