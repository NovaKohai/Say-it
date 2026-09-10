package com.example.sayit.domain.model

import java.io.File

/**
 * Represents the live progress state of an ongoing APK download.
 */
data class DownloadProgress(
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val downloadedFile: File? = null,
    val errorMessage: String? = null
)

enum class DownloadStatus {
    IDLE,
    CONNECTING,
    DOWNLOADING,
    COMPLETED,
    ERROR,
    CANCELLED
}
