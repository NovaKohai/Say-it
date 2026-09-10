package com.example.sayit.domain.repository

import com.example.sayit.domain.model.AppUpdateInfo
import com.example.sayit.domain.model.DownloadProgress
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Interface defining update check and APK download capabilities.
 */
interface UpdateRepository {
    /**
     * Checks the remote release repository for newer app versions.
     */
    suspend fun checkForUpdate(
        owner: String = "NovaKohai",
        repo: String = "Say-it"
    ): Result<AppUpdateInfo>

    /**
     * Downloads the APK file from the provided URL, streaming progress emissions.
     */
    fun downloadApk(
        downloadUrl: String,
        destinationFile: File
    ): Flow<DownloadProgress>
}
