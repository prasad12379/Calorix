package com.example.run

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        val prefs         = context.getSharedPreferences(water_tracker.PREFS_NAME, Context.MODE_PRIVATE)
        val isActive      = prefs.getBoolean(water_tracker.KEY_REMINDER_ACTIVE, false)
        val intervalHours = prefs.getInt(water_tracker.KEY_REMINDER_HOURS, 1).toLong()
        val goalMl        = (prefs.getFloat(water_tracker.KEY_WATER_GOAL, 2.5f) * 1000).toInt()

        if (!isActive || intervalHours <= 0) return

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_INTERVAL_HOURS to intervalHours,
                    ReminderWorker.KEY_GOAL_ML        to goalMl
                )
            )
            .addTag(water_tracker.WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                water_tracker.WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }
}