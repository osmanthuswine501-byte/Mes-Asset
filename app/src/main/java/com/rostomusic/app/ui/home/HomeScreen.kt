package com.rostomusic.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.rostomusic.app.R
import com.rostomusic.app.core.design.Badge
import com.rostomusic.app.core.design.BentoCard
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.ScreenTitle
import com.rostomusic.app.core.design.pressScale
import com.rostomusic.app.core.design.SectionHeader
import com.rostomusic.app.core.design.SkeletonRows
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.formatDuration
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.Album
import com.rostomusic.app.data.repo.CollectionType
import com.rostomusic.app.data.repo.ScanState
import com.rostomusic.app.domain.HomeLayout
import com.rostomusic.app.domain.HomeSection
import com.rostomusic.app.domain.SectionId
import com.rostomusic.app.domain.SectionStyle
import com.rostomusic.app.domain.ShortcutId
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.LocalBottomInset
import com.rostomusic.app.ui.Routes
import kotlin.random.Random

private data class HeroPage(
    val label: Int,
    val title: String,
    val subtitle: String,
    val albumId: Long,
    val seed: String,
    val onOpen: () -> Unit,
    val onPlay: () -> Unit,
    val song: Song? = null,
)

@Composable
fun HomeScreen(nav: NavController) {
    val app = LocalApp.current
    val vm: HomeViewModel = hiltViewModel()
    val c = Rosto.colors
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val loaded by app.library.loaded.collectAsStateWithLifecycle()
    val scan by app.library.scanState.collectAsStateWithLifecycle()
    val layout by vm.layout.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomInset.current + Space.x4)) {
        item(key = "header") {
            com.rostomusic.app.ui.common.TopSearchBar(nav)
            // Screen title as the anchor of the page, with "Tout mélanger" as a plain button beside it.
            Row(
                Modifier.fillMaxWidth().padding(start = Space.screen, end = Space.x3, top = Space.x4, bottom = Space.x3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenTitle(stringResource(R.string.nav_home), Modifier.weight(1f))
                if (songs.isNotEmpty()) {
                    com.rostomusic.app.core.design.SecondaryButton(
                        stringResource(R.string.shuffle_all), { app.shuffle(songs) }, icon = R.drawable.ic_shuffle,
                    )
                }
            }
        }

        if (!loaded || (songs.isEmpty() && scan is ScanState.Scanning)) {
            item(key = "loading") { SkeletonRows() }
            return@LazyColumn
        }
        if (songs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    R.drawable.ic_music,
                    stringResource(R.string.empty_library_title),
                    stringResource(R.string.empty_library_body),
                    actionLabel = stringResource(R.string.rescan),
                    onAction = { app.rescan() },
                )
            }
            return@LazyColumn
        }

        for (section in layout.sections) {
            if (!section.visible) continue
            item(key = section.id.name) {
                Column(Modifier.padding(bottom = Space.x6)) { Section(section, layout, vm, nav) }
            }
        }
    }
}

