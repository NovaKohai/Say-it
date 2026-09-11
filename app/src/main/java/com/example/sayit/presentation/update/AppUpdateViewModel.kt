package com.example.sayit.presentation.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sayit.BuildConfig
import com.example.sayit.data.repository.UpdateRepositoryImpl
import com.example.sayit.data.util.InstallResult
import com.example.sayit.data.util.UpdateInstaller
import com.example.sayit.data.util.UpdateNotificationHelper
import com.example.sayit.domain.model.AppUpdateInfo
import com.example.sayit.domain.model.DownloadProgress
import com.example.sayit.domain.model.DownloadStatus
import com.example.sayit.domain.repository.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UpdateUiState(
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val isDialogVisible: Boolean = false,
    val downloadProgress: DownloadProgress = DownloadProgress(),
    val isDownloading: Boolean = false,
    val isReadyToInstall: Boolean = false,
    val downloadedApkFile: File? = null,
    val userErrorMessage: String? = null,
    val lastCheckedTimestamp: Long = 0L,
    val isUpToDateFeedback: Boolean = false
)

class AppUpdateViewModel(
    context: Context,
    updateRepository: UpdateRepository? = null
) : ViewModel() {

    private val appContext = context.applicationContext
    private val updateRepository: UpdateRepository = updateRepository ?: UpdateRepositoryImpl(appContext)

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    /**
     * Checks for updates from GitHub repository.
     * @param isManual true if triggered by clicking "Check for Updates" button
     * @param isArabic whether the current interface is in Arabic
     */
    fun checkForUpdate(isManual: Boolean = false, isArabic: Boolean = true) {
        if (_uiState.value.isChecking || _uiState.value.isDownloading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, userErrorMessage = null, isUpToDateFeedback = false) }

            val result = updateRepository.checkForUpdate()

            result.onSuccess { info ->
                val now = System.currentTimeMillis()
                if (info.isUpdateAvailable) {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            isUpdateAvailable = true,
                            updateInfo = info,
                            isDialogVisible = true,
                            lastCheckedTimestamp = now,
                            isUpToDateFeedback = false
                        )
                    }
                    // Trigger system notification
                    UpdateNotificationHelper.showUpdateNotification(appContext, info, isArabic)
                } else {
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            isUpdateAvailable = false,
                            updateInfo = info,
                            lastCheckedTimestamp = now,
                            isUpToDateFeedback = isManual
                        )
                    }
                    if (isManual) {
                        // Auto-hide feedback message after 3.5 seconds
                        delay(3500)
                        _uiState.update { it.copy(isUpToDateFeedback = false) }
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        userErrorMessage = if (isManual) {
                            err.localizedMessage ?: "تعذر التحقق من التحديثات"
                        } else null
                    )
                }
            }
        }
    }

    /**
     * Starts downloading the update APK to the internal cache directory.
     */
    fun startDownload() {
        val info = _uiState.value.updateInfo ?: return
        if (info.downloadUrl.isBlank()) return

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            val destinationFile = File(appContext.cacheDir, "updates/SayIt-v${info.latestVersionName}.apk")

            _uiState.update {
                it.copy(
                    isDownloading = true,
                    isReadyToInstall = false,
                    userErrorMessage = null,
                    downloadProgress = DownloadProgress(status = DownloadStatus.CONNECTING)
                )
            }

            updateRepository.downloadApk(info.downloadUrl, destinationFile).collect { progress ->
                when (progress.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.CONNECTING -> {
                        _uiState.update {
                            it.copy(
                                isDownloading = true,
                                downloadProgress = progress
                            )
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                isReadyToInstall = true,
                                downloadedApkFile = progress.downloadedFile ?: destinationFile,
                                downloadProgress = progress
                            )
                        }
                    }
                    DownloadStatus.ERROR -> {
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                userErrorMessage = progress.errorMessage ?: "فشل التحميل",
                                downloadProgress = progress
                            )
                        }
                    }
                    DownloadStatus.CANCELLED -> {
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                downloadProgress = DownloadProgress(status = DownloadStatus.IDLE)
                            )
                        }
                    }
                    DownloadStatus.IDLE -> Unit
                }
            }
        }
    }

    /**
     * Cancels an active download.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        _uiState.update {
            it.copy(
                isDownloading = false,
                downloadProgress = DownloadProgress(status = DownloadStatus.CANCELLED)
            )
        }
    }

    /**
     * Installs the downloaded APK via Android PackageInstaller.
     */
    fun installUpdate(context: Context): InstallResult {
        val file = _uiState.value.downloadedApkFile ?: File(
            context.cacheDir,
            "updates/SayIt-v${_uiState.value.updateInfo?.latestVersionName ?: "latest"}.apk"
        )
        return UpdateInstaller.installApk(context, file)
    }

    fun dismissDialog() {
        if (_uiState.value.isDownloading) {
            cancelDownload()
        }
        _uiState.update { it.copy(isDialogVisible = false) }
    }

    fun openDialog() {
        _uiState.update { it.copy(isDialogVisible = true) }
    }

    /**
     * Helper to simulate an update (e.g. v1.1.0) with mock progress for UI demonstration and test.
     */
    fun simulateMockUpdate(isArabic: Boolean) {
        val mockInfo = AppUpdateInfo(
            currentVersionName = BuildConfig.VERSION_NAME,
            latestVersionName = "1.1.0",
            latestVersionCode = 2,
            releaseTitle = if (isArabic) "تحديث Say It v1.1.0 الجديد" else "Say It v1.1.0 New Release",
            releaseNotes = if (isArabic) {
                "• تحسين سرعة واستجابة الشات بوت المالي ⚡\n• شريط تقدم حي ومحدث للتحميل 📊\n• إصلاحات عامة وتوافق أوسع مع رسائل البنوك المصرية 🏦"
            } else {
                "• Enhanced AI Copilot speed and responsiveness ⚡\n• Live animated download progress bar 📊\n• General stability improvements & wider bank SMS support 🏦"
            },
            downloadUrl = "https://github.com/NovaKohai/Say-it/releases/download/v1.1.0/SayIt-v1.1.0.apk",
            apkSizeMb = 14.8,
            isUpdateAvailable = true,
            publishedAt = "2026-09-10",
            htmlUrl = "https://github.com/NovaKohai/Say-it/releases"
        )

        _uiState.update {
            it.copy(
                isChecking = false,
                isUpdateAvailable = true,
                updateInfo = mockInfo,
                isDialogVisible = true,
                isReadyToInstall = false,
                isDownloading = false,
                downloadProgress = DownloadProgress()
            )
        }

        UpdateNotificationHelper.showUpdateNotification(appContext, mockInfo, isArabic)
    }

    /**
     * Simulates downloading progress for testing the progress bar animation on device.
     */
    fun simulateDownloadProgress() {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    isReadyToInstall = false,
                    downloadProgress = DownloadProgress(status = DownloadStatus.CONNECTING)
                )
            }
            delay(400)

            val total = 15_518_976L // ~14.8 MB
            for (percent in 5..100 step 5) {
                val downloaded = (total * percent) / 100
                _uiState.update {
                    it.copy(
                        isDownloading = percent < 100,
                        isReadyToInstall = percent == 100,
                        downloadProgress = DownloadProgress(
                            bytesDownloaded = downloaded,
                            totalBytes = total,
                            percentage = percent,
                            status = if (percent == 100) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING
                        )
                    )
                }
                delay(120)
            }
        }
    }
}
