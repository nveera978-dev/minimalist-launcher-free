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
    private lateinit var defaultBanner: Button
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
            setPadding(0, 0, 0, 36)
        }
        root.addView(quoteView)

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
            text = "Stats"
            setTextColor(Color.parseColor("#10B981"))
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 13f
            setOnClickListener { showStatsDialog() }
        }
        bottomBar.addView(statsBtn)

        root.addView(bottomBar)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        handler.post(timeUpdater)
        checkDefaultLauncherStatus()
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

    private fun showStatsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Digital Detox Progress")
            .setMessage("Mindless opens prevented: ${prefs.opensPrevented}\nEstimated time saved: ${prefs.minutesSaved} minutes\n\nEvery time you pause at the breathing screen, your brain regains focus!")
            .setPositiveButton("Keep Going", null)
            .show()
    }

    override fun onBackPressed() {
        // As a launcher, back button simply closes any open overlay and stays home
    }
}