@Composable
private fun Section(section: HomeSection, layout: HomeLayout, vm: HomeViewModel, nav: NavController) {
    val app = LocalApp.current
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val albums by app.library.albums.collectAsStateWithLifecycle()
    val artists by app.library.artists.collectAsStateWithLifecycle()
    val playlists by app.library.playlists.collectAsStateWithLifecycle()
    val unknownArtist = stringResource(R.string.unknown_artist)
    val unknownAlbum = stringResource(R.string.unknown_album)
    fun openAlbum(id: Long) = nav.navigate(Routes.collection(CollectionType.ALBUM, id.toString()))
    fun albumSongs(id: Long) = songs.filter { it.albumId == id }.sortedWith(compareBy({ it.disc }, { it.track }))
    fun songItems(list: List<Song>, metric: (Song) -> String = { "" }) = list.mapIndexed { i, s ->
        HomeItem("s${s.id}", s.title, s.artist.ifEmpty { unknownArtist }, s.albumId, s.album.ifEmpty { s.title }, { app.play(list, i) }, { app.play(list, i) }, metric(s), song = s)
    }
    fun albumItems(list: List<Album>) = list.map { a ->
        HomeItem("a${a.id}", a.title.ifEmpty { unknownAlbum }, a.artist.ifEmpty { unknownArtist }, a.id, a.title, { openAlbum(a.id) }, { app.play(albumSongs(a.id), 0) },
            sharedKey = com.rostomusic.app.core.design.coverKey(CollectionType.ALBUM, a.id.toString()))
    }
    val playsLabel = @Composable { n: Int -> pluralStringResource(R.plurals.plays_count, n, n) }

    when (section.id) {
        SectionId.HERO -> Hero(vm, nav)
        SectionId.SHORTCUTS -> Shortcuts(layout.shortcuts, section.style, nav)
        SectionId.PINNED -> Pinned(layout.pins, nav)
        SectionId.DAILY_MIX -> DailyMix(nav)
        SectionId.DISCOVER -> Discover(nav)
        SectionId.ARTIST_SPOTLIGHT -> Spotlight(vm, nav)
        SectionId.STATS -> Stats(vm, nav)
        SectionId.ALBUM_WALL -> AlbumWall(nav)
        SectionId.RECENT_PLAYED -> {
            val list = remember(songs) { songs.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(12) }
            if (list.isEmpty()) return
            SectionHeader(
                stringResource(R.string.section_recently_played), onAddAll = { app.enqueue(list) },
                onSeeAll = { nav.navigate(Routes.collection(CollectionType.RECENT_PLAYED)) },
            )
            StyledItems(songItems(list), section.style)
        }
        SectionId.RECENT_ADDED -> {
            val list = remember(albums) { albums.sortedByDescending { it.dateAdded }.take(12) }
            SectionHeader(
                stringResource(R.string.section_recently_added),
                onAddAll = { app.enqueue(songs.sortedByDescending { it.dateAdded }.take(50)) },
                onSeeAll = { nav.navigate(Routes.collection(CollectionType.RECENT_ADDED)) },
            )
            StyledItems(albumItems(list), section.style)
        }
        SectionId.MOST_PLAYED -> {
            val list = remember(songs) { songs.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(12) }
            if (list.isEmpty()) return
            SectionHeader(
                stringResource(R.string.section_most_played), onAddAll = { app.enqueue(list) },
                onSeeAll = { nav.navigate(Routes.collection(CollectionType.MOST_PLAYED)) },
            )
            val metrics = list.associate { it.id to playsLabel(it.playCount) }
            StyledItems(songItems(list) { metrics[it.id].orEmpty() }, section.style)
        }
        SectionId.PLAYLISTS -> {
            val items = if (playlists.isNotEmpty()) playlists.map { p ->
                val ids = p.songIds
                val first = remember(p, songs) { app.library.songsOf(ids.take(1)).firstOrNull() }
                HomeItem(
                    "p${p.id}", p.name, pluralStringResource(R.plurals.songs_count, ids.size, ids.size), first?.albumId ?: -1, p.name,
                    { nav.navigate(Routes.collection(CollectionType.PLAYLIST, p.id.toString())) }, { app.play(app.library.songsOf(ids), 0) },
                    sharedKey = com.rostomusic.app.core.design.coverKey(CollectionType.PLAYLIST, p.id.toString()),
                    artSong = first,
                )
            } else autoLists(nav)
            SectionHeader(stringResource(R.string.tab_playlists), onSeeAll = { nav.navigate(Routes.library(5)) })
            StyledItems(items, section.style)
        }
        SectionId.ARTISTS -> {
            val list = remember(artists) { artists.filter { it.name.isNotEmpty() }.sortedByDescending { it.songCount }.take(12) }
            if (list.isEmpty()) return
            val byArtist = remember(songs) { songs.groupBy { it.artist } }
            SectionHeader(stringResource(R.string.section_your_artists), onSeeAll = { nav.navigate(Routes.ARTISTS) })
            StyledItems(
                list.map { a ->
                    HomeItem(
                        "r${a.name}", a.name, pluralStringResource(R.plurals.songs_count, a.songCount, a.songCount), a.albumId, a.name,
                        { nav.navigate(Routes.collection(CollectionType.ARTIST, a.name)) },
                        sharedKey = com.rostomusic.app.core.design.coverKey(CollectionType.ARTIST, a.name),
                        artSong = byArtist[a.name]?.firstOrNull(),
                    )
                },
                section.style, round = true,
            )
        }
    }
}

@Composable
private fun autoLists(nav: NavController): List<HomeItem> {
    val app = LocalApp.current
    val songs by app.library.songs.collectAsStateWithLifecycle()
    return listOf(
        Triple(CollectionType.FAVORITES, R.string.favorites, songs.filter { it.favorite }),
        Triple(CollectionType.DAILY_MIX, R.string.daily_mix, app.library.dailyMix(songs)),
        Triple(CollectionType.NEVER_PLAYED, R.string.never_played, songs.filter { it.playCount == 0 }),
    ).filter { it.third.isNotEmpty() }.map { (type, label, list) ->
        val name = stringResource(label)
        HomeItem(type, name, pluralStringResource(R.plurals.songs_count, list.size, list.size), list.first().albumId, name, { nav.navigate(Routes.collection(type)) }, { app.play(list, 0) },
            sharedKey = com.rostomusic.app.core.design.coverKey(type, ""), artSong = list.first())
    }
}

// Fixed sections

