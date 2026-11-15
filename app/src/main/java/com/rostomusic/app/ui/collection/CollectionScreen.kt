package com.rostomusic.app.ui.collection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import com.rostomusic.app.core.art.EmbeddedArtFetcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rostomusic.app.core.design.BentoCard
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.data.repo.ArtistInfo
import com.rostomusic.app.data.repo.ArtistInfoRepository
import com.rostomusic.app.data.settings.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rostomusic.app.core.design.InfoChip
import com.rostomusic.app.core.design.DolbyBadge
import com.rostomusic.app.core.design.ToneScope
import com.rostomusic.app.core.design.rememberCoverTone
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.aboveSharedCovers
import com.rostomusic.app.core.design.BannerImage
import androidx.compose.runtime.SideEffect
import com.rostomusic.app.core.design.stagger
import com.rostomusic.app.core.design.sharedCover
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.PrimaryButton
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoAlert
import com.rostomusic.app.core.design.RostoSheet
import com.rostomusic.app.core.design.SecondaryButton
import com.rostomusic.app.core.design.SectionHeader
import com.rostomusic.app.core.design.SheetItem
import com.rostomusic.app.core.design.SongRow
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.TopBar
import com.rostomusic.app.core.design.formatDuration
import com.rostomusic.app.core.design.songSubtitle
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.Album
import com.rostomusic.app.data.repo.BackupManager
import com.rostomusic.app.data.repo.CollectionType
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.LocalBottomInset
import com.rostomusic.app.ui.LocalSettings
import com.rostomusic.app.ui.Routes
import com.rostomusic.app.ui.SongMenu
import com.rostomusic.app.ui.UiMessage
import com.rostomusic.app.ui.common.MediaTile
import com.rostomusic.app.ui.common.SheetTitle
import com.rostomusic.app.ui.common.TextInputDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A song with the playlist entry it comes from, when the collection is a playlist. */
data class CollectionRow(val song: Song, val entryId: Long? = null)

