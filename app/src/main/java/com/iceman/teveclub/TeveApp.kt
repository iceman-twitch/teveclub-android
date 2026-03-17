package com.iceman.teveclub

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.iceman.teveclub.worker.DailyCheckWorker

class TeveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)

        val work = PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("daily_check", ExistingPeriodicWorkPolicy.KEEP, work)
    }
}
