package com.rostomusic.app.core.design

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.rostomusic.app.R
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.db.SongQuality
import com.rostomusic.app.data.settings.RowSubtitle
import com.rostomusic.app.domain.AudioQuality
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 22.dp,
) {
    Icon(painterResource(id), contentDescription, modifier.size(size), tint)
}

/** Bouton icône rond avec cible tactile minimale de 48 dp et sans double annonce TalkBack. */
@Composable
fun IconBtn(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    container: Color = Color.Transparent,
    size: Dp = Space.touch,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
) {
    val press = remember { MutableInteractionSource() }
    val corner = morphCorner(press, rest = size / 2)
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .pressScale(press)
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(container)
            .clickable(press, LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        RIcon(icon, null, tint = if (enabled) tint else tint.copy(alpha = 0.4f), size = iconSize)
    }
}

/** Retourne une fonction déclenchant un retour haptique si activé. */
@Composable
fun rememberHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(haptic, enabled) { { if (enabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }
}

@Composable
fun CoverArt(albumId: Long, seed: String, modifier: Modifier = Modifier, radius: Dp = Radius.cover, banner: Boolean = false) {
    CoverBox(seed, modifier, radius, placeholderIcon = !banner) {
        if (albumId != -1L) {
            var model by remember(albumId) { mutableStateOf(Song.albumArtUri(albumId)) }
            val onError = { _: AsyncImagePainter.State.Error ->
                if (albumId >= 0 && model.scheme != "rostoart") model = Song.embeddedArtUri(albumId)
            }
            if (banner) {
                FadeInPicture(model, onError, Modifier.fillMaxSize())
            } else {
                AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, onError = onError, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun CoverArt(song: Song, modifier: Modifier = Modifier, radius: Dp = Radius.cover, banner: Boolean = false) {
    CoverBox(song.album.ifEmpty { song.title }, modifier, radius, placeholderIcon = !banner) {
        if (banner) BannerImage(song.artUri, Modifier.fillMaxSize())
        else AsyncImage(model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun BannerImage(model: Any, modifier: Modifier = Modifier) = FadeInPicture(model, null, modifier)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FadeInPicture(model: Any, onError: ((AsyncImagePainter.State.Error) -> Unit)?, modifier: Modifier) {
    val context = LocalContext.current
    val request = remember(model) { ImageRequest.Builder(context).data(model).crossfade(false).build() }
    var ready by remember(model) { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (ready) 1f else 0f,
        animationSpec = if (LocalReduceMotion.current) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "bannerFade",
    )
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { ready = true },
        onError = { onError?.invoke(it) },
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    )
}

@Composable
private fun CoverBox(seed: String, modifier: Modifier, requested: Dp, placeholderIcon: Boolean = true, image: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val unused = seed
    val radius = when (requested) { 0.dp -> 0.dp; Radius.disc -> 500.dp; else -> Radius.image }
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (placeholderIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_music),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.32f),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
        image()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayMediumEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { heading() },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onAddAll: (() -> Unit)? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(start = Space.screen, end = Space.x2).heightIn(min = Space.touch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (onAddAll != null) {
            IconBtn(R.drawable.ic_list_plus, stringResource(R.string.add_all_to_queue), onAddAll, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onSeeAll != null) {
            IconBtn(R.drawable.ic_chevron_right, stringResource(R.string.see_all), onSeeAll, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Pill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    container: Color? = null,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else container ?: MaterialTheme.colorScheme.surfaceContainer
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val press = remember { MutableInteractionSource() }
    val corner = morphCorner(press, selected = selected)
    Box(
        modifier
            .pressScale(press)
            .heightIn(min = Space.touch)
            .clickable(press, null, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.height(40.dp).clip(RoundedCornerShape(corner)).background(bg).padding(horizontal = Space.x4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.x2),
        ) {
            if (icon != null) RIcon(icon, null, tint = fg, size = 18.dp)
            Text(
                text = text,
                style = if (selected) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, hueSeed: String? = null) {
    @Suppress("UNUSED_VARIABLE") val unused = hueSeed
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Space.x2 + 2.dp, vertical = Space.x1),
    )
}

/** Bouton d'action primaire haute emphase (1 seul par écran). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier
            .height(56.dp)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (icon != null) {
            RIcon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, size = 20.dp)
            Spacer(Modifier.width(Space.x2))
        }
        Text(text, style = MaterialTheme.typography.labelLargeEmphasized, maxLines = 1)
    }
}

/** Bouton secondaire moyenne emphase (Tonal). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    val contentColor = if (container == MaterialTheme.colorScheme.secondaryContainer) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier
            .height(56.dp)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = container,
            contentColor = contentColor,
        ),
    ) {
        if (icon != null) {
            RIcon(icon, null, tint = contentColor, size = 20.dp)
            Spacer(Modifier.width(Space.x2))
        }
        Text(text, style = MaterialTheme.typography.labelLargeEmphasized, maxLines = 1)
    }
}

/**
 * Carte Bento M3 Expressive (Medium = 12.dp).
 * Règle 1 : ne JAMAIS passer de [onClick] si la carte contient un Slider, Switch, Checkbox ou TextField.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val press = remember { MutableInteractionSource() }
    val shape = if (onClick != null) {
        RoundedCornerShape(morphCorner(press, rest = 12.dp, pressed = 20.dp))
    } else {
        MaterialTheme.shapes.medium
    }
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressScale(press) else Modifier)
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(press, LocalIndication.current, role = Role.Button, onClick = onClick) else Modifier),
        content = content,
    )
}

/**
 * TopAppBar M3 Expressive utilisant nativement [MediumFlexibleTopAppBar]
 * avec typographie [headlineMediumEmphasized] (max 2 lignes).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMediumEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    RIcon(R.drawable.ic_chevron_left, stringResource(R.string.back))
                }
            }
        },
        actions = actions,
    )
}

@Composable
fun EqualizerBars(playing: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val animate = playing && !LocalReduceMotion.current
    val heights = if (animate) {
        val t = rememberInfiniteTransition(label = "eq")
        listOf(420, 560, 340).map { d ->
            // Correction M3 : Remplacement de LinearEasing par EaseOutStrong (Motion Expressive)
            val v by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(d, easing = EaseOutStrong), RepeatMode.Reverse), label = "bar")
            v
        }
    } else listOf(0.45f, 0.8f, 0.6f)
    Row(modifier.size(18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        heights.forEach { h ->
            Box(Modifier.weight(1f).fillMaxHeight(h).clip(CircleShape).background(color))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongRow(
    song: Song,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    number: Int? = null,
    onAdd: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    val highlight = isCurrent
    val press = remember { MutableInteractionSource() }
    val rowCorner = morphCorner(press, rest = if (highlight) 16.dp else 4.dp, pressed = 16.dp, selected = highlight)
    // Correction M3 : Suppression du clickable global sur la Row principale pour ne pas englober les IconBtn
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = if (highlight) Space.x2 else 0.dp)
            .clip(RoundedCornerShape(rowCorner))
            .then(if (highlight) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
            .padding(start = if (highlight) Space.x3 else Space.screen, end = Space.x2)
            .heightIn(min = 68.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Correction M3 : Clic isolé uniquement sur la zone de gauche (CoverArt/numéro + textes)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(rowCorner))
                .clickable(press, LocalIndication.current, role = Role.Button, onClick = onClick)
                .padding(vertical = Space.x2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (number != null) {
                Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
                    if (isCurrent) EqualizerBars(isPlaying)
                    else Text(
                        number.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                CoverArt(song, Modifier.size(CoverSize.m), Radius.small)
                Spacer(Modifier.width(Space.x3))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (LocalSongQuality.current[song.id]?.explicit == true) {
                        Spacer(Modifier.width(6.dp))
                        ExplicitBadge()
                    }
                    if (number == null) {
                        Spacer(Modifier.width(6.dp))
                        QualityTag(songQuality(song))
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isCurrent && number == null) {
            EqualizerBars(isPlaying, Modifier.padding(horizontal = Space.x2))
        }
        if (onAdd != null) IconBtn(R.drawable.ic_plus, stringResource(R.string.add_to_queue), onAdd, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onMore != null) IconBtn(R.drawable.ic_ellipsis, stringResource(R.string.more_options), onMore, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        else Spacer(Modifier.width(Space.x3))
    }
}

@Composable
fun songSubtitle(song: Song, mode: RowSubtitle): String {
    val artist = song.artist.ifEmpty { stringResource(R.string.unknown_artist) }
    return when (mode) {
        RowSubtitle.ARTIST -> artist
        RowSubtitle.ARTIST_DURATION -> "$artist · ${formatDuration(song.duration)}"
        RowSubtitle.ARTIST_ALBUM -> if (song.album.isEmpty()) artist else "$artist · ${song.album}"
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Space.x8, vertical = Space.x10),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).clip(cookie9()).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            RIcon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, size = 32.dp)
        }
        Spacer(Modifier.height(Space.x4))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.x1))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.x5))
            PrimaryButton(actionLabel, onAction)
        }
    }
}

@Composable
fun SkeletonRows(count: Int = 8, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().padding(vertical = Space.x3), contentAlignment = Alignment.Center) {
            RostoLoading()
        }
        repeat(count) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.screen).height(68.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainer))
                Spacer(Modifier.width(Space.x3))
                Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                    Box(Modifier.width(180.dp).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer))
                    Box(Modifier.width(110.dp).height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLow))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RostoSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = Alpha.SCRIM),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SheetItem(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    trailing: String? = null,
) {
    val press = remember { MutableInteractionSource() }
    val corner = morphCorner(press, rest = 12.dp, pressed = 20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x3, vertical = 2.dp)
            .clip(RoundedCornerShape(corner))
            .clickable(press, LocalIndication.current, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = Space.x3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconPill(icon, size = 40.dp, tint = tint)
        Spacer(Modifier.width(Space.x4))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLargeEmphasized,
            color = tint,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RostoAlert(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    text: String? = null,
    dismissLabel: String? = stringResource(R.string.cancel),
    destructive: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.headlineSmallEmphasized) },
        text = content ?: text?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = dismissLabel?.let {
            {
                TextButton(onClick = onDismiss) {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    )
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * Slider M3 Expressive sans sizeIn/heightIn forcé (le Slider gère nativement sa cible tactile).
 * Inclut [stateDescriptionText] pour l'annonce A11Y TalkBack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RostoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    stateDescriptionText: String? = null,
) {
    val active = if (enabled) accent else MaterialTheme.colorScheme.outline
    val inactive = MaterialTheme.colorScheme.surfaceContainerHigh
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = if (stateDescriptionText != null) {
            modifier.semantics { stateDescription = stateDescriptionText }
        } else {
            modifier
        },
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        valueRange = valueRange,
        thumb = { Box(Modifier.width(4.dp).height(44.dp).clip(CircleShape).background(active)) },
        track = { state ->
            val span = (state.valueRange.endInclusive - state.valueRange.start).takeIf { it > 0f } ?: 1f
            val fraction = ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                val h = size.height
                val gap = 10.dp.toPx()
                val x = size.width * fraction
                val r = CornerRadius(h / 2, h / 2)
                val inner = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                val activeEnd = (x - gap).coerceAtLeast(0f)
                val inactiveStart = (x + gap).coerceAtMost(size.width)
                if (activeEnd > 0f) drawPath(
                    Path().apply {
                        addRoundRect(RoundRect(0f, 0f, activeEnd, h, r, inner, inner, r))
                    },
                    active,
                )
                if (inactiveStart < size.width) drawPath(
                    Path().apply {
                        addRoundRect(RoundRect(inactiveStart, 0f, size.width, h, inner, r, r, inner))
                    },
                    inactive,
                )
            }
        },
    )
}

val LocalSongQuality = staticCompositionLocalOf<Map<Long, SongQuality>> { emptyMap() }

private val ADDED_FORMAT = SimpleDateFormat("d MMM yyyy", Locale.FRANCE)

@Composable
fun songQuality(song: Song): String {
    val q = LocalSongQuality.current[song.id]
    return remember(song, q) { AudioQuality.label(song.path, q?.codecMime.orEmpty(), q?.bitDepth ?: 0, song.bitrate) }
}

@Composable
fun addedDate(song: Song): String = remember(song.dateAdded) { if (song.dateAdded > 0) ADDED_FORMAT.format(Date(song.dateAdded)) else "" }

@Composable
fun QualityTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Space.x2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, container: Color? = null, content: Color? = null) {
    Box(
        modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(container ?: MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = Space.x3),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content ?: MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DolbyBadge(label: String, modifier: Modifier = Modifier, container: Color? = null, small: Boolean = false) {
    val rest = label.removePrefix("Dolby").trim()
    val bg = container ?: if (small) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
    Row(
        modifier
            .height(if (small) 24.dp else 32.dp)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = if (small) Space.x2 + 2.dp else Space.x3)
            .semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(R.drawable.ic_dolby), null, Modifier.width(18.dp).height(12.dp), MaterialTheme.colorScheme.onSurface)
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface))
                append("Dolby")
                pop()
                if (rest.isNotEmpty()) append(" $rest")
            },
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

data class NowPlaying(val songId: Long? = null, val playing: Boolean = false)
val LocalNowPlaying = compositionLocalOf { NowPlaying() }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExplicitBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(18.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
            .semantics { contentDescription = "Explicite" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "E",
            style = MaterialTheme.typography.labelSmallEmphasized,
            color = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
fun NowPlayingOverlay(playing: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.scrim.copy(alpha = Alpha.SCRIM)), contentAlignment = Alignment.Center) {
        EqualizerBars(playing, color = MaterialTheme.colorScheme.onPrimary)
    }
}

class StaggerState(seen: Boolean) {
    var done = seen
}

@Composable
fun rememberStagger(): StaggerState {
    var seen by rememberSaveable { mutableStateOf(false) }
    val state = remember { StaggerState(seen) }
    LaunchedEffect(Unit) {
        delay(600)
        state.done = true
        seen = true
    }
    return state
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val androidx.compose.material3.MotionScheme.defaultSpatialDamping: Float get() = 0.8f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Modifier.stagger(state: StaggerState, index: Int): Modifier = composed {
    if (state.done || index >= 10) return@composed this
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(0f) }
    val damping = MaterialTheme.motionScheme.defaultSpatialDamping
    LaunchedEffect(Unit) {
        delay(index * 30L)
        progress.animateTo(1f, spring(dampingRatio = damping))
    }
    this.graphicsLayer {
        alpha = progress.value
        if (!reduce) translationY = (1f - progress.value) * 8.dp.toPx()
    }
}

val EaseOutStrong = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun cookie9(): Shape = MaterialShapes.Cookie9Sided.toShape()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RostoLoading(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
}

@Composable
fun RostoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        thumbContent = if (checked) ({
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
        }) else null,
    )
}

@Composable
fun IconPill(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.36f))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val resolvedTint = if (tint == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else tint
        RIcon(icon, null, tint = resolvedTint, size = size * 0.55f)
    }
}

@Composable
fun RostoGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}