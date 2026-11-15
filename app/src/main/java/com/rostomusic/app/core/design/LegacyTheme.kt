package com.rostomusic.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Pont de compatibilité temporaire pour les écrans non encore migrés vers
 * MaterialTheme.colorScheme et MaterialTheme.typography.
 */
@Deprecated("Utiliser MaterialTheme.colorScheme et MaterialTheme.typography")
object Rosto {
    val colors: RostoColors
        @Composable
        @ReadOnlyComposable
        get() {
            val s = MaterialTheme.colorScheme
            val dark = s.surface.luminance() < 0.5f
            return RostoColors(
                background = s.background,
                card = s.surfaceContainer,
                surfaceAlt = s.surfaceContainerHigh,
                textPrimary = s.onSurface,
                textSecondary = s.onSurfaceVariant,
                textMuted = s.outline,
                hairline = s.outlineVariant,
                accent = s.primary,
                onAccent = s.onPrimary,
                accentText = s.primary,
                danger = s.error,
                vinyl = s.surfaceContainerLowest,
                vinylGroove = s.onSurface.copy(alpha = 0.08f),
                isDark = dark,
                accentContainer = s.primaryContainer,
                onAccentContainer = s.onPrimaryContainer,
                surfaceContainerLowest = s.surfaceContainerLowest,
                surfaceContainerLow = s.surfaceContainerLow,
                surfaceContainer = s.surfaceContainer,
                surfaceContainerHigh = s.surfaceContainerHigh,
                surfaceContainerHighest = s.surfaceContainerHighest,
                secondary = s.secondary,
                onSecondary = s.onSecondary,
                secondaryContainer = s.secondaryContainer,
                onSecondaryContainer = s.onSecondaryContainer,
                tertiary = s.tertiary,
                onTertiary = s.onTertiary,
                tertiaryContainer = s.tertiaryContainer,
                onTertiaryContainer = s.onTertiaryContainer,
            )
        }

    val type: RostoType
        @Composable
        @ReadOnlyComposable
        get() {
            val t = MaterialTheme.typography
            return RostoType(
                display = t.displaySmall,
                title = t.titleLarge,
                heading = t.titleMedium,
                body = t.bodyLarge,
                bodyStrong = t.titleSmall,
                label = t.labelMedium,
                caption = t.labelSmall,
            )
        }
}

@Deprecated("Utiliser MaterialTheme.colorScheme directement")
@Composable
fun AccentScope(accent: Color?, content: @Composable () -> Unit) {
    if (accent == null) {
        content()
        return
    }
    val current = MaterialTheme.colorScheme
    val dark = current.surface.luminance() < 0.5f || isSystemInDarkTheme()
    val updated = remember(current, accent, dark) {
        current.copy(
            primary = accent,
            onPrimary = Palette.onColor(accent),
        )
    }
    MaterialTheme(
        colorScheme = updated,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = {
            CompositionLocalProvider(LocalRostoColors provides Rosto.colors, content = content)
        },
    )
}
