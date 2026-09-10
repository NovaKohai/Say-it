package com.example.sayit.data.repository

import android.content.Context
import com.example.sayit.BuildConfig
import com.example.sayit.domain.model.AppUpdateInfo
import com.example.sayit.domain.model.DownloadProgress
import com.example.sayit.domain.model.DownloadStatus
import com.example.sayit.domain.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateRepositoryImpl(
    private val context: Context
) : UpdateRepository {

    override suspend fun checkForUpdate(
        owner: String,
        repo: String
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 9000
                readTimeout = 9000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SayIt-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // No releases published yet on this repository
                return@withContext Result.success(
                    AppUpdateInfo(
                        currentVersionName = BuildConfig.VERSION_NAME,
                        latestVersionName = BuildConfig.VERSION_NAME,
                        latestVersionCode = BuildConfig.VERSION_CODE,
                        releaseTitle = "No release available",
                        releaseNotes = "You are running the latest build.",
                        downloadUrl = "",
                        apkSizeMb = 0.0,
                        isUpdateAvailable = false,
                        publishedAt = "",
                        htmlUrl = "https://github.com/$owner/$repo"
                    )
                )
            }

            if (responseCode !in 200..299) {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                return@withContext Result.failure(Exception("GitHub API error ($responseCode): $err"))
            }

            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseObj = JSONObject(jsonStr)

            val rawTagName = releaseObj.optString("tag_name", "")
            val cleanRemoteVersion = rawTagName.removePrefix("v").trim()
            val releaseTitle = releaseObj.optString("name", rawTagName)
            val releaseNotes = releaseObj.optString("body", "تحديث جديد وإصلاحات عامة للتطبيق.")
            val publishedAt = releaseObj.optString("published_at", "")
            val htmlUrl = releaseObj.optString("html_url", "https://github.com/$owner/$repo")

            // Scan release assets for .apk file
            val assetsArray = releaseObj.optJSONArray("assets")
            var downloadUrl = ""
            var apkSizeMb = 0.0

            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        val sizeBytes = asset.optLong("size", 0L)
                        apkSizeMb = (sizeBytes / (1024.0 * 1024.0) * 10).toInt() / 10.0
                        break
                    }
                }
            }

            // Fallback to htmlUrl if direct apk asset isn't attached yet
            if (downloadUrl.isBlank()) {
                downloadUrl = htmlUrl
            }

            val isNewer = isVersionNewer(cleanRemoteVersion, BuildConfig.VERSION_NAME)

            Result.success(
                AppUpdateInfo(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    latestVersionName = cleanRemoteVersion.ifBlank { BuildConfig.VERSION_NAME },
                    latestVersionCode = 0,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    apkSizeMb = apkSizeMb,
                    isUpdateAvailable = isNewer,
                    publishedAt = publishedAt,
                    htmlUrl = htmlUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun downloadApk(
        downloadUrl: String,
        destinationFile: File
    ): Flow<DownloadProgress> = flow {
        emit(DownloadProgress(status = DownloadStatus.CONNECTING))

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            destinationFile.parentFile?.mkdirs()
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = downloadUrl
            var redirects = 0
            val maxRedirects = 5

            // Follow HTTP redirects (301, 302, 307) which GitHub releases use for S3 asset delivery
            while (redirects < maxRedirects) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "SayIt-Android-App")
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl.isNullOrBlank()) break
                    currentUrl = newUrl
                    redirects++
                } else {
                    break
                }
            }

            val finalConn = connection ?: throw IllegalStateException("Failed to open download stream")
            if (finalConn.responseCode !in 200..299) {
                throw IllegalStateException("Download server returned HTTP ${finalConn.responseCode}")
            }

            val totalBytes = finalConn.contentLength.toLong()
            inputStream = finalConn.inputStream
            outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(8 * 1024)
            var bytesDownloaded = 0L
            var read: Int
            var lastEmittedPercentage = -1
            var lastEmitTime = 0L

            while (inputStream.read(buffer).also { read = it } != -1) {
                if (!currentCoroutineContext().isActive) {
                    destinationFile.delete()
                    emit(DownloadProgress(status = DownloadStatus.CANCELLED))
                    return@flow
                }

                outputStream.write(buffer, 0, read)
                bytesDownloaded += read

                val currentPercentage = if (totalBytes > 0) {
                    ((bytesDownloaded * 100) / totalBytes).toInt()
                } else {
                    0
                }

                val now = System.currentTimeMillis()
                // Throttle emissions to avoid Compose recomposition thrashing
                if (currentPercentage != lastEmittedPercentage || now - lastEmitTime >= 80) {
                    lastEmittedPercentage = currentPercentage
                    lastEmitTime = now
                    emit(
                        DownloadProgress(
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            percentage = currentPercentage,
                            status = DownloadStatus.DOWNLOADING
                        )
                    )
                }
            }

            outputStream.flush()

            emit(
                DownloadProgress(
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = if (totalBytes > 0) totalBytes else bytesDownloaded,
                    percentage = 100,
                    status = DownloadStatus.COMPLETED,
                    downloadedFile = destinationFile
                )
            )
        } catch (e: Exception) {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            emit(
                DownloadProgress(
                    status = DownloadStatus.ERROR,
                    errorMessage = e.localizedMessage ?: "حدث خطأ أثناء تنزيل التحديث"
                )
            )
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        /**
         * Compares semantic version strings like "1.2.0" and "1.1.9".
         * Returns true if remote is strictly newer than current.
         */
        fun isVersionNewer(remote: String, current: String): Boolean {
            if (remote.isBlank() || current.isBlank()) return false
            val cleanRemote = remote.removePrefix("v").trim()
            val cleanCurrent = current.removePrefix("v").trim()

            if (cleanRemote == cleanCurrent) return false

            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
