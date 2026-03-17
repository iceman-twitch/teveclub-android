package com.iceman.teveclub.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.iceman.teveclub.NotificationHelper

class DailyCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // Prototype behavior: send a notification that daily check ran
        try {
            NotificationHelper.sendNotification(applicationContext, 1001, "Napi ellenőrzés", "A napi ellenőrzés lefutott (prototípus).")
        } catch (e: Exception) {
            return Result.failure()
        }
        return Result.success()
    }
}
