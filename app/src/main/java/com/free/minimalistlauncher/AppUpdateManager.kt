package com.free.minimalistlauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateManager(private val activity: Activity) {

    private val handler = Handler(Looper.getMainLooper())
    private val repoReleasesApi = "https://api.github.com/repos/nveera978-dev/minimalist-launcher-free/releases/latest"

    fun checkForUpdates(isManualCheck: Boolean = false) {
        Thread {
            try {
                val url = URL(repoReleasesApi)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "MinimalistLauncher-Android")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestTag = json.optString("tag_name", "")
                    val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")
                    
                    var apkDownloadUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                apkDownloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    val currentVersion = try {
                        val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                        pInfo.versionName ?: "1.0.0"
                    } catch (e: Exception) {
                        "1.0.0"
                    }

                    val isNewer = isVersionNewer(currentVersion, latestTag)

                    handler.post {
                        if (isNewer && apkDownloadUrl.isNotEmpty()) {
                            showUpdateAvailableDialog(latestTag, releaseNotes, apkDownloadUrl)
                        } else if (isManualCheck) {
                            Toast.makeText(activity, "You have the latest version ($currentVersion)", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (isManualCheck) {
                    handler.post {
                        Toast.makeText(activity, "No updates found at this time", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (isManualCheck) {
                    handler.post {
                        Toast.makeText(activity, "Could not check for updates. Check internet connection.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun isVersionNewer(current: String, remote: String): Boolean {
        val cleanCurrent = current.removePrefix("v").trim()
        val cleanRemote = remote.removePrefix("v").trim()
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun showUpdateAvailableDialog(version: String, notes: String, downloadUrl: String) {
        AlertDialog.Builder(activity)
            .setTitle("🚀 Update Available: $version")
            .setMessage("A new update is available for Minimalist Launcher!\n\nHighlights:\n${notes.take(300)}\n\nWould you like to update directly inside the app now?")
            .setPositiveButton("Update Now") { _, _ ->
                checkInstallPermissionAndDownload(downloadUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun checkInstallPermissionAndDownload(downloadUrl: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(activity, "Please allow 'Install unknown apps' to complete the in-app update", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
                return
            }
        }
        startInAppDownload(downloadUrl)
    }

    @Suppress("DEPRECATION")
    private fun startInAppDownload(downloadUrl: String) {
        val progressDialog = ProgressDialog(activity).apply {
            setTitle("Downloading Update")
            setMessage("Downloading latest version inside app...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            max = 100
            setCancelable(false)
            show()
        }

        Thread {
            try {
                val url = URL(downloadUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                // Handle redirect if any (GitHub releases redirect to AWS S3)
                var redirectConn = conn
                var status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER) {
                    val newUrl = conn.getHeaderField("Location")
                    redirectConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                    }
                }

                val totalLength = redirectConn.contentLength
                val dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: activity.cacheDir
                val apkFile = File(dir, "MinimalistLauncher-Update.apk")
                if (apkFile.exists()) apkFile.delete()

                redirectConn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded: Long = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (totalLength > 0) {
                                val progress = ((downloaded * 100) / totalLength).toInt()
                                handler.post { progressDialog.progress = progress }
                            }
                        }
                    }
                }

                handler.post {
                    progressDialog.dismiss()
                    triggerInAppInstall(apkFile)
                }
            } catch (e: Exception) {
                handler.post {
                    progressDialog.dismiss()
                    Toast.makeText(activity, "Update download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun triggerInAppInstall(apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Failed to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
