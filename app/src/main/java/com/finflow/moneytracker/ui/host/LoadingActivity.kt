package com.finflow.moneytracker.ui.host

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.finflow.moneytracker.R
import com.finflow.moneytracker.data.sync.FirestoreSyncWorker
import com.finflow.moneytracker.databinding.ActivityLoadingBinding

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Glide.with(this)
            .asGif()
            .load(R.drawable.loading)
            .into(binding.ivLoadingGif)

        val workIdStr = intent.getStringExtra("WORK_ID")

        if (workIdStr != null) {
            // Lần đầu đăng nhập — đợi sync xong mới vào
            observeWork(java.util.UUID.fromString(workIdStr)) { goToMain() }
        } else {
            // Mở app lại — sync ngầm, vào sau 1.5s
            enqueueSyncInBackground()
            binding.root.postDelayed({ goToMain() }, 1500)
        }
    }

    private fun triggerInitialSync(onComplete: () -> Unit) {
        val syncRequest = OneTimeWorkRequestBuilder<com.finflow.moneytracker.data.sync.FirestoreSyncWorker>().build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueue(syncRequest)
        workManager.getWorkInfoByIdLiveData(syncRequest.id).observe(this, Observer { workInfo ->
            if (workInfo != null && workInfo.state.isFinished) {
                onComplete()
            }
        })
    }

    private fun observeWork(workId: java.util.UUID, onComplete: () -> Unit) {
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(workId)
            .observe(this) { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    onComplete()
                }
            }
    }

    private fun enqueueSyncInBackground() {
        val syncRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>().build()
        WorkManager.getInstance(this).enqueue(syncRequest)
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}