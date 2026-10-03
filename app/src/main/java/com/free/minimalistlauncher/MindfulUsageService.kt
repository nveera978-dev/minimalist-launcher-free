package com.free.minimalistlauncher

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MindfulUsageService : Service() {

    private var countDownTimer: CountDownTimer? = null

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        fun startTimer(context: Context, appName: String, durationMinutes: Int) {
            val intent = Intent(context, MindfulUsageService::class.java).apply {
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            }
            context.startService(intent)
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, MindfulUsageService::class.java)
            context.stopService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "Entertainment App"
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 5) ?: 5
        val totalMillis = durationMinutes * 60 * 1000L

        startForeground(1001, createRunningNotification(appName, durationMinutes))

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Background timer ticking quietly
            }

            override fun onFinish() {
                triggerTimeExpiredAlert(appName)
                stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun createRunningNotification(appName: String, minutes: Int): Notification {
        return NotificationCompat.Builder(this, MinimalistApp.CHANNEL_ID)
            .setContentTitle("Mindful Session Active: $appName")
            .setContentText("Limit set for $minutes minutes. Staying mindful.")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun triggerTimeExpiredAlert(appName: String) {
        val reminderIntent = Intent(this, MindfulExitReminderActivity::class.java).apply {
            putExtra(EXTRA_APP_NAME, appName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val expiredNotification = NotificationCompat.Builder(this, MinimalistApp.CHANNEL_ID)
            .setContentTitle("⏰ Time is Up! ($appName)")
            .setContentText("Your $appName time limit has ended. Ready to exit and utilize your time?")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1002, expiredNotification)

        // Try direct launch
        try {
            startActivity(reminderIntent)
        } catch (_: Exception) {
            // Fullscreen notification fallback handles locked/background cases
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