data class CollectionUi(
    val loaded: Boolean = false,
    val name: String = "",
    val rows: List<CollectionRow> = emptyList(),
    val albums: List<Album> = emptyList(),
) {
    val songs: List<Song> get() = rows.map { it.song }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val library: LibraryRepository,
    private val backup: BackupManager,
    private val artistInfoRepo: ArtistInfoRepository,
    settings: SettingsStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val type: String = savedState.get<String>("type").orEmpty()
    val key: String = savedState.get<String>("key").orEmpty()
    val messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)

    val ui: StateFlow<CollectionUi> = when (type) {
        CollectionType.PLAYLIST -> combine(library.playlists, library.songsById) { playlists, byId ->
            val p = playlists.firstOrNull { it.id == key.toLongOrNull() }
            CollectionUi(true, p?.name.orEmpty(), p?.entries.orEmpty().mapNotNull { e -> byId[e.songId]?.let { CollectionRow(it, e.id) } })
        }
        CollectionType.ARTIST -> combine(library.collectionSongs(type, key), library.albums) { songs, albums ->
            val ids = songs.mapTo(HashSet()) { it.albumId }
            CollectionUi(true, key, songs.map { CollectionRow(it) }, albums.filter { it.id in ids })
        }
        else -> library.collectionSongs(type, key).map { songs -> CollectionUi(true, nameOf(songs), songs.map { CollectionRow(it) }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firstUi())

    private fun nameOf(songs: List<Song>): String = when (type) {
        CollectionType.ALBUM -> songs.firstOrNull()?.album.orEmpty()
        CollectionType.FOLDER -> key.substringAfterLast('/')
        else -> key
    }

    /**
     * The page as it is right now, built from the library already in memory, so the first frame
     * shows the real title and songs instead of "Artiste inconnu · 0 titre". Album and artist pages
     * use the very same order as the live list, so nothing moves when it arrives.
     */
    private fun firstUi(): CollectionUi = runCatching {
        if (library.songs.value.isEmpty()) return@runCatching CollectionUi(name = if (type == CollectionType.ARTIST) key else "")
        when (type) {
            CollectionType.ALBUM -> library.collectionSnapshot(type, key).let { CollectionUi(true, nameOf(it), it.map { s -> CollectionRow(s) }) }
            CollectionType.ARTIST -> library.collectionSnapshot(type, key).let { songs ->
                val ids = songs.mapTo(HashSet()) { it.albumId }
                CollectionUi(true, key, songs.map { CollectionRow(it) }, library.albums.value.filter { it.id in ids })
            }
            else -> CollectionUi(name = if (type == CollectionType.FOLDER) key.substringAfterLast('/') else "")
        }
    }.getOrDefault(CollectionUi())

    /**
     * Up to four songs of this list with their own picture, one per album, for the banner mosaic.
     * Null while it is being worked out; known straight away on a page seen before.
     */
    val coverAlbums: StateFlow<List<Song>?> = ui.map { it.songs }.distinctUntilChanged().mapLatest { songs ->
        withContext(Dispatchers.IO) { pickMosaic(songs) { hasPicture(it) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), pickMosaic(ui.value.songs) { PictureMemo[it.id] })

    private inline fun pickMosaic(songs: List<Song>, has: (Song) -> Boolean?): List<Song>? {
        val out = ArrayList<Song>()
        for (s in songs.distinctBy { it.albumId }.take(40)) {
            when (has(s)) { null -> return null; true -> out += s; false -> Unit }
            if (out.size == 4) break
        }
        return out
    }

    /** Same test as the song covers: the song's own picture, else its album's only when the album is a real one. */
    private fun hasPicture(song: Song): Boolean =
        PictureMemo[song.id] ?: (EmbeddedArtFetcher.songPicture(context, song.artUri) != null).also { PictureMemo[song.id] = it }

    /** The artist photo already on the phone, without waiting, or null. */
    fun photoNow(): String? = if (type == CollectionType.ARTIST) artistInfoRepo.peek(key)?.imageUrl?.takeIf { it.isNotEmpty() } else null

    /** Photo and biography of the artist, only on artist pages and when the online option is on. */
    val artistInfo: StateFlow<ArtistInfo?> = if (type != CollectionType.ARTIST || key.isBlank()) MutableStateFlow(null)
    else settings.flow.map { it.artistInfoOnline }.distinctUntilChanged().mapLatest { on ->
        if (on) artistInfoRepo.get(key) else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), if (type == CollectionType.ARTIST) artistInfoRepo.peek(key) else null)

    fun rename(name: String) {
        key.toLongOrNull()?.let { id -> viewModelScope.launch { library.renamePlaylist(id, name) } }
    }

    fun delete(onDone: () -> Unit) {
        key.toLongOrNull()?.let { id ->
            viewModelScope.launch {
                library.deletePlaylist(id)
                onDone()
            }
        }
    }

    fun export(uri: android.net.Uri) {
        viewModelScope.launch {
            val ok = backup.exportPlaylist(uri, ui.value.songs).isSuccess
            messages.emit(UiMessage(if (ok) R.string.msg_export_done else R.string.msg_export_failed))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectionScreen(nav: NavController) {
    val app = LocalApp.current
    val settings = LocalSettings.current
    val vm: CollectionViewModel = hiltViewModel()
    val c = Rosto.colors
    val ui by vm.ui.collectAsStateWithLifecycle()
    val state by app.playerState.collectAsStateWithLifecycle()
    var menu by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri -> if (uri != null) vm.export(uri) }
    LaunchedEffect(Unit) { vm.messages.collect { app.messages.emit(it) } }

    val songs = ui.songs
    val isAlbum = vm.type == CollectionType.ALBUM
    val isPlaylist = vm.type == CollectionType.PLAYLIST
    val title = when (vm.type) {
        CollectionType.FAVORITES -> stringResource(R.string.favorites)
        CollectionType.RECENT_PLAYED -> stringResource(R.string.section_recently_played)
        CollectionType.RECENT_ADDED -> stringResource(R.string.section_recently_added)
        CollectionType.MOST_PLAYED -> stringResource(R.string.section_most_played)
        CollectionType.NEVER_PLAYED -> stringResource(R.string.never_played)
        CollectionType.DAILY_MIX -> stringResource(R.string.daily_mix)
        CollectionType.ALBUM -> ui.name.ifEmpty { stringResource(R.string.unknown_album) }
        CollectionType.ARTIST -> ui.name.ifEmpty { stringResource(R.string.unknown_artist) }
        else -> ui.name
    }
    val first = songs.firstOrNull()
    val subtitle = buildString {
        if (isAlbum && first != null) {
            append(first.albumArtist.ifEmpty { first.artist }.ifEmpty { stringResource(R.string.unknown_artist) })
            append(" · ")
        }
        append(pluralStringResource(R.plurals.songs_count, songs.size, songs.size))
        if (songs.isNotEmpty()) append(" · ").append(formatDuration(songs.sumOf { it.duration }))
    }

    val artistInfo by vm.artistInfo.collectAsStateWithLifecycle()
    val coverAlbums by vm.coverAlbums.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val stagger = com.rostomusic.app.core.design.rememberStagger()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val albumArtist = if (isAlbum && first != null) first.albumArtist.ifEmpty { first.artist }.ifEmpty { stringResource(R.string.unknown_artist) } else null
    val quality = com.rostomusic.app.core.design.LocalSongQuality.current
    val dolby = remember(songs, quality) {
        songs.firstNotNullOfOrNull { com.rostomusic.app.domain.ReleaseInfo.dolbyLabel(quality[it.id]?.codecMime) }
    }
    val chips = if (!ui.loaded) emptyList() else buildList {
        add(pluralStringResource(R.plurals.songs_count, songs.size, songs.size))
        if (songs.isNotEmpty()) add(formatDuration(songs.sumOf { it.duration }))
        val genre = songs.map { it.genre }.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        if (genre != null && vm.type != CollectionType.GENRE) add(genre)
        if (isAlbum && first != null && first.year > 0) add(first.year.toString())
    }

    // The banner picture is chosen once, from what is ready when the page opens, and never swapped
    // while the user looks at it: the artist photo when it was fetched ahead, else the covers.
    // A photo that arrives later is kept for the next visit.
    val photo = remember { if (settings.artistInfoOnline) vm.photoNow() else null }
    val mosaic = if (isAlbum || photo != null) emptyList() else coverAlbums
    // The page takes the colour of the picture's lower edge, which the banner fades into (Apple Music style).
    val albumId = if (isAlbum) vm.key.toLongOrNull() else null
    val toneModel: Any? = when {
        photo != null -> photo
        // Same picture as the album's tile everywhere (Albums, Home), so the cover that grows is the banner.
        albumId != null -> Song.albumArtUri(albumId)
        mosaic == null -> null
        else -> mosaic.firstOrNull()?.artUri ?: first?.artUri
    }
    val tone = rememberCoverTone(toneModel, c.background, alternative = albumId?.let { Song.embeddedArtUri(it) })

    // The bottom bar and the mini-player take this page's colour in the same frame as the page.
    val toneOwner = remember { Any() }
    // The bar gives the colour back as soon as the page starts to leave, not when it is gone.
    val leaving = com.rostomusic.app.core.design.LocalNavScope.current?.transition?.targetState == androidx.compose.animation.EnterExitState.PostExit
    SideEffect { if (leaving) app.clearPageTone(toneOwner) else app.setPageTone(toneOwner, tone) }
    DisposableEffect(Unit) { onDispose { app.clearPageTone(toneOwner) } }
    ToneScope(tone) {
    val c = Rosto.colors
    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomInset.current + Space.x4)) {
            item(key = "banner") {
                Banner(
                    title = title,
                    imageUrl = photo,
                    albumId = albumId,
                    first = first,
                    mosaic = mosaic,
                    sharedKey = com.rostomusic.app.core.design.coverKey(vm.type, vm.key),
                )
            }
            item(key = "actions") {
                // Left-aligned like the rest of the app: the eye starts each line at the same edge.
                Column(Modifier.fillMaxWidth().padding(horizontal = Space.screen), horizontalAlignment = Alignment.Start) {
                    Text(
                        title.ifEmpty { " " }, style = Rosto.type.display, color = c.textPrimary, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start,
                    )
                    if (albumArtist != null) {
                        Text(albumArtist, style = Rosto.type.heading, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
                    }
                    Spacer(Modifier.height(Space.x3))
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalArrangement = Arrangement.spacedBy(Space.x2),
                    ) {
                        chips.forEach { InfoChip(it) }
                        dolby?.let { DolbyBadge(it) }
                    }
                    Spacer(Modifier.height(Space.x5))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PrimaryButton(
                            text = stringResource(R.string.play),
                            onClick = { app.play(songs, 0) },
                            modifier = Modifier.weight(1f),
                            icon = R.drawable.ic_play_filled,
                            enabled = songs.isNotEmpty(),
                        )
                        IconBtn(
                            icon = R.drawable.ic_shuffle,
                            contentDescription = stringResource(R.string.shuffle),
                            onClick = { app.shuffle(songs) },
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                            container = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                            size = 56.dp,
                            iconSize = 22.dp,
                            enabled = songs.isNotEmpty(),
                        )
                    }
                    val bio = artistInfo?.bio.orEmpty()
                    // Already there when it was saved before; a biography that arrives later unfolds
                    // in 200 ms instead of pushing the songs down in one jump.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = bio.isNotEmpty(),
                        enter = androidx.compose.animation.expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                    ) {
                        Column {
                            Spacer(Modifier.height(Space.x4))
                            Biography(bio, artistInfo?.bioUrl.orEmpty())
                        }
                    }
                    Spacer(Modifier.height(Space.x4))
                }
                SectionHeader(stringResource(R.string.tab_songs))
            }
            if (ui.loaded && songs.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        R.drawable.ic_list_music,
                        stringResource(if (isPlaylist) R.string.empty_playlist_title else R.string.empty_collection_title),
                        stringResource(if (isPlaylist) R.string.empty_playlist_body else R.string.empty_collection_body),
                    )
                }
            }
            itemsIndexed(ui.rows, key = { i, r -> r.entryId ?: "${r.song.id}-$i" }) { i, row ->
                val s = row.song
                SongRow(
                    s,
                    if (isAlbum) formatDuration(s.duration) + if (s.artist.isNotEmpty() && s.artist != first?.albumArtist?.ifEmpty { first.artist }) " · ${s.artist}" else ""
                    else songSubtitle(s, settings.rowSubtitle),
                    onClick = { app.play(songs, i) },
                    isCurrent = state.song?.id == s.id, isPlaying = state.isPlaying,
                    number = if (isAlbum) (s.track.takeIf { it > 0 } ?: (i + 1)) else null,
                    onAdd = { app.enqueue(listOf(s)) },
                    onMore = { app.songMenu.value = SongMenu(s, row.entryId) },
                    modifier = Modifier.stagger(stagger, i),
                )
            }
            if (ui.albums.isNotEmpty()) {
                item(key = "albums") {
                    Spacer(Modifier.height(Space.x4))
                    SectionHeader(stringResource(R.string.tab_albums))
                    LazyRow(contentPadding = PaddingValues(horizontal = Space.screen), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                        items(ui.albums, key = { it.id }) { a ->
                            MediaTile(
                                a.id, a.title, a.title.ifEmpty { stringResource(R.string.unknown_album) }, if (a.year > 0) a.year.toString() else "",
                                onClick = { nav.navigate(Routes.collection(CollectionType.ALBUM, a.id.toString())) }, width = 152.dp,
                                sharedKey = com.rostomusic.app.core.design.coverKey(CollectionType.ALBUM, a.id.toString()),
                            )
                        }
                    }
                }
            }
        }

        // Floating bar: icons over the banner, then a solid bar with the title once the banner scrolls away.
        Row(
            Modifier
                .aboveSharedCovers()
                .fillMaxWidth()
                .background(if (collapsed) c.background else Color.Transparent)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = Space.x1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val scrim = if (collapsed) Color.Transparent else c.background.copy(alpha = 0.72f)
            IconBtn(R.drawable.ic_chevron_left, stringResource(R.string.back), { nav.popBackStack() }, container = scrim, size = 48.dp)
            Text(
                if (collapsed) title else "", style = Rosto.type.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = Space.x3),
            )
            IconBtn(R.drawable.ic_ellipsis_vertical, stringResource(R.string.more_options), { menu = true }, container = scrim, size = 48.dp)
        }
    }
    }

    if (menu) {
        RostoSheet({ menu = false }) {
            SheetTitle(title, subtitle)
            Column(Modifier.navigationBarsPadding().padding(bottom = Space.x3)) {
                SheetItem(R.drawable.ic_corner_down_right, stringResource(R.string.play_next), { menu = false; app.playNext(songs) })
                SheetItem(R.drawable.ic_list_end, stringResource(R.string.add_all_to_queue), { menu = false; app.enqueue(songs) })
                SheetItem(R.drawable.ic_list_plus, stringResource(R.string.add_to_playlist), { menu = false; if (songs.isNotEmpty()) app.playlistPicker.value = songs })
                SheetItem(R.drawable.ic_file_up, stringResource(R.string.export_m3u), { menu = false; exporter.launch("$title.m3u8") })
                val pin = "${vm.type}:${vm.key}"
                val pinned = remember(settings.homeLayout) { pin in com.rostomusic.app.domain.HomeConfig.decode(settings.homeLayout).pins }
                SheetItem(
                    if (pinned) R.drawable.ic_pin_off else R.drawable.ic_pin,
                    stringResource(if (pinned) R.string.unpin_from_home else R.string.pin_to_home),
                    { menu = false; app.togglePin(pin, pinned) },
                )
                if (isPlaylist) {
                    SheetItem(R.drawable.ic_pencil, stringResource(R.string.rename), { menu = false; renaming = true })
                    SheetItem(R.drawable.ic_trash_2, stringResource(R.string.delete_playlist), { menu = false; deleting = true }, tint = c.danger)
                }
            }
        }
    }
    if (renaming) {
        TextInputDialog(
            title = stringResource(R.string.rename),
            placeholder = stringResource(R.string.playlist_name),
            confirmLabel = stringResource(R.string.save),
            initial = ui.name,
            onDismiss = { renaming = false },
            onConfirm = {
                vm.rename(it)
                renaming = false
            },
        )
    }
    if (deleting) {
        RostoAlert(
            title = stringResource(R.string.delete_playlist),
            text = stringResource(R.string.delete_playlist_body, ui.name),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { deleting = false },
            onConfirm = {
                deleting = false
                vm.delete { nav.popBackStack() }
            },
        )
    }
}

/** Large picture at the top that fades into the page, with the name set over its lower part. */
@Composable
private fun Banner(title: String, imageUrl: String?, albumId: Long?, first: Song?, mosaic: List<Song>?, sharedKey: String? = null) {
    val c = Rosto.colors
    Box(Modifier.fillMaxWidth().height(400.dp)) {
      // Never a music-note placeholder here: while a picture loads, the banner is the page's colour.
      Box(Modifier.sharedCover(sharedKey).fillMaxSize()) {
        when {
            imageUrl != null -> BannerImage(imageUrl, Modifier.fillMaxSize())
            albumId != null -> CoverArt(albumId, title, Modifier.fillMaxSize(), radius = 0.dp, banner = true)
            mosaic == null -> Unit
            mosaic.size >= 4 -> Column(Modifier.fillMaxSize()) {
                for (row in 0..1) Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0..1) CoverArt(mosaic[row * 2 + col], Modifier.weight(1f).fillMaxHeight(), radius = 0.dp, banner = true)
                }
            }
            mosaic.isNotEmpty() -> CoverArt(mosaic.first(), Modifier.fillMaxSize(), radius = 0.dp, banner = true)
            first != null -> CoverArt(first, Modifier.fillMaxSize(), radius = 0.dp, banner = true)
            else -> Unit
        }
      }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.28f),
                    0.22f to Color.Transparent,
                    0.6f to Color.Transparent,
                    0.85f to c.background.copy(alpha = 0.85f),
                    1f to c.background,
                )
            )
        )
    }
}