@Composable
private fun Hero(vm: HomeViewModel, nav: NavController) {
    val app = LocalApp.current
    val c = Rosto.colors
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val albums by app.library.albums.collectAsStateWithLifecycle()
    val state by app.playerState.collectAsStateWithLifecycle()
    val week by vm.week.collectAsStateWithLifecycle()
    val unknownArtist = stringResource(R.string.unknown_artist)
    val unknownAlbum = stringResource(R.string.unknown_album)
    val recentPlayed = remember(songs) { songs.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(12) }
    val dailyMix = remember(songs) { app.library.dailyMix(songs) }
    val latest = remember(albums) { albums.maxByOrNull { it.dateAdded } }
    val pages = buildList {
        val resume = state.song ?: recentPlayed.firstOrNull()
        if (resume != null) add(HeroPage(
            R.string.hero_resume, resume.title, resume.artist.ifEmpty { unknownArtist }, resume.albumId, resume.album.ifEmpty { resume.title },
            onOpen = { if (state.song != null) app.expandPlayer() else app.play(recentPlayed, 0) },
            onPlay = { if (state.song != null) app.player.toggle() else app.play(recentPlayed, 0) },
            song = resume,
        ))
        dailyMix.firstOrNull()?.let { first ->
            add(HeroPage(
                R.string.hero_daily_mix, stringResource(R.string.daily_mix), pluralStringResource(R.plurals.songs_count, dailyMix.size, dailyMix.size),
                first.albumId, first.album.ifEmpty { first.title },
                onOpen = { nav.navigate(Routes.collection(CollectionType.DAILY_MIX)) }, onPlay = { app.play(dailyMix, 0) },
            ))
        }
        latest?.let { album ->
            add(HeroPage(
                R.string.hero_recently_added, album.title.ifEmpty { unknownAlbum }, album.artist.ifEmpty { unknownArtist }, album.id, album.title,
                onOpen = { nav.navigate(Routes.collection(CollectionType.ALBUM, album.id.toString())) },
                onPlay = { app.play(songs.filter { it.albumId == album.id }.sortedWith(compareBy({ it.disc }, { it.track })), 0) },
            ))
        }
    }
    // One wide card: no decorative stat tiles beside it (the minutes live in Statistics).
    HeroCarousel(pages, Modifier.padding(horizontal = Space.screen).fillMaxWidth().height(280.dp))
}

