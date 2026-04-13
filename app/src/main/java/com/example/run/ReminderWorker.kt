package com.example.run

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {   // ← CoroutineWorker survives better than Worker

    companion object {
        const val KEY_INTERVAL_HOURS = "interval_hours"
        const val KEY_GOAL_ML        = "goal_ml"
        const val CHANNEL_ID         = "reminder_channel"
        const val CHANNEL_ID_FG      = "reminder_fg_channel"  // foreground service channel
        const val NOTIF_ID           = 1001
        const val NOTIF_ID_FG        = 1002
    }

    override suspend fun doWork(): Result {
        return try {
            // ── Run as foreground service so MIUI can't kill it ──────────
            setForeground(createForegroundInfo())

            val intervalHours = inputData.getLong(KEY_INTERVAL_HOURS, 1L)
            val goalMl        = inputData.getInt(KEY_GOAL_ML, 2500)

            showWaterNotification(goalMl, intervalHours)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    // ── Foreground info (keeps worker alive on MIUI/Xiaomi) ──────────────
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_FG,
                "Water Reminder Service",
                NotificationManager.IMPORTANCE_LOW   // low so it's silent but keeps worker alive
            ).apply {
                description = "Keeps water reminders running in background"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FG)

            .setContentTitle("💧 Water reminder active")
            .setContentText("Tap to open water tracker")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)   // can't be dismissed — keeps service alive
            .setSilent(true)
            .build()

        return ForegroundInfo(NOTIF_ID_FG, notification)
    }

    // ── Actual water reminder notification ───────────────────────────────
    private fun showWaterNotification(goalMl: Int, intervalHours: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Water Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description        = "Reminds you to drink water regularly"
                enableVibration(true)
                vibrationPattern   = longArrayOf(0, 300, 100, 300)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, water_tracker::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalLabel = if (intervalHours == 1L) "1 hour" else "$intervalHours hours"
        val goalL         = goalMl / 1000.0

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)

            .setContentTitle("💧 Time to Drink Water!")
            .setContentText("Daily goal: ${String.format("%.1f", goalL)}L · Every $intervalLabel")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Your daily goal is ${String.format("%.1f", goalL)}L.\n" +
                                "Small sips throughout the day keep you energized! 💪\n" +
                                "Reminder set every $intervalLabel."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        manager.notify(NOTIF_ID, notification)
    }
}