package com.rostomusic.app.core.design

import android.os.Build
import android.provider.Settings.Global
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rostomusic.app.data.settings.Settings
import com.rostomusic.app.data.settings.ThemeMode

private val ExpressiveLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val RostoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val RostoTypography = Typography(
    displayLarge = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontFeatureSettings = "tnum"),
    labelMedium = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontFeatureSettings = "tnum"),
    labelSmall = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontFeatureSettings = "tnum"),

    displayLargeEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMediumEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmallEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLargeEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMediumEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmallEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLargeEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMediumEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmallEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLargeEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMediumEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmallEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLargeEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontFeatureSettings = "tnum"),
    labelMediumEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontFeatureSettings = "tnum"),
    labelSmallEmphasized = TextStyle(fontFamily = Jakarta, lineHeightStyle = ExpressiveLineHeightStyle, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontFeatureSettings = "tnum"),
)

private fun seedColorScheme(dark: Boolean, seed: Color): ColorScheme {
    val c = Palette.colors(dark, seed)
    return if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentContainer, onPrimaryContainer = c.onAccentContainer,
            secondary = c.secondary, onSecondary = c.onSecondary, secondaryContainer = c.secondaryContainer, onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.tertiary, onTertiary = c.onTertiary, tertiaryContainer = c.tertiaryContainer, onTertiaryContainer = c.onTertiaryContainer,
            background = c.background, onBackground = c.textPrimary, surface = c.card, onSurface = c.textPrimary,
            surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.textSecondary, outline = c.textMuted, outlineVariant = c.hairline,
            surfaceContainerLowest = c.surfaceContainerLowest, surfaceContainerLow = c.surfaceContainerLow,
            surfaceContainer = c.surfaceContainer, surfaceContainerHigh = c.surfaceContainerHigh, surfaceContainerHighest = c.surfaceContainerHighest,
            error = c.danger,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentContainer, onPrimaryContainer = c.onAccentContainer,
            secondary = c.secondary, onSecondary = c.onSecondary, secondaryContainer = c.secondaryContainer, onSecondaryContainer = c.onSecondaryContainer,
            tertiary = c.tertiary, onTertiary = c.onTertiary, tertiaryContainer = c.tertiaryContainer, onTertiaryContainer = c.onTertiaryContainer,
            background = c.background, onBackground = c.textPrimary, surface = c.card, onSurface = c.textPrimary,
            surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.textSecondary, outline = c.textMuted, outlineVariant = c.hairline,
            surfaceContainerLowest = c.surfaceContainerLowest, surfaceContainerLow = c.surfaceContainerLow,
            surfaceContainer = c.surfaceContainer, surfaceContainerHigh = c.surfaceContainerHigh, surfaceContainerHighest = c.surfaceContainerHighest,
            error = c.danger,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RostoTheme(
    settings: Settings,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        runCatching {
            Global.getFloat(context.contentResolver, Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val useDynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val rawAccent = Color(settings.accent)
    val animatedAccent by animateColorAsState(
        targetValue = rawAccent,
        animationSpec = if (reduceMotion) snap() else MotionScheme.expressive().defaultEffectsSpec(),
        label = "themeSeedAccent",
    )

    val colorScheme = remember(darkTheme, useDynamic, animatedAccent, context) {
        if (useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            seedColorScheme(darkTheme, animatedAccent)
        }
    }

    CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion,
        LocalHapticsEnabled provides settings.haptics,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = RostoShapes,
            typography = RostoTypography,
        ) {
            CompositionLocalProvider(
                LocalRostoColors provides Rosto.colors,
                LocalRostoType provides Rosto.type,
                LocalIndication provides PressIndication,
                content = content,
            )
        }
    }
}
