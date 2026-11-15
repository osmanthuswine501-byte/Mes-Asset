package com.rostomusic.app.core.design

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette as ColorPalette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The colour of a picture's lower edge, where it fades into the page (Apple Music style).
 * Returns [fallback] while loading or when there is no picture.
 */
@Composable
fun rememberCoverTone(model: Any?, fallback: Color, alternative: Any? = null): Color {
    val context = LocalContext.current
    // A page seen before opens straight in its colour; only the first visit fades in (400 ms).
    val cached = model?.let { ToneCache[it] } ?: alternative?.let { ToneCache[it] }
    val tone by produceState(cached, model) {
        if (cached != null || model == null) return@produceState
        // [alternative] is tried when [model] has no picture (an album without Android album art).
        value = measureCoverTone(context, model) ?: alternative?.let { measureCoverTone(context, it) }
    }
    if (cached != null) return cached
    val reduce = LocalReduceMotion.current
    val animated by animateColorAsState(tone ?: fallback, if (reduce) snap() else MaterialTheme.motionScheme.slowEffectsSpec(), label = "tone")
    return animated
}

/**
 * Measures and remembers the edge colour of [model] ahead of time, so the page that shows it opens
 * straight in its colour. The small 160 px picture stays in memory and serves as the banner's
 * instant placeholder.
 */
suspend fun measureCoverTone(context: android.content.Context, model: Any): Color? {
    ToneCache[model]?.let { return it }
    return withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context).data(model).allowHardware(false).size(160).build()
        val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        bitmap?.let(::edgeColor)
    }?.also { ToneCache[model] = it }
}

/** Edge colours already measured, by picture, for the life of the process. */
private object ToneCache {
    private val map = object : LinkedHashMap<Any, Color>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, Color>?) = size > 200
    }
    @Synchronized operator fun get(key: Any): Color? = map[key]
    @Synchronized operator fun set(key: Any, value: Color) { map[key] = value }
}

private fun edgeColor(bitmap: Bitmap): Color? = runCatching {
    val h = bitmap.height
    val palette = ColorPalette.from(bitmap).setRegion(0, h * 3 / 4, bitmap.width, h).clearFilters().generate()
    val swatch = palette.dominantSwatch ?: return@runCatching null
    Color(swatch.rgb)
}.getOrNull()

/** The app colours laid over a page of colour [bg]: text in black or white, pills and lines tinted from it. */
fun toneColors(base: RostoColors, bg: Color): RostoColors {
    val dark = bg.luminance() < 0.4f
    val ink = if (dark) Color.White else Palette.InkDark
    return base.copy(
        background = bg,
        card = ink.copy(alpha = 0.08f).compositeOver(bg),
        surfaceAlt = ink.copy(alpha = 0.12f).compositeOver(bg),
        textPrimary = ink,
        textSecondary = ink.copy(alpha = 0.78f).compositeOver(bg),
        textMuted = ink.copy(alpha = 0.72f).compositeOver(bg),
        hairline = ink.copy(alpha = 0.10f),
        accentText = Palette.readableOn(base.accent, bg),
        isDark = dark,
    )
}

/** Draws [content] on a page tinted by [bg]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToneScope(bg: Color, content: @Composable () -> Unit) {
    val base = LocalRostoColors.current
    val colors = remember(base, bg) { toneColors(base, bg) }
    CompositionLocalProvider(LocalRostoColors provides colors) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme, motionScheme = MaterialTheme.motionScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, ) { CompositionLocalProvider(androidx.compose.foundation.LocalIndication provides PressIndication, content = content) }
    }
}
