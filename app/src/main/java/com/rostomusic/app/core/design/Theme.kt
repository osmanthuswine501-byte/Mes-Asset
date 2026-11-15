package com.rostomusic.app.core.design

import android.os.Build
import android.provider.Settings.Global
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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

/**
 * Formes standard Material 3 Expressive (system/shape.md) :
 * - ExtraSmall : 4.dp (Snackbars, TextFields)
 * - Small      : 8.dp (Chips, Menus)
 * - Medium     : 12.dp (Cards)
 * - Large      : 16.dp (FABs, Side sheets)
 * - ExtraLarge : 28.dp (Dialogs, Bottom sheets / modales)
 */
val RostoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Typographie Material 3 Expressive avec les 15 styles Emphasized
 * (system/typography-expressive.md) pour les titres et moments Hero.
 */
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

    // 15 styles Emphasized M3 Expressive
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

/**
 * Thème Material 3 Expressive pur :
 * - Aucune couleur hexadécimale en dur
 * - Dynamic Color activé (Android 12+) avec repli sur lightColorScheme / darkColorScheme
 * - MotionScheme.expressive() pour les transitions spring()
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RostoTheme(
    settings: Settings,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = remember(darkTheme, dynamicColor, context) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    }
    val reduceMotion = remember(context) {
        runCatching {
            Global.getFloat(context.contentResolver, Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
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
                LocalIndication provides PressIndication,
                content = content,
            )
        }
    }
}
