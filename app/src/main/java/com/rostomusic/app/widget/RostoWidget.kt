package com.rostomusic.app.widget

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.rostomusic.app.MainActivity
import com.rostomusic.app.R
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private val KEY_TITLE = stringPreferencesKey("title")
private val KEY_ARTIST = stringPreferencesKey("artist")
private val KEY_PLAYING = booleanPreferencesKey("playing")
private val KEY_ART = stringPreferencesKey("art")

class RostoWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val art = prefs[KEY_ART].orEmpty()
            val cover = if (art.isNotEmpty()) loadCover(context, android.net.Uri.parse(art)) else null
            Content(
                title = prefs[KEY_TITLE].orEmpty().ifEmpty { context.getString(R.string.widget_idle) },
                artist = prefs[KEY_ARTIST].orEmpty(),
                playing = prefs[KEY_PLAYING] ?: false,
                cover = cover,
            )
        }
    }

    /** The playing song's own cover. */
    private fun loadCover(context: Context, art: android.net.Uri): Bitmap? = runCatching {
        val bytes = com.rostomusic.app.core.art.EmbeddedArtFetcher.songPicture(context, art) ?: return@runCatching null
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }.getOrNull()

    @Composable
    private fun Content(title: String, artist: String, playing: Boolean, cover: Bitmap?) {
        val bg = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1C1C20))
        val primary = ColorProvider(day = Color(0xFF14151A), night = Color(0xFFF3F3F5))
        val secondary = ColorProvider(day = Color(0xFF5C6070), night = Color(0xFFA7A9B4))
        Row(
            modifier = GlanceModifier.fillMaxSize().background(bg).cornerRadius(24.dp).padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (cover != null) {
                Image(ImageProvider(cover), contentDescription = null, modifier = GlanceModifier.size(56.dp).cornerRadius(16.dp))
            } else {
                Image(
                    ImageProvider(R.drawable.ic_disc_3), contentDescription = null,
                    modifier = GlanceModifier.size(56.dp).padding(12.dp), colorFilter = ColorFilter.tint(secondary),
                )
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(title, maxLines = 1, style = TextStyle(color = primary, fontSize = 15.sp, fontWeight = FontWeight.Medium))
                if (artist.isNotEmpty()) Text(artist, maxLines = 1, style = TextStyle(color = secondary, fontSize = 13.sp))
            }
            Control(R.drawable.ic_skip_back_filled, R.string.cd_previous, primary, actionRunCallback<PreviousAction>())
            Control(
                if (playing) R.drawable.ic_pause_filled else R.drawable.ic_play_filled,
                if (playing) R.string.cd_pause else R.string.cd_play, primary, actionRunCallback<ToggleAction>(),
            )
            Control(R.drawable.ic_skip_forward_filled, R.string.cd_next, primary, actionRunCallback<NextAction>())
        }
    }

    @Composable
    private fun Control(icon: Int, description: Int, tint: androidx.glance.unit.ColorProvider, action: androidx.glance.action.Action) {
        Image(
            ImageProvider(icon),
            contentDescription = androidx.glance.LocalContext.current.getString(description),
            modifier = GlanceModifier.size(48.dp).padding(12.dp).clickable(action),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

class RostoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RostoWidget()
}

private suspend fun command(context: Context, block: (MediaController) -> Unit) = withContext(Dispatchers.Main) {
    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future = MediaController.Builder(context, token).buildAsync()
    val controller = suspendCancellableCoroutine<MediaController?> { cont ->
        future.addListener({ cont.resume(runCatching { future.get() }.getOrNull()) }, ContextCompat.getMainExecutor(context))
    } ?: return@withContext
    runCatching { block(controller) }
    controller.release()
}

class ToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        command(context) { c ->
            if (c.isPlaying) c.pause() else {
                c.prepare()
                c.play()
            }
        }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        command(context) { it.seekToNext() }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        command(context) { it.seekToPrevious() }
}

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun update(context: Context, title: String, artist: String, playing: Boolean, art: String) {
        val app = context.applicationContext
        scope.launch {
            runCatching {
                val ids = GlanceAppWidgetManager(app).getGlanceIds(RostoWidget::class.java)
                if (ids.isEmpty()) return@launch
                for (id in ids) {
                    updateAppWidgetState(app, id) { prefs ->
                        prefs[KEY_TITLE] = title
                        prefs[KEY_ARTIST] = artist
                        prefs[KEY_PLAYING] = playing
                        prefs[KEY_ART] = art
                    }
                    RostoWidget().update(app, id)
                }
            }
        }
    }
}
