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

        // ✅ Only act on BOOT_COMPLETED broadcast
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // ✅ Read saved reminder interval and goal from SharedPreferences
        val prefs = context.getSharedPreferences(
            water_tracker.PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val savedMinutes = prefs.getInt(
            water_tracker.KEY_REMINDER_MINUTES, 2
        ).toLong()

        val savedGoalLiters = prefs.getFloat(
            water_tracker.KEY_WATER_GOAL, 2.5f
        )

        // ✅ Convert liters to ml (e.g. 2.5L → 2500 ml)
        val goalMl = (savedGoalLiters * 1000).toInt()

        // ✅ Restart the reminder chain after reboot
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_INTERVAL_MINUTES to savedMinutes,
                    ReminderWorker.KEY_GOAL_ML to goalMl
                )
            )
            .setInitialDelay(savedMinutes, TimeUnit.MINUTES)
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
