package com.rostomusic.app.core.design

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette as ColorPalette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberCoverTone(model: Any?, fallback: Color, alternative: Any? = null): Color {
    val context = LocalContext.current
    val cached = model?.let { ToneCache[it] } ?: alternative?.let { ToneCache[it] }
    val tone by produceState(cached, model, alternative) {
        if (cached != null) {
            value = cached
            return@produceState
        }
        if (model == null) return@produceState
        value = measureCoverTone(context, model) ?: alternative?.let { measureCoverTone(context, it) }
    }
    val reduce = LocalReduceMotion.current
    val animated by animateColorAsState(
        targetValue = tone ?: fallback,
        animationSpec = if (reduce) snap() else MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "coverTone",
    )
    return animated
}

suspend fun measureCoverTone(context: android.content.Context, model: Any): Color? {
    ToneCache[model]?.let { return it }
    return withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context).data(model).allowHardware(false).size(128).build()
        val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        bitmap?.let(::harmonizedSeed)
    }?.also { ToneCache[model] = it }
}

private object ToneCache {
    private val map = object : LinkedHashMap<Any, Color>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, Color>?) = size > 200
    }
    @Synchronized operator fun get(key: Any): Color? = map[key]
    @Synchronized operator fun set(key: Any, value: Color) { map[key] = value }
}

private fun harmonizedSeed(bitmap: Bitmap): Color? = runCatching {
    val palette = ColorPalette.from(bitmap).clearFilters().generate()
    val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch ?: return@runCatching null
    Color(swatch.rgb)
}.getOrNull()

fun toneColors(base: RostoColors, bg: Color): RostoColors {
    val dark = bg.luminance() < 0.4f
    return Palette.colors(dark, bg)
}

/**
 * Applique une palette Material 3 complète, cohérente et animée à partir de la couleur graine [bg]
 * (aucune couleur aléatoire ou désaccordée entre le fond, les cartes et les boutons).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToneScope(bg: Color, content: @Composable () -> Unit) {
    val baseScheme = MaterialTheme.colorScheme
    val dark = baseScheme.surface.luminance() < 0.4f || isSystemInDarkTheme()
    val reduce = LocalReduceMotion.current
    val spec = if (reduce) snap<Color>() else MaterialTheme.motionScheme.slowEffectsSpec()
    val animatedSeed by animateColorAsState(bg, spec, label = "toneSeed")
    val pal = remember(dark, animatedSeed) { Palette.colors(dark, animatedSeed) }
    val tonedScheme = remember(baseScheme, pal) {
        baseScheme.copy(
            primary = pal.accent,
            onPrimary = pal.onAccent,
            primaryContainer = pal.accentContainer,
            onPrimaryContainer = pal.onAccentContainer,
            secondary = pal.secondary,
            onSecondary = pal.onSecondary,
            secondaryContainer = pal.secondaryContainer,
            onSecondaryContainer = pal.onSecondaryContainer,
            tertiary = pal.tertiary,
            onTertiary = pal.onTertiary,
            tertiaryContainer = pal.tertiaryContainer,
            onTertiaryContainer = pal.onTertiaryContainer,
            background = pal.background,
            onBackground = pal.textPrimary,
            surface = pal.card,
            onSurface = pal.textPrimary,
            onSurfaceVariant = pal.textSecondary,
            outline = pal.textMuted,
            outlineVariant = pal.hairline,
            surfaceContainerLowest = pal.surfaceContainerLowest,
            surfaceContainerLow = pal.surfaceContainerLow,
            surfaceContainer = pal.surfaceContainer,
            surfaceContainerHigh = pal.surfaceContainerHigh,
            surfaceContainerHighest = pal.surfaceContainerHighest,
        )
    }
    CompositionLocalProvider(LocalRostoColors provides pal) {
        MaterialTheme(
            colorScheme = tonedScheme,
            motionScheme = MaterialTheme.motionScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
        ) {
            CompositionLocalProvider(LocalIndication provides PressIndication, content = content)
        }
    }
}
