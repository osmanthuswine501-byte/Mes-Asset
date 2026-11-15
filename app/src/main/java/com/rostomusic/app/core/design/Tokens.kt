package com.rostomusic.app.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rostomusic.app.R

/** Every colour used by the app. Screens never hard-code a colour. */
@Immutable
data class RostoColors(
    val background: Color,
    val card: Color,
    val surfaceAlt: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val hairline: Color,
    val accent: Color,
    val onAccent: Color,
    /** Accent shade that stays readable as text on the background. */
    val accentText: Color,
    val danger: Color,
    val vinyl: Color,
    val vinylGroove: Color,
    val isDark: Boolean,
    /** Material 3 Expressive tonal container: selected pills, tonal buttons, the tab indicator. */
    val accentContainer: Color = Color.Transparent,
    val onAccentContainer: Color = Color.Transparent,
    /** 5-level M3 Expressive surface container hierarchy. */
    val surfaceContainerLowest: Color = background,
    val surfaceContainerLow: Color = card,
    val surfaceContainer: Color = card,
    val surfaceContainerHigh: Color = surfaceAlt,
    val surfaceContainerHighest: Color = surfaceAlt,
    /** Distinct secondary & tertiary HCT roles. */
    val secondary: Color = accent,
    val onSecondary: Color = onAccent,
    val secondaryContainer: Color = accentContainer,
    val onSecondaryContainer: Color = onAccentContainer,
    val tertiary: Color = accent,
    val onTertiary: Color = onAccent,
    val tertiaryContainer: Color = accentContainer,
    val onTertiaryContainer: Color = onAccentContainer,
)

@Deprecated("Migré vers MaterialTheme.colorScheme (Dynamic Color)")
object Palette {
    val InkDark = Color(0xFF14151A)
    /** Dark theme text (ui-ux-pro-max music palette, foreground). */
    val InkLight = Color(0xFFF8FAFC)
    /** Play green, the one accent of the app (ui-ux-pro-max music palette, accent). */
    val DefaultAccent = Color(0xFF22C55E)
    val AccentPresets = listOf(
        Color(0xFFFF4A1C), // Orange corail
        Color(0xFF22C55E), // Vert émeraude
        Color(0xFFC2378F), // Magenta
        Color(0xFF3B9EFF), // Bleu azur
        Color(0xFF8B5CF6), // Violet améthyste
        Color(0xFF14B8A6), // Turquoise / Teal
        Color(0xFFF59E0B), // Or ambré
        Color(0xFFF43F5E), // Rose carmin
        Color(0xFF6366F1), // Indigo profond
        Color(0xFF84CC16), // Citron vert
        Color(0xFF06B6D4), // Cyan électrique
        Color(0xFFEC4899), // Rose bonbon
    )

    /** Convertit un [Color] en teinte Material 3 harmonisée (saturation et luminosité maîtrisées). */
    fun m3Tone(seed: Color, lightness: Float, maxSaturation: Float, minSaturation: Float = 0f): Color {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(
            android.graphics.Color.argb(
                255,
                (seed.red * 255f).toInt().coerceIn(0, 255),
                (seed.green * 255f).toInt().coerceIn(0, 255),
                (seed.blue * 255f).toInt().coerceIn(0, 255),
            ),
            hsl,
        )
        hsl[1] = hsl[1].coerceIn(minSaturation, maxSaturation)
        hsl[2] = lightness.coerceIn(0f, 1f)
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }

    fun colors(dark: Boolean, accent: Color): RostoColors {
        // Palette tonale Material 3 complète dérivée d'une seule graine (zéro couleur criarde ou dépareillée)
        val primary = if (dark) m3Tone(accent, 0.78f, 0.72f, 0.40f) else m3Tone(accent, 0.42f, 0.72f, 0.42f)
        val onPrimary = onColor(primary)
        val bg = if (dark) m3Tone(accent, 0.08f, 0.22f) else m3Tone(accent, 0.96f, 0.20f)
        val text = if (dark) Color(0xFFF3F4F8) else Color(0xFF16181D)
        val scLowest = if (dark) m3Tone(accent, 0.05f, 0.20f) else Color(0xFFFFFFFF)
        val scLow = if (dark) m3Tone(accent, 0.11f, 0.22f) else m3Tone(accent, 0.94f, 0.22f)
        val card = if (dark) m3Tone(accent, 0.14f, 0.24f) else m3Tone(accent, 0.92f, 0.24f)
        val scHigh = if (dark) m3Tone(accent, 0.18f, 0.26f) else m3Tone(accent, 0.89f, 0.24f)
        val surfaceAlt = if (dark) m3Tone(accent, 0.22f, 0.28f) else m3Tone(accent, 0.86f, 0.26f)
        val container = if (dark) m3Tone(accent, 0.27f, 0.46f, 0.25f) else m3Tone(accent, 0.86f, 0.48f, 0.28f)
        val onContainer = if (dark) m3Tone(accent, 0.92f, 0.35f) else m3Tone(accent, 0.16f, 0.55f)
        val secHue = shiftHue(accent, 22f, satScale = 0.70f)
        val sec = if (dark) m3Tone(secHue, 0.74f, 0.50f) else m3Tone(secHue, 0.46f, 0.50f)
        val secCont = if (dark) m3Tone(secHue, 0.24f, 0.36f, 0.18f) else m3Tone(secHue, 0.87f, 0.36f, 0.20f)
        val onSecCont = if (dark) m3Tone(secHue, 0.90f, 0.30f) else m3Tone(secHue, 0.18f, 0.45f)
        val tertHue = shiftHue(accent, 55f, satScale = 0.75f)
        val tert = if (dark) m3Tone(tertHue, 0.76f, 0.55f) else m3Tone(tertHue, 0.45f, 0.55f)
        val tertCont = if (dark) m3Tone(tertHue, 0.25f, 0.38f) else m3Tone(tertHue, 0.88f, 0.38f)
        val onTertCont = if (dark) m3Tone(tertHue, 0.90f, 0.30f) else m3Tone(tertHue, 0.18f, 0.45f)

        return RostoColors(
            background = bg,
            card = card,
            surfaceAlt = surfaceAlt,
            textPrimary = text,
            textSecondary = if (dark) m3Tone(accent, 0.74f, 0.14f) else m3Tone(accent, 0.34f, 0.16f),
            textMuted = if (dark) m3Tone(accent, 0.62f, 0.12f) else m3Tone(accent, 0.44f, 0.14f),
            hairline = text.copy(alpha = 0.10f),
            accent = primary,
            onAccent = onPrimary,
            accentText = readableOn(primary, bg),
            danger = if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
            vinyl = if (dark) Color(0xFF0A0A0C) else Color(0xFF17181D),
            vinylGroove = Color(0xFFFFFFFF).copy(alpha = if (dark) 0.07f else 0.09f),
            isDark = dark,
            accentContainer = container,
            onAccentContainer = onContainer,
            surfaceContainerLowest = scLowest,
            surfaceContainerLow = scLow,
            surfaceContainer = card,
            surfaceContainerHigh = scHigh,
            surfaceContainerHighest = surfaceAlt,
            secondary = sec,
            onSecondary = onColor(sec),
            secondaryContainer = secCont,
            onSecondaryContainer = onSecCont,
            tertiary = tert,
            onTertiary = onColor(tert),
            tertiaryContainer = tertCont,
            onTertiaryContainer = onTertCont,
        )
    }

    /** [base] with [tint] laid over it at [amount] opacity. */
    fun tint(base: Color, tint: Color, amount: Float): Color = tint.copy(alpha = amount).compositeOver(base)

