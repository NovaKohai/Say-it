package com.example.sayit.data.local

import android.content.Context
import android.content.SharedPreferences

import com.example.sayit.core.localization.AppLanguage

class SayItPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var monthlyBudget: Double
        get() = prefs.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_MONTHLY_BUDGET, value.toFloat()).apply()

    var activeTimePeriod: String
        get() = prefs.getString(KEY_ACTIVE_TIME_PERIOD, "THIS_MONTH") ?: "THIS_MONTH"
        set(value) = prefs.edit().putString(KEY_ACTIVE_TIME_PERIOD, value).apply()

    var isPrivacyDisclosureAccepted: Boolean
        get() = prefs.getInt(KEY_PRIVACY_DISCLOSURE_VERSION, 0) >= CURRENT_PRIVACY_DISCLOSURE_VERSION
        set(value) = prefs.edit()
            .putInt(KEY_PRIVACY_DISCLOSURE_VERSION, if (value) CURRENT_PRIVACY_DISCLOSURE_VERSION else 0)
            .apply()

    var appLanguage: AppLanguage
        get() {
            val code = prefs.getString(KEY_APP_LANGUAGE, "AR") ?: "AR"
            return try {
                AppLanguage.valueOf(code)
            } catch (_: IllegalArgumentException) {
                AppLanguage.AR
            }
        }
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value.name).apply()

    var userPreferredName: String
        get() = prefs.getString(KEY_USER_PREFERRED_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_PREFERRED_NAME, value).apply()

    var geminiApiKey: String
        get() {
            val saved = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
            if (saved.isNotBlank()) return saved
            return DEFAULT_API_KEY
        }
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var aiModel: String
        get() {
            val saved = prefs.getString(KEY_AI_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL
            if (saved.isBlank() || saved.contains("nano-omni") || saved.contains("gemma")) {
                return DEFAULT_AI_MODEL
            }
            return saved
        }
        set(value) = prefs.edit().putString(KEY_AI_MODEL, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var hasCompletedTour: Boolean
        get() = prefs.getBoolean(KEY_HAS_COMPLETED_TOUR, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_COMPLETED_TOUR, value).apply()

    var lastUpdateCheckTime: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_TIME, value).apply()

    companion object {
        private const val PREFS_NAME = "sayit_prefs"
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_HAS_COMPLETED_TOUR = "has_completed_tour"
        private const val KEY_LAST_UPDATE_CHECK_TIME = "last_update_check_time"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_ACTIVE_TIME_PERIOD = "active_time_period"
        private const val KEY_PRIVACY_DISCLOSURE_VERSION = "privacy_disclosure_version"
        private const val CURRENT_PRIVACY_DISCLOSURE_VERSION = 2
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_USER_PREFERRED_NAME = "user_preferred_name"
        val DEFAULT_API_KEY: String get() = com.example.sayit.BuildConfig.GEMINI_API_KEY
        const val DEFAULT_AI_MODEL = "inclusionai/ling-3.0-flash-fin:free"
    }
}