@Composable
private fun Chip(text: String) {
    val c = Rosto.colors
    Text(
        text, style = Rosto.type.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(RoundedCornerShape(Radius.small)).background(c.card).padding(horizontal = Space.x3, vertical = Space.x2),
    )
}

/** Biography folded to two lines with "PLUS" (Apple Music style); open, it links to the full page. */
@Composable
private fun Biography(text: String, url: String, modifier: Modifier = Modifier) {
    val c = Rosto.colors
    val uri = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.small)).clickable(role = Role.Button) { expanded = !expanded }.padding(vertical = Space.x1)) {
        Text(text, style = Rosto.type.body, color = c.textSecondary, maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (expanded && url.isNotEmpty()) {
                Row(
                    Modifier.clip(RoundedCornerShape(Radius.small)).clickable(role = Role.Button) { runCatching { uri.openUri(url) } }
                        .heightIn(min = Space.touch).padding(end = Space.x2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.x2),
                ) {
                    Text(stringResource(R.string.bio_read_more), style = Rosto.type.label, color = c.textPrimary)
                    RIcon(R.drawable.ic_external_link, null, tint = c.textPrimary, size = 16.dp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (expanded) R.string.less else R.string.more).uppercase(), style = Rosto.type.label.copy(fontWeight = FontWeight.ExtraBold),
                color = c.textPrimary, modifier = Modifier.padding(vertical = Space.x2),
            )
        }
    }
}

/** Which songs have a picture of their own, remembered for the life of the app. */
private object PictureMemo {
    private val map = java.util.concurrent.ConcurrentHashMap<Long, Boolean>()
    operator fun get(id: Long): Boolean? = map[id]
    operator fun set(id: Long, value: Boolean) { map[id] = value }
}
