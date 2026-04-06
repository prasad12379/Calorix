package com.example.run

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val KEY_INTERVAL_HOURS = "interval_hours"
        const val KEY_GOAL_ML        = "goal_ml"
        const val CHANNEL_ID         = "reminder_channel"
    }

    override fun doWork(): Result {
        return try {
            val intervalHours = inputData.getLong(KEY_INTERVAL_HOURS, 1L)
            val goalMl        = inputData.getInt(KEY_GOAL_ML, 2500)
            showWaterNotification(goalMl, intervalHours)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showWaterNotification(goalMl: Int, intervalHours: Long) {
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

        val openIntent = Intent(context, Water_take::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalLabel = if (intervalHours == 1L) "1 hour" else "$intervalHours hours"

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