package com.rostomusic.app.core.design

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.LocalIndication
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
        val request = ImageRequest.Builder(context).data(model).allowHardware(false).size(160).build()
        val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        bitmap?.let(::edgeColor)
    }?.also { ToneCache[model] = it }
}

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
    val swatch = palette.dominantSwatch ?: palette.vibrantSwatch ?: palette.mutedSwatch ?: return@runCatching null
    Color(swatch.rgb)
}.getOrNull()

fun toneColors(base: RostoColors, bg: Color): RostoColors {
    val dark = bg.luminance() < 0.4f
    val ink = if (dark) Color.White else Palette.InkDark
    return base.copy(
        background = bg,
        card = ink.copy(alpha = 0.08f).compositeOver(bg),
        surfaceAlt = ink.copy(alpha = 0.14f).compositeOver(bg),
        surfaceContainerLowest = ink.copy(alpha = 0.04f).compositeOver(bg),
        surfaceContainerLow = ink.copy(alpha = 0.07f).compositeOver(bg),
        surfaceContainer = ink.copy(alpha = 0.10f).compositeOver(bg),
        surfaceContainerHigh = ink.copy(alpha = 0.14f).compositeOver(bg),
        surfaceContainerHighest = ink.copy(alpha = 0.18f).compositeOver(bg),
        textPrimary = ink,
        textSecondary = ink.copy(alpha = 0.78f).compositeOver(bg),
        textMuted = ink.copy(alpha = 0.72f).compositeOver(bg),
        hairline = ink.copy(alpha = 0.10f),
        accentText = Palette.readableOn(base.accent, bg),
        isDark = dark,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToneScope(bg: Color, content: @Composable () -> Unit) {
    val base = Rosto.colors
    val colors = remember(base, bg) { toneColors(base, bg) }
    val baseScheme = MaterialTheme.colorScheme
    val dark = bg.luminance() < 0.4f
    val ink = if (dark) Color.White else Palette.InkDark
    val tonedScheme = remember(baseScheme, bg, dark) {
        baseScheme.copy(
            background = bg,
            onBackground = ink,
            surface = bg,
            onSurface = ink,
            onSurfaceVariant = ink.copy(alpha = 0.76f).compositeOver(bg),
            outline = ink.copy(alpha = 0.65f).compositeOver(bg),
            outlineVariant = ink.copy(alpha = 0.14f).compositeOver(bg),
            surfaceContainerLowest = ink.copy(alpha = 0.05f).compositeOver(bg),
            surfaceContainerLow = ink.copy(alpha = 0.08f).compositeOver(bg),
            surfaceContainer = ink.copy(alpha = 0.11f).compositeOver(bg),
            surfaceContainerHigh = ink.copy(alpha = 0.15f).compositeOver(bg),
            surfaceContainerHighest = ink.copy(alpha = 0.20f).compositeOver(bg),
        )
    }
    CompositionLocalProvider(LocalRostoColors provides colors) {
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
