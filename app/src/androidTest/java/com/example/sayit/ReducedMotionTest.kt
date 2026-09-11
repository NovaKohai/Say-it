package com.example.sayit

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sayit.core.localization.EnglishStrings
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.presentation.common.motionEnabled
import com.example.sayit.presentation.common.pressScale
import com.example.sayit.presentation.splash.SplashScreen
import com.example.sayit.theme.SayItTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReducedMotionTest {
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 0f
    })

    @Test fun disabledSystemAnimationsKeepPressSizeStable() {
        var enabled = true
        compose.setContent {
            SayItTheme {
                enabled = motionEnabled()
                Text("Action", Modifier.testTag("action").pressScale(onClick = {}))
            }
        }
        compose.runOnIdle { assertFalse(enabled) }
        val button = compose.onNodeWithTag("action")
        val width = button.fetchSemanticsNode().boundsInRoot.width
        button.performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals(width, button.fetchSemanticsNode().boundsInRoot.width, 0.01f)
        button.performTouchInput { up() }
    }

    @Test fun splashDoesNotDelayWhenMotionIsDisabled() {
        var completions = 0
        compose.setContent {
            SayItTheme {
                CompositionLocalProvider(LocalStrings provides EnglishStrings) {
                    SplashScreen(isArabic = false, onSplashFinished = { completions++ })
                }
            }
        }
        compose.runOnIdle { assertEquals(1, completions) }
    }
}
