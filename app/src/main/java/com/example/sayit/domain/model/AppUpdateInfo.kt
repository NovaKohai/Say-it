package com.example.sayit.domain.model

/**
 * Encapsulates update metadata fetched from the remote release source.
 */
data class AppUpdateInfo(
    val currentVersionName: String,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSizeMb: Double,
    val isUpdateAvailable: Boolean,
    val publishedAt: String,
    val htmlUrl: String
)
