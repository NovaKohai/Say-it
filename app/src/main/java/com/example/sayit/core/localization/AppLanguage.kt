package com.example.sayit.core.localization

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val code: String,
    val titleNative: String,
    val titleEnglish: String,
    val layoutDirection: LayoutDirection
) {
    AR("ar", "العربية", "Arabic", LayoutDirection.Rtl),
    EN("en", "English", "English", LayoutDirection.Ltr)
}
