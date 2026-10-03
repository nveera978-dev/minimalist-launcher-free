package com.free.minimalistlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MindfulExitReminderActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra(MindfulUsageService.EXTRA_APP_NAME) ?: "Entertainment App"

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(48, 120, 48, 60)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Warning Icon & Badge
        val badge = TextView(this).apply {
            text = "⏰ BRAIN RECOVERY REMINDER"
            setTextColor(Color.parseColor("#F87171"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        rootLayout.addView(badge)

        // Title
        val titleView = TextView(this).apply {
            text = "Time is Up!"
            setTextColor(Color.WHITE)
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(titleView)

        // Description
        val descView = TextView(this).apply {
            text = "Your planned session for $appName has concluded.\n\nNotice how easy it is to keep scrolling. Use your conscious brain right now: close the app and invest your time in real life."
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 15f
            gravity = Gravity.CENTER
            setLineSpacing(10f, 1.2f)
            setPadding(0, 0, 0, 80)
        }
        rootLayout.addView(descView)

        // Exit Button (Primary)
        val exitButton = Button(this).apply {
            text = "Exit Now & Utilize My Time"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#059669"))
            textSize = 16f
            setPadding(30, 30, 30, 30)
            setOnClickListener {
                // Return to home launcher
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        }
        rootLayout.addView(exitButton)

        // Extend 2 minutes button (Secondary)
        val extendButton = Button(this).apply {
            text = "Extend 2 Minutes Only"
            setTextColor(Color.parseColor("#94A3B8"))
            setBackgroundColor(Color.parseColor("#1E293B"))
            textSize = 12f
            setPadding(30, 20, 30, 20)
            setOnClickListener {
                MindfulUsageService.startTimer(this@MindfulExitReminderActivity, appName, 2)
                finish()
            }
        }
        rootLayout.addView(extendButton)

        setContentView(rootLayout)
    }
}