@Composable
private fun HeroCarousel(pages: List<HeroPage>, modifier: Modifier = Modifier) {
    val c = Rosto.colors
    if (pages.isEmpty()) {
        BentoCard(modifier) {}
        return
    }
    val pager = rememberPagerState { pages.size }
    BentoCard(modifier) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { index ->
            val page = pages[index]
            Column(Modifier.fillMaxSize().clickable(role = Role.Button, onClick = page.onOpen)) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (page.song != null) CoverArt(page.song, Modifier.fillMaxSize(), radius = 0.dp) else CoverArt(page.albumId, page.seed, Modifier.fillMaxSize(), radius = 0.dp)
                    if (pages.size > 1) {
                        Row(
                            Modifier.align(Alignment.TopStart).padding(Space.x3).clip(CircleShape).background(c.card).padding(horizontal = Space.x2, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(Space.x1),
                        ) {
                            repeat(pages.size) { i ->
                                Box(Modifier.size(6.dp).clip(CircleShape).background(if (i == index) c.textPrimary else c.textMuted.copy(alpha = 0.4f)))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = Space.x4, end = Space.x3, top = Space.x3, bottom = Space.x3), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(page.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(page.subtitle, style = Rosto.type.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val description = stringResource(R.string.cd_play_item, page.title)
                    Box(
                        Modifier.size(Space.touch).clip(CircleShape).background(c.accent)
                            .clickable(role = Role.Button, onClick = page.onPlay)
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.Center,
                    ) { RIcon(R.drawable.ic_play_filled, null, tint = c.onAccent, size = 18.dp) }
                }
            }
        }
    }
}

private fun shortcutIcon(id: ShortcutId) = when (id) {
    ShortcutId.FAVORITES -> R.drawable.ic_heart
    ShortcutId.RECENT -> R.drawable.ic_clock
    ShortcutId.MOST_PLAYED -> R.drawable.ic_trending_up
    ShortcutId.NEVER_PLAYED -> R.drawable.ic_eye_off
    ShortcutId.DAILY_MIX -> R.drawable.ic_shuffle
    ShortcutId.ALBUMS -> R.drawable.ic_disc_3
    ShortcutId.ARTISTS -> R.drawable.ic_user_round
    ShortcutId.STATS -> R.drawable.ic_chart_column
}

fun shortcutLabel(id: ShortcutId) = when (id) {
    ShortcutId.FAVORITES -> R.string.favorites
    ShortcutId.RECENT -> R.string.shortcut_recent
    ShortcutId.MOST_PLAYED -> R.string.shortcut_most_played
    ShortcutId.NEVER_PLAYED -> R.string.shortcut_never_played
    ShortcutId.DAILY_MIX -> R.string.daily_mix
    ShortcutId.ALBUMS -> R.string.tab_albums
    ShortcutId.ARTISTS -> R.string.tab_artists
    ShortcutId.STATS -> R.string.stats
}

private fun openShortcut(id: ShortcutId, nav: NavController) = when (id) {
    ShortcutId.FAVORITES -> nav.navigate(Routes.collection(CollectionType.FAVORITES))
    ShortcutId.RECENT -> nav.navigate(Routes.collection(CollectionType.RECENT_PLAYED))
    ShortcutId.MOST_PLAYED -> nav.navigate(Routes.collection(CollectionType.MOST_PLAYED))
    ShortcutId.NEVER_PLAYED -> nav.navigate(Routes.collection(CollectionType.NEVER_PLAYED))
    ShortcutId.DAILY_MIX -> nav.navigate(Routes.collection(CollectionType.DAILY_MIX))
    ShortcutId.ALBUMS -> nav.navigate(Routes.ALBUMS)
    ShortcutId.ARTISTS -> nav.navigate(Routes.ARTISTS)
    ShortcutId.STATS -> nav.navigate(Routes.STATS)
}

@Composable
private fun Shortcuts(ids: List<ShortcutId>, style: SectionStyle, nav: NavController) {
    val c = Rosto.colors
    val app = LocalApp.current
    val songs by app.library.songs.collectAsStateWithLifecycle()
    if (ids.isEmpty()) return
    if (style == SectionStyle.TILES) {
        Column(Modifier.padding(horizontal = Space.screen), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val wide = ids.take(2)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                wide.forEach { id ->
                    val count = when (id) {
                        ShortcutId.FAVORITES -> songs.count { it.favorite }
                        ShortcutId.NEVER_PLAYED -> songs.count { it.playCount == 0 }
                        ShortcutId.DAILY_MIX -> minOf(30, songs.size)
                        else -> -1
                    }
                    Row(
                        Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(Radius.tile)).background(c.card)
                            .clickable(role = Role.Button) { openShortcut(id, nav) }.padding(horizontal = Space.x4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RIcon(shortcutIcon(id), null, tint = c.textPrimary, size = 22.dp)
                        Spacer(Modifier.width(Space.x3))
                        Column {
                            Text(stringResource(shortcutLabel(id)), style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1)
                            if (count >= 0) Text(pluralStringResource(R.plurals.songs_count, count, count), style = Rosto.type.caption, color = c.textSecondary)
                        }
                    }
                }
                if (wide.size == 1) Spacer(Modifier.weight(1f))
            }
            ids.drop(2).chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { id ->
                        Column(
                            Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(Radius.tile)).background(c.card)
                                .clickable(role = Role.Button) { openShortcut(id, nav) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            RIcon(shortcutIcon(id), null, tint = c.textPrimary, size = 20.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(shortcutLabel(id)), style = Rosto.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    } else {
        // Rectangular tiles, icon and name side by side: no icons in circles.
        Column(Modifier.padding(horizontal = Space.screen), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
            ids.take(4).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                    pair.forEach { id ->
                        val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        Row(
                            Modifier.weight(1f).pressScale(press).height(56.dp).clip(RoundedCornerShape(Radius.tile)).background(c.card)
                                .clickable(press, androidx.compose.foundation.LocalIndication.current, role = Role.Button) { openShortcut(id, nav) }
                                .padding(horizontal = Space.x4),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RIcon(shortcutIcon(id), null, tint = c.textPrimary, size = 22.dp)
                            Spacer(Modifier.width(Space.x3))
                            Text(stringResource(shortcutLabel(id)), style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class PinUi(val key: String, val type: String, val title: String, val subtitle: String, val covers: List<Song>, val open: () -> Unit)

@Composable
private fun Pinned(pins: List<String>, nav: NavController) {
    val app = LocalApp.current
    val c = Rosto.colors
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val playlists by app.library.playlists.collectAsStateWithLifecycle()
    val labels = mapOf(
        CollectionType.FAVORITES to stringResource(R.string.favorites), CollectionType.DAILY_MIX to stringResource(R.string.daily_mix),
        CollectionType.RECENT_PLAYED to stringResource(R.string.section_recently_played), CollectionType.RECENT_ADDED to stringResource(R.string.section_recently_added),
        CollectionType.MOST_PLAYED to stringResource(R.string.section_most_played), CollectionType.NEVER_PLAYED to stringResource(R.string.never_played),
    )
    val kinds = mapOf(
        CollectionType.ALBUM to stringResource(R.string.kind_album), CollectionType.ARTIST to stringResource(R.string.kind_artist),
        CollectionType.PLAYLIST to stringResource(R.string.kind_playlist), CollectionType.GENRE to stringResource(R.string.kind_genre),
        CollectionType.FOLDER to stringResource(R.string.kind_folder),
    )
    val songsCount = @Composable { n: Int -> pluralStringResource(R.plurals.songs_count, n, n) }
    val items = pins.mapNotNull { pin ->
        val type = pin.substringBefore(':')
        val key = pin.substringAfter(':', "")
        val list = remember(pin, songs, playlists) { app.library.collectionNow(type, key) }
        if (list.isEmpty() && type != CollectionType.PLAYLIST) return@mapNotNull null
        val title = when (type) {
            CollectionType.ALBUM -> list.firstOrNull()?.album.orEmpty()
            CollectionType.PLAYLIST -> playlists.firstOrNull { it.id == key.toLongOrNull() }?.name ?: return@mapNotNull null
            CollectionType.FOLDER -> key.substringAfterLast('/')
            else -> labels[type] ?: key
        }
        val subtitle = listOfNotNull(kinds[type], songsCount(list.size)).joinToString(" · ")
        PinUi(pin, type, title, subtitle, list.distinctBy { it.albumId }.take(4)) { nav.navigate(Routes.collection(type, key)) }
    }
    SectionHeader(stringResource(R.string.section_pinned))
    if (items.isEmpty()) {
        Text(
            stringResource(R.string.pinned_empty), style = Rosto.type.body, color = c.textSecondary,
            modifier = Modifier.padding(horizontal = Space.screen).fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(c.card).padding(Space.x5),
        )
        return
    }
    Row(Modifier.padding(horizontal = Space.screen).fillMaxWidth().height(312.dp), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
        val first = items[0]
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(Radius.tile)).background(c.card)
                .clickable(role = Role.Button, onClick = first.open).padding(Space.x4),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                first.covers.chunked(2).take(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { CoverArt(it, Modifier.weight(1f).height(64.dp), Radius.small) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Column {
                Text(first.title, style = Rosto.type.heading, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(first.subtitle, style = Rosto.type.label, color = c.textSecondary, maxLines = 1)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
            for (i in 1..2) {
                val pin = items.getOrNull(i)
                if (pin == null) Spacer(Modifier.weight(1f))
                else if (pin.type == CollectionType.ARTIST) {
                    Row(
                        Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(c.card)
                            .clickable(role = Role.Button, onClick = pin.open).padding(Space.x3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        pin.covers.firstOrNull()?.let { CoverArt(it, Modifier.size(com.rostomusic.app.core.design.CoverSize.l)) } ?: CoverArt(-1, pin.title, Modifier.size(com.rostomusic.app.core.design.CoverSize.l))
                        Spacer(Modifier.width(Space.x3))
                        Column(Modifier.weight(1f)) {
                            Text(pin.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(pin.subtitle, style = Rosto.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).clickable(role = Role.Button, onClick = pin.open)) {
                        pin.covers.firstOrNull()?.let { CoverArt(it, Modifier.fillMaxSize(), radius = 0.dp) } ?: CoverArt(-1, pin.title, Modifier.fillMaxSize(), radius = 0.dp)
                        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(c.card).padding(horizontal = Space.x3, vertical = Space.x2)) {
                            Text(pin.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pin.subtitle, style = Rosto.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
    val rest = items.drop(3)
    if (rest.isNotEmpty()) {
        Spacer(Modifier.height(Space.x3))
        StyledItems(rest.map { HomeItem(it.key, it.title, it.subtitle, -1, it.title, it.open, artSong = it.covers.firstOrNull()) }, SectionStyle.WIDE)
    }
}

@Composable
private fun DailyMix(nav: NavController) {
    val app = LocalApp.current
    val c = Rosto.colors
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val mix = remember(songs) { app.library.dailyMix(songs) }
    if (mix.isEmpty()) return
    val covers = remember(mix) { mix.distinctBy { it.albumId }.take(3) }
    Column(Modifier.padding(horizontal = Space.screen), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(c.card)
                .clickable(role = Role.Button) { nav.navigate(Routes.collection(CollectionType.DAILY_MIX)) }.padding(Space.x4),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(192.dp), horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                covers.getOrNull(0)?.let { CoverArt(it, Modifier.weight(2f).fillMaxHeight()) } ?: CoverArt(-1, "mix", Modifier.weight(2f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                    covers.getOrNull(1)?.let { CoverArt(it, Modifier.weight(1f).fillMaxWidth()) } ?: CoverArt(-1, "mix1", Modifier.weight(1f).fillMaxWidth())
                    covers.getOrNull(2)?.let { CoverArt(it, Modifier.weight(1f).fillMaxWidth()) } ?: CoverArt(-1, "mix2", Modifier.weight(1f).fillMaxWidth())
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.daily_mix), style = Rosto.type.title, color = c.textPrimary)
                    Text(
                        pluralStringResource(R.plurals.songs_count, mix.size, mix.size) + " · " + formatDuration(mix.sumOf { it.duration }),
                        style = Rosto.type.label, color = c.textSecondary,
                    )
                }
                IconBtn(R.drawable.ic_shuffle, stringResource(R.string.shuffle), { app.shuffle(mix) }, container = c.surfaceAlt)
                Spacer(Modifier.width(Space.x2))
                IconBtn(R.drawable.ic_play_filled, stringResource(R.string.cd_play_item, stringResource(R.string.daily_mix)), { app.play(mix, 0) }, tint = c.onAccent, container = c.accent, size = 56.dp, iconSize = 20.dp)
            }
        }
        val weekAgo = remember { System.currentTimeMillis() - 7 * 86_400_000L }
        Row(Modifier.height(84.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile(null, songs.count { it.playCount == 0 }.toString(), stringResource(R.string.never_played), Modifier.weight(1f).fillMaxHeight()) {
                nav.navigate(Routes.collection(CollectionType.NEVER_PLAYED))
            }
            InfoTile(null, songs.count { it.favorite }.toString(), stringResource(R.string.favorites), Modifier.weight(1f).fillMaxHeight()) {
                nav.navigate(Routes.collection(CollectionType.FAVORITES))
            }
            InfoTile(null, songs.count { it.dateAdded >= weekAgo }.toString(), stringResource(R.string.added_this_week), Modifier.weight(1f).fillMaxHeight()) {
                nav.navigate(Routes.collection(CollectionType.RECENT_ADDED))
            }
        }
    }
}

@Composable
private fun Discover(nav: NavController) {
    val app = LocalApp.current
    val c = Rosto.colors
    val songs by app.library.songs.collectAsStateWithLifecycle()
    val albums by app.library.albums.collectAsStateWithLifecycle()
    val genres by app.library.genres.collectAsStateWithLifecycle()
    val never = remember(songs) { songs.filter { it.playCount == 0 }.sortedByDescending { it.dateAdded } }
    val day = System.currentTimeMillis() / 86_400_000L
    val genre = remember(genres, day) { genres.filter { it.songCount >= 3 }.ifEmpty { genres }.let { if (it.isEmpty()) null else it[Random(day).nextInt(it.size)] } }
    SectionHeader(stringResource(R.string.section_discover))
    Column(Modifier.padding(horizontal = Space.screen), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
        Row(Modifier.fillMaxWidth().height(268.dp), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(Radius.tile)).background(c.card)
                    .clickable(role = Role.Button) { nav.navigate(Routes.collection(CollectionType.NEVER_PLAYED)) }.padding(Space.x4),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.fillMaxWidth().height(140.dp)) {
                    val ids = never.distinctBy { it.albumId }.take(3)
                    ids.getOrNull(2)?.let { CoverArt(it, Modifier.size(com.rostomusic.app.core.design.CoverSize.l).offset(0.dp, 20.dp).rotate(-8f), radius = Radius.cover) }
                    ids.getOrNull(1)?.let { CoverArt(it, Modifier.size(com.rostomusic.app.core.design.CoverSize.l).offset(30.dp, 6.dp).rotate(4f), radius = Radius.cover) }
                    ids.getOrNull(0)?.let { CoverArt(it, Modifier.size(com.rostomusic.app.core.design.CoverSize.l).offset(56.dp, 32.dp), radius = Radius.cover) }
                }
                Column {
                    Text(stringResource(R.string.to_discover), style = Rosto.type.heading, color = c.textPrimary)
                    Text(pluralStringResource(R.plurals.never_played_count, never.size, never.size), style = Rosto.type.label, color = c.textSecondary)
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
                InfoTile(
                    R.drawable.ic_shuffle, stringResource(R.string.random_album), stringResource(R.string.random_album_hint),
                    Modifier.weight(1f).fillMaxWidth(),
                ) {
                    if (albums.isNotEmpty()) nav.navigate(Routes.collection(CollectionType.ALBUM, albums.random().id.toString()))
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(c.card)
                        .clickable(enabled = genre != null, role = Role.Button) { genre?.let { nav.navigate(Routes.collection(CollectionType.GENRE, it.name)) } }
                        .padding(Space.x4),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (genre != null) Badge(genre.name, hueSeed = genre.name)
                    Column {
                        Text(stringResource(R.string.genre_of_day), style = Rosto.type.bodyStrong, color = c.textPrimary)
                        Text(
                            genre?.let { pluralStringResource(R.plurals.songs_count, it.songCount, it.songCount) } ?: stringResource(R.string.no_genre),
                            style = Rosto.type.caption, color = c.textSecondary,
                        )
                    }
                }
            }
        }
        if (never.isNotEmpty()) {
            Text(stringResource(R.string.added_not_played), style = Rosto.type.heading, color = c.textPrimary, modifier = Modifier.padding(top = Space.x2))
            never.take(3).forEachIndexed { i, s ->
                val now = com.rostomusic.app.core.design.LocalNowPlaying.current
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(com.rostomusic.app.core.design.Radius.image))
                        .background(if (now.songId == s.id) c.accent.copy(alpha = com.rostomusic.app.core.design.Alpha.NOW_PLAYING) else Color.Transparent)
                        .clickable { app.play(never, i) }.padding(start = if (now.songId == s.id) Space.x3 else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp)) {
                        CoverArt(s, Modifier.matchParentSize(), Radius.small)
                        if (now.songId == s.id) com.rostomusic.app.core.design.NowPlayingOverlay(now.playing, Modifier.matchParentSize().clip(RoundedCornerShape(Radius.image)))
                    }
                    Spacer(Modifier.width(Space.x3))
                    Column(Modifier.weight(1f)) {
                        Text(s.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${s.artist.ifEmpty { stringResource(R.string.unknown_artist) }} · ${formatDuration(s.duration)}", style = Rosto.type.label, color = c.textSecondary, maxLines = 1)
                    }
                    IconBtn(R.drawable.ic_plus, stringResource(R.string.add_to_queue), { app.enqueue(listOf(s)) }, tint = c.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun Spotlight(vm: HomeViewModel, nav: NavController) {
    val c = Rosto.colors
    val top by vm.topArtist.collectAsStateWithLifecycle()
    val image by vm.topArtistImage.collectAsStateWithLifecycle()
    val artist = top ?: return
    Box(
        Modifier.padding(horizontal = Space.screen).fillMaxWidth().height(240.dp).clip(RoundedCornerShape(Radius.tile))
            .clickable(role = Role.Button) { nav.navigate(Routes.collection(CollectionType.ARTIST, artist.name)) },
    ) {
        val artistSong = LocalApp.current.library.songs.collectAsStateWithLifecycle().value.firstOrNull { it.artist == artist.name }
        if (artistSong != null) CoverArt(artistSong, Modifier.fillMaxSize(), radius = 0.dp) else CoverArt(-1, artist.name, Modifier.fillMaxSize(), radius = 0.dp)
        image?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to c.background)))
        Column(Modifier.align(Alignment.BottomStart).padding(Space.x5)) {
            Text(artist.name, style = Rosto.type.display, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.songs_count, artist.songCount, artist.songCount) + " · " + listenLabel(artist.listenedMs),
                style = Rosto.type.label, color = c.textSecondary,
            )
        }
    }
}

@Composable
private fun listenLabel(ms: Long): String {
    val minutes = (ms / 60_000).toInt()
    return if (minutes >= 60) stringResource(R.string.hours_minutes, minutes / 60, minutes % 60) else pluralStringResource(R.plurals.minutes_count, minutes, minutes)
}

@Composable
private fun Stats(vm: HomeViewModel, nav: NavController) {
    val c = Rosto.colors
    val week by vm.week.collectAsStateWithLifecycle()
    val top by vm.topArtist.collectAsStateWithLifecycle()
    val recent by vm.recentPlays.collectAsStateWithLifecycle()
    val dayFormat = remember { java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()) }
    Column(Modifier.padding(horizontal = Space.screen), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
        BentoCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.STATS) }) {
            Column(Modifier.padding(Space.x4)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.stats_week), style = Rosto.type.label, color = c.textSecondary)
                        Text(listenLabel(week.weekMs), style = Rosto.type.title, color = c.textPrimary)
                    }
                    val diff = ((week.weekMs - week.previousWeekMs) / 60_000).toInt()
                    if (week.previousWeekMs > 0) {
                        Text(stringResource(R.string.vs_last_week, if (diff >= 0) "+$diff" else "$diff"), style = Rosto.type.caption, color = c.textMuted)
                    }
                }
                Spacer(Modifier.height(Space.x4))
                val max = (week.days.maxOrNull() ?: 0).coerceAtLeast(1)
                val summary = week.days.joinToString { "$it min" }
                Canvas(Modifier.fillMaxWidth().height(84.dp).semantics { contentDescription = summary }) {
                    val slot = size.width / week.days.size
                    val w = slot * 0.55f
                    week.days.forEachIndexed { i, m ->
                        val h = (size.height * m / max).coerceAtLeast(6f)
                        drawRoundRect(
                            if (i == week.days.lastIndex) c.textPrimary else c.surfaceAlt,
                            topLeft = Offset(slot * i + (slot - w) / 2, size.height - h), size = Size(w, h), cornerRadius = CornerRadius(w / 4, w / 4),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    week.dayStarts.forEachIndexed { i, start ->
                        Text(
                            dayFormat.format(java.util.Date(start)).trimEnd('.'), style = Rosto.type.caption,
                            color = if (i == week.dayStarts.lastIndex) c.textPrimary else c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Row(Modifier.height(88.dp), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
            Row(
                Modifier.weight(2f).fillMaxHeight().clip(RoundedCornerShape(Radius.tile)).background(c.card)
                    .clickable(enabled = top != null, role = Role.Button) { top?.let { nav.navigate(Routes.collection(CollectionType.ARTIST, it.name)) } }
                    .padding(Space.x3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val topSong = LocalApp.current.library.songs.collectAsStateWithLifecycle().value.firstOrNull { it.artist == top?.name }
                if (topSong != null) CoverArt(topSong, Modifier.size(com.rostomusic.app.core.design.CoverSize.l)) else CoverArt(-1, top?.name.orEmpty(), Modifier.size(com.rostomusic.app.core.design.CoverSize.l))
                Spacer(Modifier.width(Space.x3))
                Column {
                    Text(stringResource(R.string.artist_of_week), style = Rosto.type.caption, color = c.textMuted)
                    Text(top?.name ?: "—", style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            InfoTile(null, (week.todayMs / 60_000).toString(), stringResource(R.string.minutes_today), Modifier.weight(1f).fillMaxHeight())
        }
        if (recent.isNotEmpty()) {
            Text(stringResource(R.string.stats_history), style = Rosto.type.heading, color = c.textPrimary, modifier = Modifier.padding(top = Space.x2))
            recent.forEach { r ->
                val now = com.rostomusic.app.core.design.LocalNowPlaying.current
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(com.rostomusic.app.core.design.Radius.image))
                        .background(if (now.songId == r.song.id) c.accent.copy(alpha = com.rostomusic.app.core.design.Alpha.NOW_PLAYING) else Color.Transparent)
                        .padding(start = if (now.songId == r.song.id) Space.x3 else 0.dp, end = if (now.songId == r.song.id) Space.x3 else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp)) {
                        CoverArt(r.song, Modifier.matchParentSize(), Radius.small)
                        if (now.songId == r.song.id) com.rostomusic.app.core.design.NowPlayingOverlay(now.playing, Modifier.matchParentSize().clip(RoundedCornerShape(Radius.image)))
                    }
                    Spacer(Modifier.width(Space.x3))
                    Column(Modifier.weight(1f)) {
                        Text(r.song.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(r.song.artist.ifEmpty { stringResource(R.string.unknown_artist) }, style = Rosto.type.label, color = c.textSecondary, maxLines = 1)
                    }
                    Text(
                        android.text.format.DateUtils.getRelativeTimeSpanString(r.playedAt, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString(),
                        style = Rosto.type.caption, color = c.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumWall(nav: NavController) {
    val app = LocalApp.current
    val c = Rosto.colors
    val albums by app.library.albums.collectAsStateWithLifecycle()
    val songs by app.library.songs.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val plays = remember(songs) { songs.groupBy { it.albumId }.mapValues { (_, s) -> s.sumOf { it.playCount } } }
    val list = remember(albums, tab, plays) {
        when (tab) {
            1 -> albums.sortedByDescending { plays[it.id] ?: 0 }
            2 -> albums.sortedBy { it.title.lowercase() }
            else -> albums.sortedByDescending { it.dateAdded }
        }.take(6)
    }
    val unknownArtist = stringResource(R.string.unknown_artist)
    LazyRow(contentPadding = PaddingValues(horizontal = Space.screen), horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
        items(listOf(R.string.wall_recent, R.string.shortcut_most_played, R.string.wall_az).withIndex().toList()) { (i, label) ->
            com.rostomusic.app.core.design.Pill(stringResource(label), tab == i, { tab = i })
        }
    }
    Spacer(Modifier.height(Space.x3))
    StyledItems(
        list.map { a ->
            HomeItem("w${a.id}", a.title.ifEmpty { stringResource(R.string.unknown_album) }, a.artist.ifEmpty { unknownArtist }, a.id, a.title, {
                nav.navigate(Routes.collection(CollectionType.ALBUM, a.id.toString()))
            })
        },
        SectionStyle.STAGGERED,
    )
    if (list.isEmpty()) Text(stringResource(R.string.empty_library_title), style = Rosto.type.body, color = c.textSecondary, modifier = Modifier.padding(Space.screen))
}

