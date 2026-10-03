package com.free.minimalistlauncher

import android.content.Context
import android.content.SharedPreferences

data class AppItem(
    val label: String,
    val packageName: String,
    val isMindful: Boolean = false,
    val isFavorite: Boolean = false
)

class LauncherPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("minimalist_launcher_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FAVORITES = "pref_favorites"
        private const val KEY_MINDFUL_APPS = "pref_mindful_apps"
        private const val KEY_DELAY_SECONDS = "pref_delay_seconds"
        private const val KEY_OPENS_PREVENTED = "pref_opens_prevented"
        private const val KEY_MINUTES_SAVED = "pref_minutes_saved"
    }

    var delaySeconds: Int
        get() = prefs.getInt(KEY_DELAY_SECONDS, 5)
        set(value) = prefs.edit().putInt(KEY_DELAY_SECONDS, value).apply()

    var opensPrevented: Int
        get() = prefs.getInt(KEY_OPENS_PREVENTED, 0)
        set(value) = prefs.edit().putInt(KEY_OPENS_PREVENTED, value).apply()

    var minutesSaved: Int
        get() = prefs.getInt(KEY_MINUTES_SAVED, 0)
        set(value) = prefs.edit().putInt(KEY_MINUTES_SAVED, value).apply()

    fun getFavoritePackages(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun setFavorite(packageName: String, isFav: Boolean) {
        val current = getFavoritePackages().toMutableSet()
        if (isFav) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun getMindfulPackages(): Set<String> {
        return prefs.getStringSet(KEY_MINDFUL_APPS, emptySet()) ?: emptySet()
    }

    fun setMindful(packageName: String, isMindful: Boolean) {
        val current = getMindfulPackages().toMutableSet()
        if (isMindful) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_MINDFUL_APPS, current).apply()
    }

    fun recordPreventedOpen() {
        opensPrevented += 1
        minutesSaved += 3
    }
}
