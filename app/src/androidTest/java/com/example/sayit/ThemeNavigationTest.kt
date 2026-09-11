package com.example.sayit

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sayit.core.localization.*
import com.example.sayit.presentation.main.*
import com.example.sayit.theme.SayItTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ThemeNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun navigationAndContrastFollowThemeAndLocale() {
        val dark = mutableStateOf(false)
        val arabic = mutableStateOf(false)
        val selected = mutableStateOf<NavTab>(NavTab.Dashboard)
        lateinit var colors: ColorScheme
        compose.setContent {
            val strings = if (arabic.value) ArabicStrings else EnglishStrings
            SayItTheme(darkTheme = dark.value) {
                colors = MaterialTheme.colorScheme
                BottomNavBar(selected.value, false, strings) { selected.value = it }
            }
        }
        for (isDark in listOf(false, true)) {
            for (isArabic in listOf(false, true)) {
                compose.runOnIdle { dark.value = isDark; arabic.value = isArabic }
                val strings = if (isArabic) ArabicStrings else EnglishStrings
                compose.onNodeWithText(strings.tabSettings).performClick().assertIsSelected()
                compose.onNodeWithText(strings.tabDashboard).performClick().assertIsSelected()
                compose.runOnIdle {
                    for ((background, foreground) in listOf(
                        colors.surface to colors.onSurface,
                        colors.surfaceVariant to colors.onSurfaceVariant,
                        colors.primary to colors.onPrimary,
                        colors.secondary to colors.onSecondary,
                        colors.error to colors.onError
                    )) assertTrue("Insufficient contrast in dark=$isDark", contrast(background, foreground) >= 4.5f)
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val first = a.luminance(); val second = b.luminance()
        return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
    }
}
