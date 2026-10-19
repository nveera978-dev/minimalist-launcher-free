package com.free.minimalistlauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var prefs: LauncherPreferences
    private lateinit var usageStatsHelper: UsageStatsHelper
    private lateinit var defaultBanner: Button
    private lateinit var screenTimePill: Button
    private lateinit var clockView: TextView
    private lateinit var amPmView: TextView
    private lateinit var dateView: TextView
    private lateinit var quoteView: TextView
    private lateinit var favoritesContainer: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var allInstalledApps: List<AppItem> = emptyList()

    private val positiveAffirmations = listOf(
        "You are doing great today. Stay present and keep winning.",
        "Be proud of how far you've come. Every step matters.",
        "Your time is your life. You are utilizing it with wisdom.",
        "Believe in yourself. You have the power to create great things.",
        "You are in control of your day. Keep shining and growing.",
        "Appreciate this moment. You are focused, capable, and strong."
    )

    private val timeUpdater = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LauncherPreferences(this)
        usageStatsHelper = UsageStatsHelper(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(50, 70, 50, 40)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Default Launcher Setup Banner (Visible only if not yet set as default)
        defaultBanner = Button(this).apply {
            text = "⚡ Tap to Set as Default Home Screen"
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#34D399"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(24, 24, 24, 24)
            visibility = View.GONE
            setOnClickListener { promptSetDefaultLauncher() }
        }
        root.addView(defaultBanner)

        // 12-Hour Clock with AM/PM on the right
        val clockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        clockView = TextView(this).apply {
            textSize = 58f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        }
        amPmView = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#34D399"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(14, 0, 0, 14)
        }
        clockRow.addView(clockView)
        clockRow.addView(amPmView)
        root.addView(clockRow)

        dateView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 4, 0, 16)
        }
        root.addView(dateView)

        // Positive Appreciation Words
        quoteView = TextView(this).apply {
            text = getString(R.string.default_quote)
            textSize = 13f
            setTextColor(Color.parseColor("#34D399"))
            setPadding(0, 0, 0, 20)
        }
        root.addView(quoteView)

        // Live Screen Time & Digital Wellbeing Pill
        screenTimePill = Button(this).apply {
            text = "⏱ Loading screen time..."
            textSize = 12f
            setTextColor(Color.parseColor("#E2E8F0"))
            setBackgroundColor(Color.parseColor("#0F172A"))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(24, 14, 24, 14)
            setOnClickListener {
                if (usageStatsHelper.hasUsagePermission()) {
                    showDigitalWellbeingDashboard()
                } else {
                    usageStatsHelper.openUsageAccessSettings()
                }
            }
        }
        root.addView(screenTimePill)

        // Favorites Container
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        favoritesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(favoritesContainer)
        root.addView(scrollView)

        // Bottom Navigation Bar
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 20, 0, 0)
        }

        val allAppsBtn = Button(this).apply {
            text = getString(R.string.all_apps)
            setTextColor(Color.parseColor("#94A3B8"))
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 13f
            setOnClickListener { showAppDrawerDialog() }
        }
        bottomBar.addView(allAppsBtn)

        val statsBtn = Button(this).apply {
            text = "Wellbeing"
            setTextColor(Color.parseColor("#10B981"))
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 13f
            setOnClickListener { showDigitalWellbeingDashboard() }
        }
        bottomBar.addView(statsBtn)

        root.addView(bottomBar)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        handler.post(timeUpdater)
        checkDefaultLauncherStatus()
        updateScreenTimePill()
        loadInstalledApps()
        refreshFavoritesUI()
    }

    private fun checkDefaultLauncherStatus() {
        if (!isDefaultLauncher()) {
            defaultBanner.visibility = View.VISIBLE
        } else {
            defaultBanner.visibility = View.GONE
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun promptSetDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    startActivity(intent)
                    return
                }
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timeUpdater)
    }

    private fun updateClock() {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

        clockView.text = timeFormat.format(now)
        amPmView.text = amPmFormat.format(now).uppercase(Locale.getDefault())
        dateView.text = dateFormat.format(now)

        val cal = Calendar.getInstance()
        val dayIndex = cal.get(Calendar.DAY_OF_YEAR)
        quoteView.text = "\"${positiveAffirmations[dayIndex % positiveAffirmations.size]}\""
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val favPackages = prefs.getFavoritePackages()
        val mindfulPackages = prefs.getMindfulPackages()

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        allInstalledApps = resolveInfos
            .filter { it.activityInfo.packageName != packageName } // don't list self
            .map { ri ->
                val pName = ri.activityInfo.packageName
                val label = ri.loadLabel(pm).toString()
                AppItem(
                    label = label,
                    packageName = pName,
                    isMindful = mindfulPackages.contains(pName),
                    isFavorite = favPackages.contains(pName)
                )
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }

        // If user is opening launcher for the first time with empty favorites, auto-pin common essentials
        if (favPackages.isEmpty() && allInstalledApps.isNotEmpty()) {
            val defaults = listOf("phone", "message", "whatsapp", "youtube", "chrome", "camera")
            allInstalledApps.forEach { app ->
                if (defaults.any { app.label.lowercase(Locale.getDefault()).contains(it) }) {
                    prefs.setFavorite(app.packageName, true)
                }
            }
        }
    }

    private fun refreshFavoritesUI() {
        favoritesContainer.removeAllViews()
        val favPackages = prefs.getFavoritePackages()
        val favApps = allInstalledApps.filter { favPackages.contains(it.packageName) }

        if (favApps.isEmpty()) {
            val emptyMsg = TextView(this).apply {
                text = "Tap 'All Apps' below to add your favorite apps"
                setTextColor(Color.parseColor("#475569"))
                textSize = 14f
                setPadding(0, 40, 0, 0)
            }
            favoritesContainer.addView(emptyMsg)
            return
        }

        favApps.forEach { app ->
            val appRow = TextView(this).apply {
                text = if (prefs.getMindfulPackages().contains(app.packageName)) {
                    "${app.label}  •"
                } else {
                    app.label
                }
                textSize = 24f
                setTextColor(Color.parseColor("#F1F5F9"))
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                setPadding(0, 24, 0, 24)
                setOnClickListener { launchAppWithMindfulness(app) }
            }
            favoritesContainer.addView(appRow)
        }
    }

    private fun launchAppWithMindfulness(app: AppItem) {
        val isMindful = prefs.getMindfulPackages().contains(app.packageName)
        if (isMindful) {
            val intent = Intent(this, MindfulDelayActivity::class.java).apply {
                putExtra(MindfulDelayActivity.EXTRA_TARGET_PACKAGE, app.packageName)
                putExtra(MindfulDelayActivity.EXTRA_TARGET_LABEL, app.label)
            }
            startActivity(intent)
        } else {
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }
    }

    private fun showAppDrawerDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(30, 30, 30, 30)
        }

        val searchInput = EditText(this).apply {
            hint = "Search apps..."
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20, 20, 20, 20)
        }
        dialogView.addView(searchInput)

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                800
            )
            addView(listContainer)
        }
        dialogView.addView(scroll)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        fun updateList(query: String) {
            listContainer.removeAllViews()
            val filtered = allInstalledApps.filter {
                it.label.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            }

            filtered.forEach { app ->
                val isFav = prefs.getFavoritePackages().contains(app.packageName)
                val isMindful = prefs.getMindfulPackages().contains(app.packageName)

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(10, 16, 10, 16)
                }

                val title = TextView(this).apply {
                    text = app.label
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        dialog.dismiss()
                        launchAppWithMindfulness(app)
                    }
                }
                row.addView(title)

                // Favorite Toggle
                val favBtn = Button(this).apply {
                    text = if (isFav) "★ Fav" else "☆ Add"
                    textSize = 11f
                    setTextColor(if (isFav) Color.parseColor("#F59E0B") else Color.parseColor("#64748B"))
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        prefs.setFavorite(app.packageName, !isFav)
                        refreshFavoritesUI()
                        updateList(searchInput.text.toString())
                    }
                }
                row.addView(favBtn)

                // Mindful Delay Toggle
                val mindfulBtn = Button(this).apply {
                    text = if (isMindful) "⏸ Mindful" else "+ Delay"
                    textSize = 11f
                    setTextColor(if (isMindful) Color.parseColor("#10B981") else Color.parseColor("#64748B"))
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        prefs.setMindful(app.packageName, !isMindful)
                        refreshFavoritesUI()
                        updateList(searchInput.text.toString())
                    }
                }
                row.addView(mindfulBtn)

                listContainer.addView(row)
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateList(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })

        updateList("")
        dialog.show()
    }

    private fun updateScreenTimePill() {
        if (!usageStatsHelper.hasUsagePermission()) {
            screenTimePill.text = "📊 Tap to Enable Screen Time & Digital Wellbeing"
            screenTimePill.setTextColor(Color.parseColor("#F59E0B"))
            screenTimePill.setBackgroundColor(Color.parseColor("#292524"))
            return
        }

        val report = usageStatsHelper.getTodayUsageReport()
        val totalMinutes = (report.totalScreenTimeMillis / (1000 * 60)).toInt()
        val goalMinutes = usageStatsHelper.dailyGoalMinutes
        val remainingMinutes = goalMinutes - totalMinutes

        val statusText = if (remainingMinutes > 0) {
            "${remainingMinutes}m left"
        } else {
            "${-remainingMinutes}m over goal!"
        }

        screenTimePill.text = "⏱ Today: ${report.formattedTotalTime} • Goal: ${goalMinutes / 60}h (${statusText}) • ${report.unlockCount} Unlocks"
        screenTimePill.setTextColor(if (remainingMinutes >= 0) Color.parseColor("#34D399") else Color.parseColor("#F87171"))
        screenTimePill.setBackgroundColor(Color.parseColor("#0F172A"))
    }

    private fun showDigitalWellbeingDashboard() {
        if (!usageStatsHelper.hasUsagePermission()) {
            usageStatsHelper.openUsageAccessSettings()
            return
        }

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#090D16"))
            setPadding(36, 36, 36, 36)
        }

        // Header Title
        val headerTitle = TextView(this).apply {
            text = "📊 Digital Wellbeing & Reports"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        dialogView.addView(headerTitle)

        // Dynamic container for Today vs Weekly
        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                900
            )
            addView(contentContainer)
        }

        // Tab Row
        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }

        val todayTabBtn = Button(this).apply {
            text = "Today's Report"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#10B981"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val weeklyTabBtn = Button(this).apply {
            text = "Weekly (7 Days)"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setBackgroundColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        tabRow.addView(todayTabBtn)
        tabRow.addView(weeklyTabBtn)
        dialogView.addView(tabRow)
        dialogView.addView(scroll)

        fun renderToday() {
            contentContainer.removeAllViews()
            todayTabBtn.setBackgroundColor(Color.parseColor("#10B981"))
            todayTabBtn.setTextColor(Color.WHITE)
            weeklyTabBtn.setBackgroundColor(Color.parseColor("#1E293B"))
            weeklyTabBtn.setTextColor(Color.parseColor("#94A3B8"))

            val report = usageStatsHelper.getTodayUsageReport()
            val totalMinutes = (report.totalScreenTimeMillis / (1000 * 60)).toInt()
            val goalMinutes = usageStatsHelper.dailyGoalMinutes
            val remaining = goalMinutes - totalMinutes

            val statsBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#131B2E"))
                setPadding(24, 24, 24, 24)
            }

            val screenTimeText = TextView(this).apply {
                text = "Screen Time Today: ${report.formattedTotalTime}"
                setTextColor(Color.WHITE)
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
            }
            statsBox.addView(screenTimeText)

            val goalText = TextView(this).apply {
                text = "Daily Goal: ${goalMinutes / 60}h (${if (remaining >= 0) "${remaining}m remaining" else "${-remaining}m exceeded!"})"
                setTextColor(if (remaining >= 0) Color.parseColor("#34D399") else Color.parseColor("#F87171"))
                textSize = 13f
                setPadding(0, 6, 0, 12)
            }
            statsBox.addView(goalText)

            val unlocksText = TextView(this).apply {
                text = "🔓 Phone Unlocks: ${report.unlockCount} times picked up today"
                setTextColor(Color.parseColor("#CBD5E1"))
                textSize = 13f
                setPadding(0, 0, 0, 8)
            }
            statsBox.addView(unlocksText)

            val savedText = TextView(this).apply {
                text = "🎯 Mindless opens resisted: ${prefs.opensPrevented} (${prefs.minutesSaved}m saved)"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 13f
            }
            statsBox.addView(savedText)
            contentContainer.addView(statsBox)

            // Top Apps Header
            val topAppsLabel = TextView(this).apply {
                text = "Top Used Apps Today:"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 24, 0, 12)
            }
            contentContainer.addView(topAppsLabel)

            if (report.topApps.isEmpty()) {
                val empty = TextView(this).apply {
                    text = "No apps used yet today."
                    setTextColor(Color.parseColor("#64748B"))
                }
                contentContainer.addView(empty)
            } else {
                report.topApps.forEach { app ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 12, 0, 12)
                    }
                    val name = TextView(this).apply {
                        text = app.appName
                        setTextColor(Color.WHITE)
                        textSize = 15f
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val time = TextView(this).apply {
                        text = app.formattedTime
                        setTextColor(Color.parseColor("#34D399"))
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    row.addView(name)
                    row.addView(time)
                    contentContainer.addView(row)
                }
            }
        }

        fun renderWeekly() {
            contentContainer.removeAllViews()
            weeklyTabBtn.setBackgroundColor(Color.parseColor("#10B981"))
            weeklyTabBtn.setTextColor(Color.WHITE)
            todayTabBtn.setBackgroundColor(Color.parseColor("#1E293B"))
            todayTabBtn.setTextColor(Color.parseColor("#94A3B8"))

            val weekly = usageStatsHelper.getWeeklyReport()

            val summaryBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#131B2E"))
                setPadding(24, 24, 24, 24)
            }

            val totalText = TextView(this).apply {
                text = "Weekly Total: ${weekly.formattedWeeklyTotal}"
                setTextColor(Color.WHITE)
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
            }
            summaryBox.addView(totalText)

            val avgText = TextView(this).apply {
                text = "Daily Average: ${weekly.formattedDailyAverage} / day"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 14f
                setPadding(0, 6, 0, 6)
            }
            summaryBox.addView(avgText)

            val goalScore = TextView(this).apply {
                text = "Goal Achieved: ${weekly.daysUnderGoal} of 7 days"
                setTextColor(if (weekly.daysUnderGoal >= 4) Color.parseColor("#34D399") else Color.parseColor("#F87171"))
                textSize = 13f
            }
            summaryBox.addView(goalScore)
            contentContainer.addView(summaryBox)

            // 7-Day History Chart
            val chartHeader = TextView(this).apply {
                text = "7-Day Usage History:"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 24, 0, 12)
            }
            contentContainer.addView(chartHeader)

            val maxTime = weekly.dailyHistory.maxOfOrNull { it.totalTimeMillis } ?: 1L
            weekly.dailyHistory.forEach { day ->
                val dayRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 8, 0, 8)
                }

                val dayLabel = TextView(this).apply {
                    text = day.dayName
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(90, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                dayRow.addView(dayLabel)

                // Visual Bar
                val barProgress = if (maxTime > 0) ((day.totalTimeMillis.toFloat() / maxTime) * 15).toInt().coerceAtLeast(1) else 1
                val barStr = "█".repeat(barProgress)

                val barView = TextView(this).apply {
                    text = barStr
                    val isOver = day.totalTimeMillis > (usageStatsHelper.dailyGoalMinutes * 60 * 1000L)
                    setTextColor(if (isOver) Color.parseColor("#F87171") else Color.parseColor("#10B981"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                dayRow.addView(barView)

                val timeView = TextView(this).apply {
                    text = day.formattedTime
                    setTextColor(Color.parseColor("#CBD5E1"))
                    textSize = 12f
                }
                dayRow.addView(timeView)
                contentContainer.addView(dayRow)
            }

            // Top Apps of the Week
            val topAppsWeekLabel = TextView(this).apply {
                text = "Most Used Apps (Past 7 Days):"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 24, 0, 12)
            }
            contentContainer.addView(topAppsWeekLabel)

            weekly.topAppsOfWeek.forEach { app ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 10, 0, 10)
                }
                val name = TextView(this).apply {
                    text = app.appName
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val time = TextView(this).apply {
                    text = app.formattedTime
                    setTextColor(Color.parseColor("#34D399"))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                row.addView(name)
                row.addView(time)
                contentContainer.addView(row)
            }
        }

        todayTabBtn.setOnClickListener { renderToday() }
        weeklyTabBtn.setOnClickListener { renderWeekly() }

        renderToday()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Change Goal") { _, _ ->
                val goals = arrayOf("1h 30m", "2h 00m (Default)", "2h 30m", "3h 00m")
                val goalValues = intArrayOf(90, 120, 150, 180)
                AlertDialog.Builder(this)
                    .setTitle("Select Daily Screen Time Goal")
                    .setItems(goals) { _, which ->
                        usageStatsHelper.dailyGoalMinutes = goalValues[which]
                        updateScreenTimePill()
                        showDigitalWellbeingDashboard()
                    }
                    .show()
            }
            .create()

        dialog.show()
    }

    override fun onBackPressed() {
        // As a launcher, back button simply closes any open overlay and stays home
    }
}