    /**
     * Material 3 Expressive colours from the wallpaper (Material You), laid on the app's own roles:
     * a tinted background, lighter cards, tinted pills and the wallpaper's main colour as accent.
     */
    fun dynamic(s: androidx.compose.material3.ColorScheme, dark: Boolean): RostoColors {
        val bg = if (dark) s.surface else s.surfaceContainer
        return RostoColors(
            background = bg,
            card = if (dark) s.surfaceContainer else s.surfaceContainerLowest,
            surfaceAlt = s.secondaryContainer,
            textPrimary = s.onSurface,
            textSecondary = s.onSurfaceVariant,
            textMuted = s.outline,
            hairline = s.onSurface.copy(alpha = 0.08f),
            accent = s.primary,
            onAccent = s.onPrimary,
            accentText = readableOn(s.primary, bg),
            danger = s.error,
            vinyl = if (dark) Color(0xFF0A0A0C) else Color(0xFF17181D),
            vinylGroove = Color(0xFFFFFFFF).copy(alpha = if (dark) 0.07f else 0.09f),
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

    fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return if (la > lb) la / lb else lb / la
    }

    /** Ink or white, whichever reads better on [background]. */
    fun onColor(background: Color): Color =
        if (contrast(InkDark, background) >= contrast(Color.White, background)) InkDark else Color.White

    /** Moves [color] toward black or white until it reaches text contrast on [background]. */
    fun readableOn(color: Color, background: Color, target: Float = 4.5f): Color {
        val toward = if (background.luminance() > 0.5f) Color.Black else Color.White
        var c = color
        var i = 0
        while (contrast(c, background) < target && i < 20) {
            c = Color(
                red = c.red + (toward.red - c.red) * 0.1f,
                green = c.green + (toward.green - c.green) * 0.1f,
                blue = c.blue + (toward.blue - c.blue) * 0.1f,
            )
            i++
        }
        return c
    }

    /** Soft, stable colour for a genre chip or a missing cover. */
    fun hueOf(seed: String): Float = ((seed.lowercase().hashCode() % 360) + 360) % 360f

    fun shiftHue(color: Color, degrees: Float, satScale: Float = 1f): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (color.red * 255f).toInt(),
            (color.green * 255f).toInt(),
            (color.blue * 255f).toInt(),
            hsv
        )
        hsv[0] = (hsv[0] + degrees) % 360f
        hsv[1] = (hsv[1] * satScale).coerceIn(0f, 1f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}

/** Spacing follows a 4dp grid with 8dp as the main step. */
object Space {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x10 = 40.dp
    val x12 = 48.dp
    val screen = 16.dp
    val touch = 48.dp
}

/**
 * Material 3 Expressive corners, larger than before: 12 for pictures and small tiles, 16 for cards, tiles
 * and the playing song, 28 for sheets, dialogs and the bottom dock; fully round for buttons, chips and
 * the tab pill. Buttons and chips also morph between round and 12 while pressed or selected.
 */
object Radius {
    val image = 12.dp
    val tile = 16.dp
    val small = image
    val cover = image
    /** Sheets, dialogs, the bottom dock. */
    val card = 28.dp
    /** Corner of a pressed or selected button and chip. */
    val morph = 12.dp
    /** Only the record label of the vinyl stays round. */
    val disc = 999.dp
}

/** Four cover sizes only. */
object CoverSize {
    /** Queue and dense lists. */
    val s = 40.dp
    /** Song rows and the mini-player. */
    val m = 52.dp
    /** Album list and pins. */
    val l = 64.dp
    /** Rows of covers. */
    val xl = 160.dp
}

/** Shared see-through values, instead of numbers repeated across screens. */
object Alpha {
    /** Background of the playing song. */
    const val NOW_PLAYING = 0.14f
    /** Veil over a picture. */
    const val SCRIM = 0.40f
}

val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
)

private val NoTrim = androidx.compose.ui.text.style.LineHeightStyle(
    androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
    androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
)

@Immutable
@Deprecated("Migré vers MaterialTheme.typography (*Emphasized)")
data class RostoType(
    /** Screen titles. */
    val display: TextStyle = TextStyle(fontFamily = Jakarta, lineHeightStyle = NoTrim, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-0.8).sp),
    val title: TextStyle = TextStyle(fontFamily = Jakarta, lineHeightStyle = NoTrim, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
    val heading: TextStyle = TextStyle(fontFamily = Jakarta, lineHeightStyle = NoTrim, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    val body: TextStyle = TextStyle(fontFamily = Jakarta, lineHeightStyle = NoTrim, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    val bodyStrong: TextStyle = TextStyle(fontFamily = Jakarta, lineHeightStyle = NoTrim, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    // Figures keep a fixed width ("tnum") so times and counters never shift while they change.
    val label: TextStyle = TextStyle(fontFamily = Jakarta, lineHeightStyle = NoTrim, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 20.sp, fontFeatureSettings = "tnum"),
    val caption: TextStyle = TextStyle(fontFamily = Jakarta, lineHeightStyle = NoTrim, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum"),
)

val LocalRostoColors = staticCompositionLocalOf { Palette.colors(false, Palette.DefaultAccent) }
val LocalRostoType = staticCompositionLocalOf { RostoType() }
val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }
