package com.rostomusic.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.Pill
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.ScreenTitle
import com.rostomusic.app.core.design.SectionHeader
import com.rostomusic.app.core.design.SongRow
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.songSubtitle
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.Album
import com.rostomusic.app.data.repo.Artist
import com.rostomusic.app.data.repo.CollectionType
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.repo.PlaylistInfo
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.LocalBottomInset
import com.rostomusic.app.ui.LocalSettings
import com.rostomusic.app.ui.Routes
import com.rostomusic.app.ui.SongMenu
import com.rostomusic.app.ui.common.MediaTile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<PlaylistInfo> = emptyList(),
    val lyricMatches: List<Song> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty() && lyricMatches.isEmpty()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val db: RostoDatabase,
    private val settings: SettingsStore,
) : ViewModel() {
    val query = MutableStateFlow("")

    val results: StateFlow<SearchResults> = combine(query.debounce(120), library.songs) { q, songs ->
        val needle = fold(q.trim())
        if (needle.isEmpty()) return@combine SearchResults()
        val found = songs.filter { fold(it.title).contains(needle) || fold(it.artist).contains(needle) || fold(it.album).contains(needle) }
        val ids = found.mapTo(HashSet()) { it.id }
        val inLyrics = if (needle.length >= 3) library.songsOf(db.lyrics().search(q.trim())).filter { it.id !in ids } else emptyList()
        SearchResults(
            songs = found.sortedBy { if (fold(it.title).startsWith(needle)) 0 else 1 }.take(60),
            albums = library.albums.value.filter { fold(it.title).contains(needle) }.take(12),
            artists = library.artists.value.filter { it.name.isNotEmpty() && fold(it.name).contains(needle) }.take(12),
            playlists = library.playlists.value.filter { fold(it.name).contains(needle) },
            lyricMatches = inLyrics,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    fun remember() {
        val q = query.value.trim()
        if (q.length >= 2) viewModelScope.launch { settings.addSearch(q) }
    }

    fun clearHistory() {
        viewModelScope.launch { settings.clearSearchHistory() }
    }

    /** Lower-cases and strips accents so "eté" finds "Été". */
    private fun fold(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(MARKS, "")

    private companion object {
        val MARKS = Regex("\\p{Mn}+")
    }
}

@Composable
fun SearchScreen(nav: NavController) {
    val app = LocalApp.current
    val settings = LocalSettings.current
    val vm: SearchViewModel = hiltViewModel()
    val c = Rosto.colors
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val state by app.playerState.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    // Opened from the search field at the top of a tab: the keyboard comes up straight away.
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (query.isEmpty()) runCatching { focusRequester.requestFocus() } }
    val unknownArtist = stringResource(R.string.unknown_artist)

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.nav_search), Modifier.padding(start = Space.screen, top = Space.x3, end = Space.screen))
        Spacer(Modifier.height(Space.x3))
        Row(
            Modifier.padding(horizontal = Space.screen).fillMaxWidth().height(56.dp).clip(CircleShape).background(c.surfaceContainerHigh).padding(start = Space.x4, end = Space.x1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RIcon(R.drawable.ic_search, null, tint = c.textMuted, size = 20.dp)
            Spacer(Modifier.width(Space.x3))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(stringResource(R.string.search_hint), style = Rosto.type.body, color = c.textMuted, maxLines = 1)
                BasicTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    singleLine = true,
                    textStyle = Rosto.type.body.copy(color = c.textPrimary),
                    cursorBrush = SolidColor(c.accentText),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.remember(); focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            if (query.isNotEmpty()) IconBtn(R.drawable.ic_x, stringResource(R.string.clear), { vm.query.value = "" }, tint = c.textMuted, iconSize = 18.dp)
        }
        Spacer(Modifier.height(Space.x2))

        if (query.isBlank()) {
            if (settings.searchHistory.isEmpty()) {
                EmptyState(R.drawable.ic_search, stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_body))
            } else {
                SectionHeader(stringResource(R.string.search_recent))
                LazyColumn(contentPadding = PaddingValues(bottom = LocalBottomInset.current)) {
                    items(settings.searchHistory) { past ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.query.value = past }.heightIn(min = Space.touch).padding(horizontal = Space.screen),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RIcon(R.drawable.ic_history, null, tint = c.textMuted, size = 18.dp)
                            Spacer(Modifier.width(Space.x3))
                            Text(past, style = Rosto.type.body, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    item {
                        Box(Modifier.padding(horizontal = Space.screen, vertical = Space.x2)) {
                            Pill(stringResource(R.string.search_clear_history), false, { vm.clearHistory() })
                        }
                    }
                }
            }
            return@Column
        }

        if (results.isEmpty) {
            EmptyState(R.drawable.ic_search, stringResource(R.string.search_none_title, query.trim()), stringResource(R.string.search_none_body))
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomInset.current + Space.x4)) {
            if (results.artists.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.tab_artists))
                    LazyRow(contentPadding = PaddingValues(horizontal = Space.screen), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                        items(results.artists, key = { it.name }) { a ->
                            MediaTile(a.albumId, a.name, a.name, "", onClick = {
                                vm.remember()
                                nav.navigate(Routes.collection(CollectionType.ARTIST, a.name))
                            }, width = 88.dp, circle = true, sharedKey = com.rostomusic.app.core.design.coverKey(CollectionType.ARTIST, a.name))
                        }
                    }
                    Spacer(Modifier.height(Space.x2))
                }
            }
            if (results.albums.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.tab_albums))
                    LazyRow(contentPadding = PaddingValues(horizontal = Space.screen), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                        items(results.albums, key = { it.id }) { a ->
                            MediaTile(a.id, a.title, a.title.ifEmpty { stringResource(R.string.unknown_album) }, a.artist.ifEmpty { unknownArtist }, onClick = {
                                vm.remember()
                                nav.navigate(Routes.collection(CollectionType.ALBUM, a.id.toString()))
                            }, width = 120.dp, sharedKey = com.rostomusic.app.core.design.coverKey(CollectionType.ALBUM, a.id.toString()))
                        }
                    }
                    Spacer(Modifier.height(Space.x2))
                }
            }
            if (results.playlists.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.tab_playlists)) }
                items(results.playlists, key = { "p${it.id}" }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            vm.remember()
                            nav.navigate(Routes.collection(CollectionType.PLAYLIST, p.id.toString()))
                        }.heightIn(min = 56.dp).padding(horizontal = Space.screen),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(Radius.small)).background(c.card), contentAlignment = Alignment.Center) {
                            RIcon(R.drawable.ic_list_music, null, tint = c.textSecondary, size = 20.dp)
                        }
                        Spacer(Modifier.width(Space.x3))
                        Column {
                            Text(p.name, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pluralStringResource(R.plurals.songs_count, p.entries.size, p.entries.size), style = Rosto.type.label, color = c.textSecondary)
                        }
                    }
                }
            }
            if (results.songs.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.tab_songs), onAddAll = { app.enqueue(results.songs) }) }
                itemsIndexed(results.songs, key = { _, s -> "s${s.id}" }) { i, s ->
                    SongRow(
                        s, songSubtitle(s, settings.rowSubtitle),
                        onClick = { vm.remember(); app.play(results.songs, i) },
                        isCurrent = state.song?.id == s.id, isPlaying = state.isPlaying,
                        onAdd = { app.enqueue(listOf(s)) },
                        onMore = { app.songMenu.value = SongMenu(s) },
                    )
                }
            }
            if (results.lyricMatches.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.search_in_lyrics)) }
                itemsIndexed(results.lyricMatches, key = { _, s -> "l${s.id}" }) { i, s ->
                    SongRow(
                        s, songSubtitle(s, settings.rowSubtitle),
                        onClick = { vm.remember(); app.play(results.lyricMatches, i) },
                        isCurrent = state.song?.id == s.id, isPlaying = state.isPlaying,
                        onMore = { app.songMenu.value = SongMenu(s) },
                    )
                }
            }
        }
    }
}
