package com.rostomusic.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rostomusic.app.R
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.sharedCover
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.domain.SectionStyle

/** One thing a home section shows: a song, an album, a playlist or an artist. */
data class HomeItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val albumId: Long,
    val seed: String,
    val onClick: () -> Unit,
    val onPlay: (() -> Unit)? = null,
    /** Extra line for the ranked style, e.g. "31 écoutes". */
    val metric: String = "",
    /** Set for a song, which then shows its own cover. */
    val song: com.rostomusic.app.data.db.Song? = null,
    /** Lets the cover grow into the page it opens. */
    val sharedKey: String? = null,
    /** A song whose own cover stands for this item (artists, playlists), so no other song's picture is borrowed. */
    val artSong: com.rostomusic.app.data.db.Song? = null,
)

/** Draws [items] in the shape the user picked. Each shape looks different on purpose. */
@Composable
fun StyledItems(items: List<HomeItem>, style: SectionStyle, round: Boolean = false) {
    if (items.isEmpty()) return
    when (style) {
        SectionStyle.MOSAIC -> Mosaic(items, round)
        SectionStyle.WIDE -> WideRow(items, round)
        SectionStyle.PORTRAIT -> PortraitRow(items)
        SectionStyle.RANKED -> Ranked(items, round)
        SectionStyle.LIST -> CompactList(items, round)
        SectionStyle.GRID -> SmallGrid(items, round)
        SectionStyle.LARGE -> SquareRow(items, com.rostomusic.app.core.design.CoverSize.xl, round)
        SectionStyle.STAGGERED -> Staggered(items)
        SectionStyle.CIRCLES -> ArtistCards(items)
        SectionStyle.ARTIST_BENTO -> ArtistBento(items)
        else -> WideRow(items, round)
    }
}

private val edge = PaddingValues(horizontal = Space.screen)

@Composable
private fun Cover(item: HomeItem, modifier: Modifier, radius: Dp, round: Boolean = false) {
    val now = com.rostomusic.app.core.design.LocalNowPlaying.current
    val art = item.song ?: item.artSong
    Box(Modifier.sharedCover(item.sharedKey).then(modifier)) {
        if (art != null) CoverArt(art, Modifier.matchParentSize(), radius = if (round) 400.dp else radius)
        else CoverArt(item.albumId, item.seed, Modifier.matchParentSize(), radius = if (round) 400.dp else radius)
        // The playing song shows the moving bars wherever it appears.
        if (item.song != null && item.song.id == now.songId) {
            com.rostomusic.app.core.design.NowPlayingOverlay(
                now.playing,
                Modifier.matchParentSize().clip(RoundedCornerShape(if (radius == 0.dp) 0.dp else Radius.image)),
            )
        }
    }
}

@Composable
private fun isNow(item: HomeItem): Boolean = item.song != null && item.song.id == com.rostomusic.app.core.design.LocalNowPlaying.current.songId

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TwoLines(item: HomeItem, modifier: Modifier = Modifier, center: Boolean = false, onImage: Boolean = false) {
    val c = Rosto.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = if (onImage) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (item.subtitle.isNotEmpty()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onImage) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (center) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** One large tile, one wide card and two small covers. */
@Composable
private fun Mosaic(items: List<HomeItem>, round: Boolean) {
    val c = Rosto.colors
    Row(Modifier.padding(edge).fillMaxWidth().height(166.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val big = items[0]
        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(Radius.tile)).clickable(role = Role.Button, onClick = big.onClick)) {
            Cover(big, Modifier.fillMaxSize(), 20.dp)
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(c.card).padding(horizontal = Space.x3, vertical = Space.x2)) {
                TwoLines(big)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.getOrNull(1)?.let { item ->
                Row(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(c.card)
                        .clickable(role = Role.Button, onClick = item.onClick).padding(Space.x2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(item, Modifier.fillMaxHeight().aspectRatio(1f), Radius.small, round)
                    Spacer(Modifier.width(Space.x2))
                    TwoLines(item, Modifier.weight(1f))
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 2..3) {
                    val item = items.getOrNull(i)
                    if (item == null) Spacer(Modifier.weight(1f))
                    else Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(Radius.tile))
                            .clickable(role = Role.Button, onClick = item.onClick)
                            .semantics { contentDescription = item.title },
                    ) { Cover(item, Modifier.fillMaxSize(), 16.dp, round) }
                }
            }
        }
    }
}

