package com.example.sayit.presentation.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.sayit.core.localization.AppStrings
import com.example.sayit.presentation.common.EmilEasings
import com.example.sayit.presentation.common.motionEnabled

@Composable
internal fun BottomNavBar(currentTab: NavTab, developerUnlocked: Boolean, strings: AppStrings, onSelect: (NavTab) -> Unit) {
    val allowMotion = motionEnabled()
    val tabs = buildList {
        add(NavTab.Dashboard); add(NavTab.Installments); add(NavTab.Analytics); add(NavTab.AiCopilot)
        if (developerUnlocked) add(NavTab.SqlDb)
        add(NavTab.Settings)
    }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            val selected = currentTab == tab
            val iconScale by animateFloatAsState(
                targetValue = if (selected && allowMotion) 1.08f else 1f,
                animationSpec = if (allowMotion) tween(160, easing = EmilEasings.StrongEaseOut) else snap(),
                label = "${tab::class.simpleName}SelectionScale"
            )
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer {
                            val scale = if (allowMotion) iconScale else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                },
                label = { Text(tab.getTitle(strings), maxLines = 2) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
