package com.finflow.moneytracker.utils

import android.content.Context
import androidx.core.content.edit // Quan trọng: Phải import cái này

object UserPrefs {
    private const val PREF_NAME = "user_cache"

    fun saveUser(context: Context, name: String?, email: String?, photoUrl: String?) {
        // Dùng KTX: Không cần biến trung gian 'pref' hay gọi 'apply()' thủ công
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString("name", name)
            putString("email", email)
            putString("photo", photoUrl)
        }
    }

    fun getName(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("name", null)

    fun getEmail(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("email", null)

    fun getPhoto(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString("photo", null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            clear()
        }
    }
}