@Composable
private fun WideRow(items: List<HomeItem>, round: Boolean) {
    val c = Rosto.colors
    LazyRow(contentPadding = edge, horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
        items(items, key = { it.key }) { item ->
            Row(
                Modifier.width(250.dp).height(96.dp).clip(RoundedCornerShape(Radius.tile)).background(c.card)
                    .clickable(role = Role.Button, onClick = item.onClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(item, Modifier.size(96.dp), if (round) 400.dp else 0.dp, round)
                TwoLines(item, Modifier.weight(1f).padding(horizontal = Space.x4))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitRow(items: List<HomeItem>) {
    val carouselState = rememberCarouselState { items.size }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 156.dp,
        itemSpacing = Space.x3,
        contentPadding = edge,
        modifier = Modifier.fillMaxWidth(),
    ) { index ->
        val item = items[index]
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = item.onClick),
        ) {
            Cover(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .maskClip(MaterialTheme.shapes.extraLarge),
                radius = 0.dp,
            )
            Spacer(Modifier.height(Space.x2))
            TwoLines(item)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SquareRow(items: List<HomeItem>, size: Dp, round: Boolean, centered: Boolean = false) {
    val carouselState = rememberCarouselState { items.size }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = size,
        itemSpacing = Space.x3,
        contentPadding = edge,
        modifier = Modifier.fillMaxWidth(),
    ) { index ->
        val item = items[index]
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = item.onClick),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Cover(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(size)
                    .maskClip(if (round) CircleShape else MaterialTheme.shapes.extraLarge),
                radius = 0.dp,
                round = round,
            )
            Spacer(Modifier.height(Space.x2))
            TwoLines(item, center = centered)
        }
    }
}

@Composable
private fun Ranked(items: List<HomeItem>, round: Boolean) {
    val c = Rosto.colors
    Column(Modifier.padding(edge)) {
        items.take(5).forEachIndexed { i, item ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(com.rostomusic.app.core.design.Radius.image))
                    .background(if (isNow(item)) c.accent.copy(alpha = com.rostomusic.app.core.design.Alpha.NOW_PLAYING) else Color.Transparent)
                    .clickable(onClick = item.onClick).padding(start = if (isNow(item)) Space.x3 else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    (i + 1).toString(), style = Rosto.type.title, color = if (i == 0) c.textPrimary else c.textMuted,
                    modifier = Modifier.width(36.dp),
                )
                Cover(item, Modifier.size(com.rostomusic.app.core.design.CoverSize.m), Radius.small, round)
                Spacer(Modifier.width(Space.x3))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(item.subtitle, item.metric).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = Rosto.type.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactList(items: List<HomeItem>, round: Boolean) {
    val c = Rosto.colors
    Column(Modifier.padding(edge), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
        items.take(8).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                pair.forEach { item ->
                    Row(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(Radius.tile)).background(c.card)
                            .clickable(role = Role.Button, onClick = item.onClick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cover(item, Modifier.size(com.rostomusic.app.core.design.CoverSize.m), if (round) 400.dp else 0.dp, round)
                        Text(
                            item.title, style = Rosto.type.label, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = Space.x3),
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SmallGrid(items: List<HomeItem>, round: Boolean) {
    val c = Rosto.colors
    Column(Modifier.padding(edge), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
        items.take(8).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                row.forEach { item ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(Radius.tile)).clickable(role = Role.Button, onClick = item.onClick)) {
                        Cover(item, Modifier.fillMaxWidth().aspectRatio(1f), 14.dp, round)
                        Spacer(Modifier.height(6.dp))
                        Text(item.title, style = Rosto.type.caption, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Staggered(items: List<HomeItem>) {
    Row(Modifier.padding(edge), horizontalArrangement = Arrangement.spacedBy(Space.x3), verticalAlignment = Alignment.Top) {
        for (col in 0..1) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.x4)) {
                items.take(6).filterIndexed { i, _ -> i % 2 == col }.forEachIndexed { row, item ->
                    val tall = (row + col) % 2 == 0
                    Column(Modifier.clip(RoundedCornerShape(Radius.tile)).clickable(role = Role.Button, onClick = item.onClick)) {
                        Cover(item, Modifier.fillMaxWidth().height(if (tall) 220.dp else 140.dp), 24.dp)
                        Spacer(Modifier.height(Space.x2))
                        TwoLines(item)
                    }
                }
            }
        }
    }
}

/** Artists as bento cards: square photo, name and count inside one card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistCards(items: List<HomeItem>) {
    val carouselState = rememberCarouselState { items.size }
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 156.dp,
        itemSpacing = Space.x3,
        contentPadding = edge,
        modifier = Modifier.fillMaxWidth(),
    ) { index ->
        val item = items[index]
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = item.onClick),
        ) {
            Cover(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .maskClip(MaterialTheme.shapes.extraLarge),
                radius = 0.dp,
            )
            Spacer(Modifier.height(Space.x2))
            TwoLines(item)
        }
    }
}

@Composable
private fun ArtistBento(items: List<HomeItem>) {
    val c = Rosto.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.x3)) {
        Row(Modifier.padding(edge).fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
            val big = items[0]
            Column(
                Modifier.weight(2f).fillMaxHeight().clip(RoundedCornerShape(Radius.tile)).background(c.card)
                    .clickable(role = Role.Button, onClick = big.onClick).padding(Space.x3),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Cover(big, Modifier.size(132.dp), 0.dp, round = true)
                Spacer(Modifier.height(Space.x3))
                Text(big.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                for (i in 1..2) items.getOrNull(i)?.let { item ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).clickable(role = Role.Button, onClick = item.onClick),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Cover(item, Modifier.size(76.dp), 0.dp, round = true)
                        Spacer(Modifier.height(6.dp))
                        Text(item.title, style = Rosto.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        val rest = items.drop(3).take(6)
        if (rest.isNotEmpty()) {
            LazyRow(contentPadding = edge, horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                items(rest, key = { it.key }) { item ->
                    Row(
                        Modifier.height(56.dp).clip(CircleShape).background(c.card).clickable(role = Role.Button, onClick = item.onClick)
                            .padding(start = 6.dp, end = Space.x4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cover(item, Modifier.size(com.rostomusic.app.core.design.CoverSize.s), 0.dp, round = true)
                        Spacer(Modifier.width(Space.x3))
                        Text(item.title, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Card with a small icon and two lines, used by the fixed sections. */
@Composable
fun InfoTile(icon: Int?, value: String, label: String, modifier: Modifier = Modifier, iconTint: Color? = null, onClick: (() -> Unit)? = null) {
    val c = Rosto.colors
    Column(
        modifier.clip(RoundedCornerShape(Radius.tile)).background(c.card)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(Space.x4),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (icon != null) RIcon(icon, null, tint = iconTint ?: c.textPrimary, size = 22.dp)
        else Text(value, style = Rosto.type.title, color = c.textPrimary, maxLines = 1)
        Column {
            if (icon != null && value.isNotEmpty()) Text(value, style = Rosto.type.bodyStrong, color = c.textPrimary, maxLines = 2)
            Text(label, style = Rosto.type.caption, color = c.textSecondary, maxLines = 2)
        }
    }
}

@Composable
fun stylesLabel(style: SectionStyle): String = stringResource(
    when (style) {
        SectionStyle.MOSAIC -> R.string.style_mosaic
        SectionStyle.WIDE -> R.string.style_wide
        SectionStyle.PORTRAIT -> R.string.style_portrait
        SectionStyle.RANKED -> R.string.style_ranked
        SectionStyle.LIST -> R.string.style_list
        SectionStyle.GRID -> R.string.style_grid
        SectionStyle.LARGE -> R.string.style_large
        SectionStyle.STAGGERED -> R.string.style_staggered
        SectionStyle.CIRCLES -> R.string.style_circles
        SectionStyle.ARTIST_BENTO -> R.string.style_artist_bento
        SectionStyle.ROUND -> R.string.style_round
        SectionStyle.TILES -> R.string.style_tiles
        SectionStyle.FIXED -> R.string.style_fixed
    }
)
