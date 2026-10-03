package com.free.minimalistlauncher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MinimalistApp : Application() {

    companion object {
        const val CHANNEL_ID = "mindful_launcher_timer_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Mindful Session Reminders"
            val descriptionText = "Notifies when your intentional screen time session is up"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
