package com.example.run

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
        const val KEY_GOAL_ML          = "goal_ml"
        const val CHANNEL_ID           = "reminder_channel"
    }

    override fun doWork(): Result {
        return try {
            val intervalMinutes = inputData.getLong(KEY_INTERVAL_MINUTES, 1L)
            val goalMl          = inputData.getInt(KEY_GOAL_ML, 2500)

            showWaterNotification(goalMl, intervalMinutes)
            scheduleNext(intervalMinutes, goalMl)

            Result.success()
        } catch (e: Exception) {
            // ✅ Never let an exception kill the chain — always reschedule even on error
            val intervalMinutes = inputData.getLong(KEY_INTERVAL_MINUTES, 1L)
            val goalMl          = inputData.getInt(KEY_GOAL_ML, 2500)
            scheduleNext(intervalMinutes, goalMl)
            Result.success()
        }
    }

    private fun scheduleNext(intervalMinutes: Long, goalMl: Int) {
        val nextRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                workDataOf(
                    KEY_INTERVAL_MINUTES to intervalMinutes,
                    KEY_GOAL_ML          to goalMl
                )
            )
            .setInitialDelay(intervalMinutes, TimeUnit.MINUTES)
            .addTag(water_tracker.WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                water_tracker.WORK_TAG + "_next",
                ExistingWorkPolicy.KEEP,
                nextRequest
            )
    }

    private fun showWaterNotification(goalMl: Int, intervalMinutes: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to drink water regularly"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
            }
            manager.createNotificationChannel(channel)
        }

        // ✅ Direct explicit intent to Water_take
        // FLAG_ACTIVITY_NEW_TASK   — required from non-activity context (worker/service)
        // FLAG_ACTIVITY_CLEAR_TOP  — if already in stack, bring to front instead of new instance
        val openIntent = Intent(context, Water_take::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalLabel = if (intervalMinutes == 1L) "1 min" else "$intervalMinutes mins"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("💧 Time to Drink Water!")
            .setContentText("Daily goal: ${goalMl}ml · Every $intervalLabel")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your daily goal is ${goalMl}ml.\nSmall sips throughout the day keep you energized! 💪")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()

        manager.notify(1001, notification)
    }
}