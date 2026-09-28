package com.example.aitester.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aitester.MainActivity
import com.example.aitester.R
import com.example.aitester.data.preferences.PreferencesManager

class IssueCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "issues_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "issue_check_work"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    importance
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun showTestNotification(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            createChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.new_issue))
                .setContentText(context.getString(R.string.new_issue_notification))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    private val preferencesManager = PreferencesManager(applicationContext)

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val (owner, repo) = preferencesManager.getFullRepo()
        val lastKnownId = preferencesManager.getLastKnownIssueId()

        return try {
            val latestIssueId = fetchLatestIssueId(owner, repo)

            if (latestIssueId > lastKnownId && lastKnownId > 0) {
                showNotification(latestIssueId)
                preferencesManager.saveLastKnownIssueId(latestIssueId)
            } else if (lastKnownId == 0L) {
                // First run - just save the latest issue id without notification
                preferencesManager.saveLastKnownIssueId(latestIssueId)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchLatestIssueId(owner: String, repo: String): Long {
        val url = java.net.URL("https://api.github.com/repos/$owner/$repo/issues?state=open&sort=updated&per_page=1")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("User-Agent", "AITESTER-App")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw Exception("GitHub API error: $responseCode")
        }

        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val jsonArray = org.json.JSONArray(response)
        if (jsonArray.length() > 0) {
            val obj = jsonArray.getJSONObject(0)
            return obj.getLong("id")
        }
        return 0L
    }

    private fun createNotificationChannel() {
        createChannel(applicationContext)
    }

    private fun showNotification(issueId: Long) {
        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.new_issue))
            .setContentText(applicationContext.getString(R.string.new_issue_notification))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }
}
