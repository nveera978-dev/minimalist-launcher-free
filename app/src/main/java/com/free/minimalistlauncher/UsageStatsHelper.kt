package com.free.minimalistlauncher

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeMillis: Long
) {
    val formattedTime: String
        get() {
            val totalMinutes = totalTimeMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
}

data class DayUsageSummary(
    val dayName: String,
    val totalTimeMillis: Long
) {
    val formattedTime: String
        get() {
            val totalMinutes = totalTimeMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
}

data class DailyUsageReport(
    val totalScreenTimeMillis: Long,
    val unlockCount: Int,
    val topApps: List<AppUsageInfo>
) {
    val formattedTotalTime: String
        get() {
            val totalMinutes = totalScreenTimeMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
}

data class WeeklyUsageReport(
    val totalTimeMillis: Long,
    val dailyAverageMillis: Long,
    val daysExceededGoal: Int,
    val daysUnderGoal: Int,
    val dailyHistory: List<DayUsageSummary>,
    val topAppsOfWeek: List<AppUsageInfo>
) {
    val formattedWeeklyTotal: String
        get() {
            val totalMinutes = totalTimeMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return "${hours}h ${mins}m"
        }

    val formattedDailyAverage: String
        get() {
            val totalMinutes = dailyAverageMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return "${hours}h ${mins}m"
        }
}

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    companion object {
        private const val PREFS_NAME = "digital_wellbeing_prefs"
        private const val KEY_DAILY_GOAL_MINUTES = "daily_goal_minutes"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var dailyGoalMinutes: Int
        get() = prefs.getInt(KEY_DAILY_GOAL_MINUTES, 120) // Default 2 hours goal
        set(value) = prefs.edit().putInt(KEY_DAILY_GOAL_MINUTES, value).apply()

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun getTodayUsageReport(): DailyUsageReport {
        if (!hasUsagePermission()) {
            return DailyUsageReport(0, 0, emptyList())
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val pm = context.packageManager
        val appUsageList = mutableListOf<AppUsageInfo>()
        var totalScreenMillis = 0L

        stats?.filter { it.totalTimeInForeground > 30 * 1000 } // Over 30s
            ?.forEach { stat ->
                totalScreenMillis += stat.totalTimeInForeground
                val appName = try {
                    val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    stat.packageName
                }
                appUsageList.add(AppUsageInfo(stat.packageName, appName, stat.totalTimeInForeground))
            }

        // Count unlocks (Screen ON / Keyguard dismissed)
        var unlocks = 0
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE)) {
                    unlocks++
                }
            }
        } catch (_: Exception) {
            unlocks = 0
        }

        val topApps = appUsageList.sortedByDescending { it.totalTimeMillis }.take(5)
        return DailyUsageReport(totalScreenMillis, unlocks, topApps)
    }

    fun getWeeklyReport(): WeeklyUsageReport {
        if (!hasUsagePermission()) {
            return WeeklyUsageReport(0, 0, 0, 0, emptyList(), emptyList())
        }

        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dailyList = mutableListOf<DayUsageSummary>()
        val appAggregator = mutableMapOf<String, Long>()
        var weeklyTotalMillis = 0L
        var daysExceeded = 0
        var daysUnder = 0
        val goalMillis = dailyGoalMinutes * 60 * 1000L

        for (i in 6 downTo 0) {
            val calStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calEnd = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val dayName = days[calStart.get(Calendar.DAY_OF_WEEK) - 1]
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                calStart.timeInMillis,
                calEnd.timeInMillis
            )

            var dayTotal = 0L
            stats?.forEach { stat ->
                dayTotal += stat.totalTimeInForeground
                val current = appAggregator.getOrDefault(stat.packageName, 0L)
                appAggregator[stat.packageName] = current + stat.totalTimeInForeground
            }

            weeklyTotalMillis += dayTotal
            if (dayTotal > goalMillis) daysExceeded++ else daysUnder++
            dailyList.add(DayUsageSummary(dayName, dayTotal))
        }

        val dailyAverageMillis = if (dailyList.isNotEmpty()) weeklyTotalMillis / dailyList.size else 0L

        val pm = context.packageManager
        val topAppsOfWeek = appAggregator.map { (pkg, time) ->
            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg
            }
            AppUsageInfo(pkg, appName, time)
        }.sortedByDescending { it.totalTimeMillis }.take(5)

        return WeeklyUsageReport(
            totalTimeMillis = weeklyTotalMillis,
            dailyAverageMillis = dailyAverageMillis,
            daysExceededGoal = daysExceeded,
            daysUnderGoal = daysUnder,
            dailyHistory = dailyList,
            topAppsOfWeek = topAppsOfWeek
        )
    }
}
