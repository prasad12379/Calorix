package com.example.run

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

        // Restore reminder if it was active before reboot
        val prefs = context.getSharedPreferences(
            water_tracker.PREFS_NAME, Context.MODE_PRIVATE
        )
        val wasActive    = prefs.getBoolean(water_tracker.KEY_REMINDER_ACTIVE, false)
        val hours        = prefs.getInt(water_tracker.KEY_REMINDER_HOURS, 1)
        val goalL        = prefs.getFloat(water_tracker.KEY_WATER_GOAL, 2.5f)
        val goalMl       = (goalL * 1000).toInt()

        if (!wasActive) return  // user had stopped reminders — don't restart

        val intervalMinutes = maxOf(hours * 60L, 15L)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_INTERVAL_HOURS to hours.toLong(),
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