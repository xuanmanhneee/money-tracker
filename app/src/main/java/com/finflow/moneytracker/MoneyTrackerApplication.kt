package com.finflow.moneytracker

import android.app.Application
import com.finflow.moneytracker.di.AppContainer
import com.finflow.moneytracker.di.DefaultAppContainer

class MoneyTrackerApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}