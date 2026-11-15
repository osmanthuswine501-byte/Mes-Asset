package com.rostomusic.app.ui.player

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rostomusic.app.R
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.playback.AudioOutput
import com.rostomusic.app.ui.LocalApp

/**
 * Where the sound goes: this phone, wired or Bluetooth headphones and speakers. The active device
 * doubles as the volume slider, as on iOS, in the app's own shapes.
 */
@Composable
fun OutputSheet(song: Song, onDismiss: () -> Unit) {
    val tone by LocalApp.current.playerTone.collectAsStateWithLifecycle()
    // Same tint as the player behind it.
    if (tone != null) com.rostomusic.app.core.design.ToneScope(tone!!) { OutputSheetContent(song, onDismiss) } else OutputSheetContent(song, onDismiss)
}

@Composable
private fun OutputSheetContent(song: Song, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val c = Rosto.colors
    val context = LocalContext.current
    val route = app.audioRoute
    val outputs by route.outputs.collectAsStateWithLifecycle()
    val preferred by route.preferred.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        route.refresh()
        onDispose { }
    }
    val active = remember(outputs, preferred) { route.active(outputs) }
    var volume by remember { mutableFloatStateOf(route.volume) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(c.card).padding(Space.x4),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.padding(start = Space.x1, top = Space.x1, bottom = Space.x2), verticalAlignment = Alignment.CenterVertically) {
                CoverArt(song, Modifier.size(com.rostomusic.app.core.design.CoverSize.m), Radius.small)
                Spacer(Modifier.width(Space.x3))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist.ifEmpty { stringResource(R.string.unknown_artist) }, style = Rosto.type.label, color = c.textSecondary, maxLines = 1)
                }
            }
            for (o in outputs) {
                val name = when (o.kind) {
                    AudioOutput.Kind.PHONE -> stringResource(R.string.output_phone)
                    AudioOutput.Kind.WIRED -> o.name.ifEmpty { stringResource(R.string.output_wired) }
                    AudioOutput.Kind.USB -> o.name.ifEmpty { stringResource(R.string.output_usb) }
                    AudioOutput.Kind.BLUETOOTH -> o.name.ifEmpty { stringResource(R.string.output_bluetooth) }
                }
                val icon = when (o.kind) {
                    AudioOutput.Kind.PHONE -> R.drawable.ic_smartphone
                    AudioOutput.Kind.BLUETOOTH -> R.drawable.ic_headphones
                    else -> R.drawable.ic_headphones
                }
                if (o.id == active?.id) {
                    val label = stringResource(R.string.output_volume, name)
                    Box(
                        Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(Radius.cover)).background(c.surfaceAlt)
                            .pointerInput(Unit) {
                                detectTapGestures { volume = (it.x / size.width).coerceIn(0f, 1f); route.volume = volume }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    volume = (change.position.x / size.width).coerceIn(0f, 1f)
                                    route.volume = volume
                                }
                            }
                            .semantics {
                                contentDescription = label
                                progressBarRangeInfo = ProgressBarRangeInfo(volume, 0f..1f)
                                setProgress { v -> volume = v; route.volume = v; true }
                            },
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(volume).background(c.accent.copy(alpha = 0.35f)))
                        OutputRow(icon, name, selected = true)
                    }
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(Radius.cover)).background(c.background)
                            .clickable(role = Role.Button) { route.select(o) },
                    ) { OutputRow(icon, name, selected = false) }
                }
            }
            Box(
                Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(Radius.cover)).background(c.background)
                    .clickable(role = Role.Button) {
                        runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    },
            ) { OutputRow(R.drawable.ic_plus, stringResource(R.string.output_pair), selected = false) }
            Text(
                stringResource(R.string.output_hint), style = Rosto.type.caption, color = c.textMuted,
                modifier = Modifier.padding(horizontal = Space.x1),
            )
        }
    }
}

@Composable
private fun OutputRow(icon: Int, name: String, selected: Boolean) {
    val c = Rosto.colors
    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = Space.x4), verticalAlignment = Alignment.CenterVertically) {
        RIcon(icon, null, tint = c.textPrimary, size = 22.dp)
        Spacer(Modifier.width(Space.x3))
        Text(name, style = if (selected) Rosto.type.bodyStrong else Rosto.type.body, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (selected) RIcon(R.drawable.ic_check, null, tint = c.textPrimary, size = 22.dp)
    }
}
