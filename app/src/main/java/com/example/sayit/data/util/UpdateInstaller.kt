package com.example.sayit.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed class InstallResult {
    object Success : InstallResult()
    data class NeedsUnknownSourcePermission(val intent: Intent) : InstallResult()
    data class Failure(val error: String) : InstallResult()
}

object UpdateInstaller {

    fun installApk(context: Context, apkFile: File): InstallResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return InstallResult.Failure("الملف غير موجود أو تالف")
        }

        // On Android 8.0+ (Oreo), verify permission to install unknown apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            if (!canInstall) {
                val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                return InstallResult.NeedsUnknownSourcePermission(permissionIntent)
            }
        }

        return try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            InstallResult.Success
        } catch (e: Exception) {
            InstallResult.Failure(e.localizedMessage ?: "تعذر تشغيل مثبت الحزم")
        }
    }
}
