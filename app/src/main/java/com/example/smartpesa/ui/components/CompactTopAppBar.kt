package com.example.smartpesa.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Compact TopAppBar with reduced height (48dp instead of Material 3's default 64dp)
 * Optimized for devices where screen real estate is important
 *
 * Uses Material 3 surface background with elevation:
 * - Light mode: shadow elevation for depth
 * - Dark mode: tonal elevation for subtle surface tinting
 *
 * Supports: navigation icon (hamburger menu) + title + action buttons
 */
@Composable
fun CompactTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    navigationIconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp), // Compact height - 25% smaller than Material 3 default
        color = containerColor,
        tonalElevation = 3.dp,    // Subtle tint in dark mode
        shadowElevation = 4.dp     // Shadow in light mode
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp), // Reduced horizontal padding for compact feel
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation icon (hamburger menu)
            CompositionLocalProvider(
                LocalContentColor provides navigationIconContentColor
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    navigationIcon()
                }
            }

            // Title - takes available space
            CompositionLocalProvider(
                LocalContentColor provides titleContentColor
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    ProvideTextStyle(value = MaterialTheme.typography.titleMedium) {
                        title()
                    }
                }
            }

            // Action buttons
            CompositionLocalProvider(
                LocalContentColor provides actionIconContentColor
            ) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}
