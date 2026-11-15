package com.rostomusic.app.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.Badge
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.stagger
import com.rostomusic.app.core.design.sharedCover
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.Pill
import com.rostomusic.app.core.design.PrimaryButton
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.ScreenTitle
import com.rostomusic.app.core.design.SecondaryButton
import com.rostomusic.app.core.design.SkeletonRows
import com.rostomusic.app.core.design.SongRow
import com.rostomusic.app.core.design.Space
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
import com.rostomusic.app.ui.common.TextInputDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SongSort(val label: Int) {
    TITLE(R.string.sort_title), ARTIST(R.string.sort_artist), ALBUM(R.string.sort_album), ADDED(R.string.sort_added), DURATION(R.string.sort_duration)
}

enum class AlbumSort(val label: Int) {
    TITLE(R.string.sort_title), ARTIST(R.string.sort_artist), YEAR(R.string.sort_year), COUNT(R.string.sort_count), ADDED(R.string.sort_added)
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val backup: BackupManager,
) : ViewModel() {
    val songSort = MutableStateFlow(SongSort.TITLE)
    val albumSort = MutableStateFlow(AlbumSort.TITLE)
    val favoritesOnly = MutableStateFlow(false)
    val songDescending = MutableStateFlow(false)
    val albumDescending = MutableStateFlow(false)
    val includeSingles = MutableStateFlow(true)
    val albumsGrid = MutableStateFlow(false)
    /** Genre picked in the sort sheet; null shows every genre. */
    val genre = MutableStateFlow<String?>(null)
    val genres: StateFlow<List<String>> = library.genres.map { list -> list.map { it.name }.filter { it.isNotBlank() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)

    private data class SongFilter(val sort: SongSort, val fav: Boolean, val desc: Boolean, val genre: String?)
    private data class AlbumFilter(val sort: AlbumSort, val desc: Boolean, val singles: Boolean, val genre: String?)

    val songs: StateFlow<List<Song>> = combine(
        library.songs,
        combine(songSort, favoritesOnly, songDescending, genre) { a, b, c, d -> SongFilter(a, b, c, d) },
    ) { list, f ->
        var base = if (f.fav) list.filter { it.favorite } else list
        if (f.genre != null) base = base.filter { it.genre.equals(f.genre, ignoreCase = true) }
        val sorted = when (f.sort) {
            SongSort.TITLE -> base.sortedBy { it.title.lowercase() }
            SongSort.ARTIST -> base.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            SongSort.ALBUM -> base.sortedWith(compareBy({ it.album.lowercase() }, { it.disc }, { it.track }))
            SongSort.ADDED -> base.sortedByDescending { it.dateAdded }
            SongSort.DURATION -> base.sortedByDescending { it.duration }
        }
        if (f.desc) sorted.asReversed() else sorted
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = combine(
        library.albums,
        library.songs,
        combine(albumSort, albumDescending, includeSingles, genre) { a, b, c, d -> AlbumFilter(a, b, c, d) },
    ) { list, songs, f ->
        var base = if (f.singles) list else list.filter { it.songCount > 1 }
        if (f.genre != null) {
            val ids = songs.filter { it.genre.equals(f.genre, ignoreCase = true) }.mapTo(HashSet()) { it.albumId }
            base = base.filter { it.id in ids }
        }
        val sorted = when (f.sort) {
            AlbumSort.TITLE -> base
            AlbumSort.ARTIST -> base.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            AlbumSort.YEAR -> base.sortedByDescending { it.year }
            AlbumSort.COUNT -> base.sortedByDescending { it.songCount }
            AlbumSort.ADDED -> base.sortedByDescending { it.dateAdded }
        }
        if (f.desc) sorted.asReversed() else sorted
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importPlaylist(uri: android.net.Uri, name: String) {
        viewModelScope.launch {
            backup.importPlaylist(uri, name).fold(
                onSuccess = { (matched, total) ->
                    messages.emit(if (matched == 0) UiMessage(R.string.msg_import_none) else UiMessage(R.string.msg_import_done, listOf(matched, total)))
                },
                onFailure = { messages.emit(UiMessage(R.string.msg_import_failed)) },
            )
        }
    }
}

/** The Bibliothèque tab: the playlists. Songs, albums and artists have their own tabs; genres sit in the sort sheet. */
@Composable
fun LibraryScreen(nav: NavController, @Suppress("UNUSED_PARAMETER") initialTab: Int) {
    val app = LocalApp.current
    val vm: LibraryViewModel = hiltViewModel()
    val loaded by app.library.loaded.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.messages.collect { app.messages.emit(it) } }

    Column(Modifier.fillMaxSize()) {
        com.rostomusic.app.ui.common.TopSearchBar(nav)
        ScreenTitle(stringResource(R.string.nav_library), Modifier.padding(start = Space.screen, top = Space.x3, end = Space.screen, bottom = Space.x2))
        if (!loaded) {
            SkeletonRows()
            return@Column
        }
        PlaylistsTab(vm, nav)
    }
}

@Composable
internal fun SongsTab(vm: LibraryViewModel) {
    val app = LocalApp.current
    val settings = LocalSettings.current
    val songs by vm.songs.collectAsStateWithLifecycle()
    val sort by vm.songSort.collectAsStateWithLifecycle()
    val favOnly by vm.favoritesOnly.collectAsStateWithLifecycle()
    val desc by vm.songDescending.collectAsStateWithLifecycle()
    val genre by vm.genre.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()
    val state by app.playerState.collectAsStateWithLifecycle()
    var sorting by rememberSaveable { mutableStateOf(false) }
    val stagger = com.rostomusic.app.core.design.rememberStagger()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomInset.current + Space.x4)) {
        item(key = "sort") {
            SortBar(
                label = genre ?: stringResource(sort.label), descending = desc, onSort = { sorting = true },
                onShuffle = { app.shuffle(songs) },
            )
        }
        if (songs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    R.drawable.ic_music,
                    stringResource(if (favOnly) R.string.empty_favorites_title else R.string.empty_library_title),
                    stringResource(if (favOnly) R.string.empty_favorites_body else R.string.empty_library_body),
                )
            }
        }
        itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
            SongRow(
                s, songSubtitle(s, settings.rowSubtitle),
                onClick = { app.play(songs, i) },
                isCurrent = state.song?.id == s.id, isPlaying = state.isPlaying,
                onAdd = { app.enqueue(listOf(s)) },
                onMore = { app.songMenu.value = SongMenu(s) },
                modifier = Modifier.stagger(stagger, i),
            )
        }
    }
    if (sorting) {
        SortSheet(
            section = stringResource(R.string.nav_songs),
            options = SongSort.entries.map { stringResource(it.label) },
            selected = sort.ordinal, onSelect = { vm.songSort.value = SongSort.entries[it] },
            descending = desc, onDescending = { vm.songDescending.value = it },
            switchTitle = stringResource(R.string.favorites_only), switchHint = stringResource(R.string.favorites_only_hint),
            switchOn = favOnly, onSwitch = { vm.favoritesOnly.value = it },
            genres = genres, genre = genre, onGenre = { vm.genre.value = it },
            onDismiss = { sorting = false },
        )
    }
}

