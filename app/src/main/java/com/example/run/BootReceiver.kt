package com.example.run

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        // Read saved preferences
        val prefs = context.getSharedPreferences(water_tracker.PREFS_NAME, Context.MODE_PRIVATE)
        val reminderMinutes = prefs.getInt(water_tracker.KEY_REMINDER_MINUTES, 0).toLong()
        val goalMl = (prefs.getFloat(water_tracker.KEY_WATER_GOAL, 2.5f) * 1000).toInt()

        // Only restart if user had set a reminder before
        if (reminderMinutes <= 0) return

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_INTERVAL_MINUTES to reminderMinutes,
                    ReminderWorker.KEY_GOAL_ML to goalMl
                )
            )
            .setInitialDelay(reminderMinutes, TimeUnit.MINUTES)
            .addTag(water_tracker.WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                water_tracker.WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }
}