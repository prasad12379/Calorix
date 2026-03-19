package com.example.run

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
        const val KEY_GOAL_ML          = "goal_ml"           // ✅ goal passed from water_tracker
        private val notificationIdCounter = AtomicInteger(2000)
        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val FOREGROUND_CHANNEL_ID = "reminder_foreground_channel"
        private const val REMINDER_CHANNEL_ID   = "reminder_channel"
    }

    override suspend fun doWork(): Result {

        val intervalMinutes = inputData.getLong(KEY_INTERVAL_MINUTES, 2L)
        val goalMl          = inputData.getInt(KEY_GOAL_ML, 2500)   // ✅ read goal (default 2500ml)

        // ✅ Run as foreground to survive app close
        setForeground(createForegroundInfo())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.failure()
            }
        }

        // ✅ Intent that opens Water_take and passes the goal
        val openAppIntent = Intent(applicationContext, Water_take::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("GOAL_ML", goalMl)   // ✅ Water_take reads this
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE  // ✅ required on Android 12+
        )

        // ✅ Notification with tap action → opens Water_take
        val goalLiters = String.format("%.1f", goalMl / 1000f)
        val reminderNotification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Water Reminder 💧")
            .setContentText("Time to drink water! Your goal is ${goalLiters}L 🌊")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)   // ✅ tap opens Water_take
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationIdCounter.getAndIncrement(), reminderNotification)

        // ✅ Reschedule next reminder — carry goal forward
        val nextRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(
                workDataOf(
                    KEY_INTERVAL_MINUTES to intervalMinutes,
                    KEY_GOAL_ML          to goalMl           // ✅ keep passing goal
                )
            )
            .setInitialDelay(intervalMinutes, TimeUnit.MINUTES)
            .addTag(water_tracker.WORK_TAG)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(nextRequest)

        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        createForegroundChannel()

        val notification = NotificationCompat.Builder(applicationContext, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Water Reminder Active")
            .setContentText("Reminders are running in background...")
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun createForegroundChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Water Reminder Service",
                NotificationManager.IMPORTANCE_LOW
            )
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}