@Composable
internal fun AlbumsTab(vm: LibraryViewModel, nav: NavController) {
    val app = LocalApp.current
    val c = Rosto.colors
    val albums by vm.albums.collectAsStateWithLifecycle()
    val sort by vm.albumSort.collectAsStateWithLifecycle()
    val desc by vm.albumDescending.collectAsStateWithLifecycle()
    val singles by vm.includeSingles.collectAsStateWithLifecycle()
    val grid by vm.albumsGrid.collectAsStateWithLifecycle()
    val genre by vm.genre.collectAsStateWithLifecycle()
    val genres by vm.genres.collectAsStateWithLifecycle()
    val songs by app.library.songs.collectAsStateWithLifecycle()
    var sorting by rememberSaveable { mutableStateOf(false) }
    val stagger = com.rostomusic.app.core.design.rememberStagger()
    val unknownArtist = stringResource(R.string.unknown_artist)
    val unknownAlbum = stringResource(R.string.unknown_album)
    fun open(a: Album) = nav.navigate(Routes.collection(CollectionType.ALBUM, a.id.toString()))
    val shuffleAll = {
        val ids = albums.mapTo(HashSet()) { it.id }
        app.shuffle(songs.filter { it.albumId in ids })
    }
    val bar: @Composable () -> Unit = {
        SortBar(
            label = genre ?: stringResource(sort.label), descending = desc, onSort = { sorting = true },
            onShuffle = shuffleAll, grid = grid, onGrid = { vm.albumsGrid.value = it },
        )
    }
    val empty: @Composable () -> Unit = {
        EmptyState(R.drawable.ic_disc_3, stringResource(R.string.empty_library_title), stringResource(R.string.empty_library_body))
    }

    if (grid) Column(Modifier.fillMaxSize()) {
        bar()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.screen, end = Space.screen, bottom = LocalBottomInset.current + Space.x4),
            horizontalArrangement = Arrangement.spacedBy(Space.x3),
            verticalArrangement = Arrangement.spacedBy(Space.x4),
        ) {
            if (albums.isEmpty()) item(span = { GridItemSpan(2) }) { empty() }
            itemsIndexed(albums, key = { _, a -> a.id }) { i, a ->
                MediaTile(
                    a.id, a.title, a.title.ifEmpty { unknownAlbum },
                    if (a.year > 0) "${a.artist.ifEmpty { unknownArtist }} · ${a.year}" else a.artist.ifEmpty { unknownArtist },
                    onClick = { open(a) },
                    modifier = Modifier.stagger(stagger, i),
                    width = null,
                    sharedKey = com.rostomusic.app.core.design.coverKey(CollectionType.ALBUM, a.id.toString()),
                )
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomInset.current + Space.x4)) {
            item(key = "sort") { bar() }
            if (albums.isEmpty()) item { empty() }
            itemsIndexed(albums, key = { _, a -> a.id }) { i, a ->
                Row(
                    Modifier.stagger(stagger, i).fillMaxWidth().clickable(role = Role.Button) { open(a) }.heightIn(min = 80.dp).padding(horizontal = Space.screen),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverArt(
                        a.id, a.title,
                        Modifier.sharedCover(com.rostomusic.app.core.design.coverKey(CollectionType.ALBUM, a.id.toString())).size(com.rostomusic.app.core.design.CoverSize.l),
                        Radius.cover,
                    )
                    Spacer(Modifier.width(Space.x4))
                    Column(Modifier.weight(1f)) {
                        Text(a.title.ifEmpty { unknownAlbum }, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (a.year > 0) "${a.artist.ifEmpty { unknownArtist }} · ${a.year}" else a.artist.ifEmpty { unknownArtist },
                            style = Rosto.type.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    if (sorting) {
        SortSheet(
            section = stringResource(R.string.nav_albums),
            options = AlbumSort.entries.map { stringResource(it.label) },
            selected = sort.ordinal, onSelect = { vm.albumSort.value = AlbumSort.entries[it] },
            descending = desc, onDescending = { vm.albumDescending.value = it },
            switchTitle = stringResource(R.string.include_singles), switchHint = stringResource(R.string.include_singles_hint),
            switchOn = singles, onSwitch = { vm.includeSingles.value = it },
            genres = genres, genre = genre, onGenre = { vm.genre.value = it },
            onDismiss = { sorting = false },
        )
    }
}

@Composable
private fun ListRow(title: String, subtitle: String, onClick: () -> Unit, leading: @Composable () -> Unit) {
    val c = Rosto.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(horizontal = Space.screen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(Space.x3))
        Column(Modifier.weight(1f)) {
            Text(title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Rosto.type.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RIcon(R.drawable.ic_chevron_right, null, tint = c.textMuted, size = 18.dp)
    }
}

/** A line of the Bibliothèque under the blocks: picture, name, count and chevron, with a thin divider (Deezer style). */
@Composable
private fun LibRow(title: String, count: String, onClick: () -> Unit, leading: @Composable () -> Unit) {
    val c = Rosto.colors
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = Space.screen)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(Space.x4))
            Text(title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (count.isNotEmpty()) Text(count, style = Rosto.type.label, color = c.textMuted)
            RIcon(R.drawable.ic_chevron_right, null, tint = c.textPrimary, size = 22.dp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
    }
}

@Composable
private fun IconTile(icon: Int) {
    val c = Rosto.colors
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(Radius.image)).background(c.card), contentAlignment = Alignment.Center) {
        RIcon(icon, null, tint = c.textSecondary, size = 22.dp)
    }
}

@Composable
internal fun ArtistsTab(nav: NavController) {
    val app = LocalApp.current
    val c = Rosto.colors
    val artists by app.library.artists.collectAsStateWithLifecycle()
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val byArtist = remember(songs) { songs.groupBy { it.artist } }
    val stagger = com.rostomusic.app.core.design.rememberStagger()
    val photos by app.artistPhotos.collectAsStateWithLifecycle()
    val online = LocalSettings.current.artistInfoOnline
    val cookie = com.rostomusic.app.core.design.cookie9()
    // Artists as bento cards: the artist's photo (Deezer, fetched in the background) when there is one,
    // else the square picture of one of their own songs, then the name and the counts.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Space.screen, end = Space.screen, bottom = LocalBottomInset.current + Space.x4),
        horizontalArrangement = Arrangement.spacedBy(Space.x3),
        verticalArrangement = Arrangement.spacedBy(Space.x3),
    ) {
        if (artists.isEmpty()) item(span = { GridItemSpan(2) }) {
            EmptyState(R.drawable.ic_user_round, stringResource(R.string.empty_library_title), stringResource(R.string.empty_library_body))
        }
        itemsIndexed(artists, key = { _, a -> a.name }) { i, a ->
            val song = byArtist[a.name]?.firstOrNull()
            Column(
                Modifier.stagger(stagger, i).clickable(role = Role.Button) { nav.navigate(Routes.collection(CollectionType.ARTIST, a.name)) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val pic = Modifier.sharedCover(com.rostomusic.app.core.design.coverKey(CollectionType.ARTIST, a.name)).fillMaxWidth().aspectRatio(1f).clip(cookie).background(c.surfaceAlt)
                val photo = if (online) photos[a.name]?.takeIf { it.isNotEmpty() } else null
                if (photo != null) com.rostomusic.app.core.design.BannerImage(photo, pic)
                else if (song != null) CoverArt(song, pic, radius = 0.dp) else CoverArt(-1, a.name, pic, radius = 0.dp)
                Spacer(Modifier.height(Space.x2))
                Text(
                    a.name.ifEmpty { stringResource(R.string.unknown_artist) },
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Space.x2))
            }
        }
    }
}

@Composable
private fun GenresTab(nav: NavController) {
    val app = LocalApp.current
    val genres by app.library.genres.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomInset.current + Space.x4)) {
        if (genres.isEmpty()) item { EmptyState(R.drawable.ic_tag, stringResource(R.string.empty_genres_title), stringResource(R.string.empty_genres_body)) }
        items(genres, key = { it.name }) { g ->
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate(Routes.collection(CollectionType.GENRE, g.name)) }.heightIn(min = 56.dp).padding(horizontal = Space.screen),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Badge(g.name, hueSeed = g.name)
                Spacer(Modifier.weight(1f))
                Text(pluralStringResource(R.plurals.songs_count, g.songCount, g.songCount), style = Rosto.type.label, color = Rosto.colors.textSecondary)
                Spacer(Modifier.width(Space.x2))
                RIcon(R.drawable.ic_chevron_right, null, tint = Rosto.colors.textMuted, size = 18.dp)
            }
        }
    }
}

