package com.example.sayit.data.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.sayit.MainActivity
import com.example.sayit.domain.model.AppUpdateInfo

object UpdateNotificationHelper {
    private const val CHANNEL_ID = "sayit_updates_channel"
    private const val NOTIFICATION_ID = 2026

    const val EXTRA_LAUNCH_UPDATE_POPUP = "extra_launch_update_popup"

    fun showUpdateNotification(
        context: Context,
        updateInfo: AppUpdateInfo,
        isArabic: Boolean
    ) {
        // Check Android 13+ POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                if (isArabic) "تحديثات التطبيق" else "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = if (isArabic) "إشعارات عند توفر إصدار جديد من Say It" else "Alerts when a new version of Say It is available"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LAUNCH_UPDATE_POPUP, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isArabic) {
            "🚀 تحديث جديد متاح: v${updateInfo.latestVersionName}"
        } else {
            "🚀 New Update Available: v${updateInfo.latestVersionName}"
        }

        val message = if (isArabic) {
            "تم إطلاق إصدار جديد من Say It. اضغط هنا للتنزيل والاطلاع على المزايا."
        } else {
            "A new version of Say It is ready. Tap to download and view what's new."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
