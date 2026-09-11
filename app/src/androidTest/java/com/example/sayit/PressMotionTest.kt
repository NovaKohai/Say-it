package com.example.sayit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sayit.presentation.common.pressScale
import com.example.sayit.theme.SayItTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PressMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nativeButtonScalesAndInvokesOnlyOneAction() {
        var clicks = 0
        compose.setContent {
            SayItTheme {
                val source = remember { MutableInteractionSource() }
                Button(onClick = { clicks++ }, interactionSource = source,
                    modifier = Modifier.pressScale(interactionSource = source).testTag("action")) {
                    Text("Action")
                }
            }
        }
        val button = compose.onNodeWithTag("action")
        val initialWidth = button.fetchSemanticsNode().boundsInRoot.width
        compose.mainClock.autoAdvance = false
        button.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(160)
        val pressedWidth = button.fetchSemanticsNode().boundsInRoot.width
        assertTrue("A native Material button must receive press feedback", pressedWidth < initialWidth)
        button.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(240)
        compose.runOnIdle { assertEquals(1, clicks) }
        assertEquals(initialWidth, button.fetchSemanticsNode().boundsInRoot.width, 0.5f)
    }

    @Test fun disabledCustomActionCannotBeInvoked() {
        var clicks = 0
        compose.setContent {
            SayItTheme {
                Text("Disabled", Modifier.testTag("disabled").pressScale(enabled = false, onClick = { clicks++ }))
            }
        }
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, clicks) }
    }
}
