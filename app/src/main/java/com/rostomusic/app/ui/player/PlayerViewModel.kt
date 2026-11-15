package com.rostomusic.app.ui.player

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.rostomusic.app.data.db.Bookmark
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.settings.ProgressStyle
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.data.tags.AudioDetails
import com.rostomusic.app.data.tags.TagEditor
import com.rostomusic.app.playback.PlayerConnection
import com.rostomusic.app.playback.WaveformRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Everything the full-screen player derives from the current song. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: PlayerConnection,
    private val db: RostoDatabase,
    private val waveforms: WaveformRepository,
    private val tags: TagEditor,
    settings: SettingsStore,
) : ViewModel() {
    private val currentSong = player.state.map { it.song }.distinctUntilChangedBy { it?.id }

    private val _accent = MutableStateFlow<Color?>(null)
    val accent: StateFlow<Color?> = _accent

    private val _details = MutableStateFlow<AudioDetails?>(null)
    val details: StateFlow<AudioDetails?> = _details

    private val _wave = MutableStateFlow(FloatArray(0))
    val wave: StateFlow<FloatArray> = _wave

    val bookmarks: StateFlow<List<Bookmark>> = currentSong
        .flatMapLatest { s -> if (s == null) flowOf(emptyList()) else db.bookmarks().observe(s.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            currentSong.collectLatest { song ->
                _details.value = null
                if (song == null || song.id < 0) {
                    _accent.value = null
                    return@collectLatest
                }
                launch { _accent.value = coverColor(song) }
                launch { _details.value = runCatching { tags.readDetails(song) }.getOrNull() }
            }
        }
        viewModelScope.launch {
            combine(currentSong, settings.flow.map { it.progressStyle }.distinctUntilChanged()) { s, style -> s to style }
                .collectLatest { (song, style) ->
                    if (song == null || style != ProgressStyle.WAVE) return@collectLatest
                    _wave.value = waveforms.placeholder(song.id)
                    _wave.value = waveforms.bars(song)
                }
        }
    }

    private suspend fun coverColor(song: Song): Color? = withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context).data(song.artUri).allowHardware(false).size(96).build()
            val bitmap = (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.darkVibrantSwatch ?: palette.mutedSwatch
            swatch?.let { Color(it.rgb) }
        }.getOrNull()
    }

    fun addBookmark(label: String) {
        val song = player.state.value.song ?: return
        val position = player.position
        viewModelScope.launch {
            db.bookmarks().insert(Bookmark(songId = song.id, positionMs = position, label = label, createdAt = System.currentTimeMillis()))
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch { db.bookmarks().delete(bookmark) }
    }
}
