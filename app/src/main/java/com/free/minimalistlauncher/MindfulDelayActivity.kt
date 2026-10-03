package com.free.minimalistlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MindfulDelayActivity : Activity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_LABEL = "extra_target_label"
    }

    private var countDownTimer: CountDownTimer? = null
    private var selectedDurationMinutes = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        val targetLabel = intent.getStringExtra(EXTRA_TARGET_LABEL) ?: "App"
        val prefs = LauncherPreferences(this)
        val delaySeconds = prefs.delaySeconds

        // Main layout container (OLED pure black)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(48, 80, 48, 60)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Subtitle badge
        val badge = TextView(this).apply {
            text = "MINDFUL BREATHING PAUSE"
            setTextColor(Color.parseColor("#34D399"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(20, 10, 20, 10)
        }
        rootLayout.addView(badge)

        // Title
        val titleView = TextView(this).apply {
            text = "Take a breath"
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 10)
        }
        rootLayout.addView(titleView)

        // Description
        val descView = TextView(this).apply {
            text = "Opening $targetLabel. Are you opening this unconsciously, or do you have a specific purpose?"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }
        rootLayout.addView(descView)

        // Countdown visual number
        val countdownView = TextView(this).apply {
            text = delaySeconds.toString()
            setTextColor(Color.parseColor("#34D399"))
            textSize = 64f
            typeface = Typeface.DEFAULT
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }
        rootLayout.addView(countdownView)

        // Session length selector label
        val limitLabel = TextView(this).apply {
            text = "Choose intentional time limit:"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(limitLabel)

        // Buttons for session length (5m, 10m, 15m)
        val timeSelectorLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }

        val btn5 = Button(this).apply {
            text = "5 Min"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#047857"))
            setOnClickListener { selectedDurationMinutes = 5 }
        }
        val btn10 = Button(this).apply {
            text = "10 Min"
            setTextColor(Color.parseColor("#94A3B8"))
            setBackgroundColor(Color.parseColor("#1E293B"))
            setOnClickListener { selectedDurationMinutes = 10 }
        }
        val btn15 = Button(this).apply {
            text = "15 Min"
            setTextColor(Color.parseColor("#94A3B8"))
            setBackgroundColor(Color.parseColor("#1E293B"))
            setOnClickListener { selectedDurationMinutes = 15 }
        }
        timeSelectorLayout.addView(btn5)
        timeSelectorLayout.addView(btn10)
        timeSelectorLayout.addView(btn15)
        rootLayout.addView(timeSelectorLayout)

        // "I don't need this (Exit to Home)" button
        val exitButton = Button(this).apply {
            text = "I don't need this (Exit to Home)"
            setTextColor(Color.parseColor("#34D399"))
            setBackgroundColor(Color.parseColor("#1E293B"))
            textSize = 14f
            setPadding(30, 25, 30, 25)
            setOnClickListener {
                countDownTimer?.cancel()
                prefs.recordPreventedOpen()
                Toast.makeText(this@MindfulDelayActivity, "🎉 Mindful Win! Time saved.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        rootLayout.addView(exitButton)

        // "Continue" button (disabled during countdown)
        val continueButton = Button(this).apply {
            text = "Wait ${delaySeconds}s..."
            setTextColor(Color.parseColor("#64748B"))
            setBackgroundColor(Color.parseColor("#0F172A"))
            textSize = 13f
            isEnabled = false
            setPadding(30, 25, 30, 25)
            setOnClickListener {
                countDownTimer?.cancel()
                launchTargetApp(targetPackage, targetLabel, selectedDurationMinutes)
            }
        }
        rootLayout.addView(continueButton)

        setContentView(rootLayout)

        // Start 5-second countdown
        countDownTimer = object : CountDownTimer((delaySeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = (millisUntilFinished / 1000) + 1
                countdownView.text = secs.toString()
                continueButton.text = "Wait ${secs}s..."
            }

            override fun onFinish() {
                countdownView.text = "✓"
                continueButton.isEnabled = true
                continueButton.text = "Continue to $targetLabel ($selectedDurationMinutes min limit)"
                continueButton.setTextColor(Color.WHITE)
                continueButton.setBackgroundColor(Color.parseColor("#059669"))
            }
        }.start()
    }

    private fun launchTargetApp(packageName: String, appName: String, durationMinutes: Int) {
        MindfulUsageService.startTimer(this, appName, durationMinutes)
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
        finish()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