@Composable
private fun FoldersTab(nav: NavController) {
    val app = LocalApp.current
    val folders by app.library.folders.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomInset.current + Space.x4)) {
        if (folders.isEmpty()) item { EmptyState(R.drawable.ic_folder, stringResource(R.string.empty_library_title), stringResource(R.string.empty_library_body)) }
        items(folders, key = { it.path }) { f ->
            ListRow(
                f.name,
                pluralStringResource(R.plurals.songs_count, f.songCount, f.songCount) + " · " + f.path.substringBeforeLast('/'),
                onClick = { nav.navigate(Routes.collection(CollectionType.FOLDER, f.path)) },
            ) { IconTile(R.drawable.ic_folder) }
        }
    }
}

@Composable
private fun PlaylistsTab(vm: LibraryViewModel, nav: NavController) {
    val app = LocalApp.current
    val c = Rosto.colors
    val settings = LocalSettings.current
    val playlists by app.library.playlists.collectAsStateWithLifecycle()
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val state by app.playerState.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    val importName = stringResource(R.string.imported_playlist)
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importPlaylist(uri, importName)
    }
    val favorites = remember(songs) { songs.filter { it.favorite }.sortedByDescending { it.lastPlayed } }
    val mostPlayed = remember(songs) { songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount } }
    val recentAdded = remember(songs) { songs.sortedByDescending { it.dateAdded } }
    val blocks = listOf(
        Triple(R.drawable.ic_heart_filled to R.string.favorites, CollectionType.FAVORITES, favorites),
        Triple(R.drawable.ic_trending_up to R.string.section_most_played, CollectionType.MOST_PLAYED, mostPlayed),
        Triple(R.drawable.ic_clock to R.string.section_recently_added, CollectionType.RECENT_ADDED, recentAdded),
    )
    val smart = listOf(
        Triple(R.drawable.ic_history, R.string.section_recently_played, CollectionType.RECENT_PLAYED),
        Triple(R.drawable.ic_eye_off, R.string.never_played, CollectionType.NEVER_PLAYED),
        Triple(R.drawable.ic_shuffle, R.string.daily_mix, CollectionType.DAILY_MIX),
    )
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomInset.current + Space.x4),
        verticalArrangement = Arrangement.spacedBy(Space.x3),
    ) {
        // Favourites, most played and recently added as bento blocks, three songs each.
        items(blocks, key = { it.second }) { (head, type, list) ->
            val (icon, label) = head
            Column(Modifier.padding(horizontal = Space.screen).fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(c.card).padding(vertical = Space.x2)) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { nav.navigate(Routes.collection(type)) }
                        .heightIn(min = Space.touch).padding(horizontal = Space.x4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RIcon(icon, null, tint = c.textPrimary, size = 22.dp)
                    Spacer(Modifier.width(Space.x2))
                    Text(stringResource(label), style = Rosto.type.heading, color = c.textPrimary, modifier = Modifier.weight(1f))
                    Text(list.size.toString(), style = Rosto.type.label, color = c.textMuted)
                    RIcon(R.drawable.ic_chevron_right, null, tint = c.textPrimary, size = 22.dp)
                }
                if (list.isEmpty()) {
                    Text(stringResource(R.string.library_block_empty), style = Rosto.type.label, color = c.textMuted, modifier = Modifier.padding(horizontal = Space.x4, vertical = Space.x2))
                }
                list.take(3).forEachIndexed { i, s ->
                    SongRow(
                        s, songSubtitle(s, settings.rowSubtitle),
                        onClick = { app.play(list, i) },
                        isCurrent = state.song?.id == s.id, isPlaying = state.isPlaying,
                        onMore = { app.songMenu.value = SongMenu(s) },
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.screen, vertical = Space.x2), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                PrimaryButton(stringResource(R.string.new_playlist), { creating = true }, Modifier.weight(1f), icon = R.drawable.ic_plus)
                IconBtn(
                    icon = R.drawable.ic_file_down,
                    contentDescription = stringResource(R.string.import_m3u),
                    onClick = { importer.launch(arrayOf("*/*")) },
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                    container = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                    size = 56.dp,
                    iconSize = 22.dp,
                )
            }
        }
        item {
            Column {
                smart.forEach { (icon, label, type) ->
                    LibRow(stringResource(label), "", onClick = { nav.navigate(Routes.collection(type)) }) { IconTile(icon) }
                }
                playlists.forEach { p ->
                    val first = remember(p) { app.library.songsOf(p.songIds.take(1)).firstOrNull() }
                    LibRow(p.name, p.entries.size.toString(), onClick = { nav.navigate(Routes.collection(CollectionType.PLAYLIST, p.id.toString())) }) {
                        val m = Modifier.sharedCover(com.rostomusic.app.core.design.coverKey(CollectionType.PLAYLIST, p.id.toString())).size(com.rostomusic.app.core.design.CoverSize.m)
                        if (first != null) CoverArt(first, m) else IconTile(R.drawable.ic_list_music)
                    }
                }
            }
        }
    }
    if (creating) {
        TextInputDialog(
            title = stringResource(R.string.new_playlist),
            placeholder = stringResource(R.string.playlist_name),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { creating = false },
            onConfirm = {
                app.createPlaylist(it, emptyList())
                creating = false
            },
        )
    }
}
