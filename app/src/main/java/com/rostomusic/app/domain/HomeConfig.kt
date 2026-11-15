package com.rostomusic.app.domain

/** Sections the home screen can show. */
enum class SectionId {
    HERO, PINNED, SHORTCUTS, DAILY_MIX, DISCOVER, ARTIST_SPOTLIGHT, STATS,
    RECENT_PLAYED, RECENT_ADDED, MOST_PLAYED, PLAYLISTS, ARTISTS, ALBUM_WALL,
}

/** Shapes a section can take. */
enum class SectionStyle {
    /** One large tile, one wide tile and two small ones. */
    MOSAIC,
    /** Horizontal row of wide cards, cover on the left. */
    WIDE,
    /** Horizontal row of tall cards. */
    PORTRAIT,
    /** Numbered list, the first number in the accent colour. */
    RANKED,
    /** Two columns of compact rows. */
    LIST,
    /** Four small covers per row. */
    GRID,
    /** Horizontal row of large squares. */
    LARGE,
    /** Two columns with alternating heights. */
    STAGGERED,
    /** Horizontal row of round pictures. */
    CIRCLES,
    /** One large round picture, two smaller ones and pills. */
    ARTIST_BENTO,
    /** Round shortcut buttons. */
    ROUND,
    /** Shortcut tiles of two widths. */
    TILES,
    /** The section's own fixed layout. */
    FIXED,
}

enum class ShortcutId { FAVORITES, RECENT, MOST_PLAYED, NEVER_PLAYED, DAILY_MIX, ALBUMS, ARTISTS, STATS }

data class HomeSection(val id: SectionId, val visible: Boolean, val style: SectionStyle)

data class HomeLayout(
    val preset: String,
    val sections: List<HomeSection>,
    val shortcuts: List<ShortcutId>,
    /** Pinned collections as "type:key", in display order. */
    val pins: List<String>,
) {
    fun section(id: SectionId): HomeSection? = sections.firstOrNull { it.id == id }
}

object HomeConfig {
    const val DEFAULT_PRESET = "default"

    /** Shapes each section can take, the first being its usual one. */
    fun stylesFor(id: SectionId): List<SectionStyle> = when (id) {
        SectionId.RECENT_PLAYED, SectionId.RECENT_ADDED, SectionId.MOST_PLAYED -> listOf(
            SectionStyle.MOSAIC, SectionStyle.WIDE, SectionStyle.PORTRAIT, SectionStyle.RANKED,
            SectionStyle.LIST, SectionStyle.GRID, SectionStyle.LARGE, SectionStyle.STAGGERED,
        )
        SectionId.PLAYLISTS -> listOf(SectionStyle.PORTRAIT, SectionStyle.LARGE, SectionStyle.WIDE, SectionStyle.LIST, SectionStyle.GRID, SectionStyle.MOSAIC)
        SectionId.ARTISTS -> listOf(SectionStyle.CIRCLES, SectionStyle.LARGE, SectionStyle.ARTIST_BENTO, SectionStyle.LIST, SectionStyle.WIDE)
        SectionId.SHORTCUTS -> listOf(SectionStyle.ROUND, SectionStyle.TILES)
        else -> listOf(SectionStyle.FIXED)
    }

    val ALL_SHORTCUTS: List<ShortcutId> = ShortcutId.entries.toList()
    private val DEFAULT_SHORTCUTS = listOf(ShortcutId.FAVORITES, ShortcutId.RECENT, ShortcutId.MOST_PLAYED, ShortcutId.DAILY_MIX)

    /** The ten starting layouts. Sections a preset does not list are added after, hidden. */
    val PRESETS: List<String> = listOf(
        "default", "pinned", "minimal", "mix", "artists", "discover", "wall", "stats", "shortcuts", "compact",
    )

    fun preset(name: String): HomeLayout {
        fun s(id: SectionId, style: SectionStyle = stylesFor(id).first()) = HomeSection(id, true, style)
        var shortcuts = DEFAULT_SHORTCUTS
        val shown: List<HomeSection> = when (name) {
            "pinned" -> listOf(
                s(SectionId.PINNED), s(SectionId.DAILY_MIX), s(SectionId.RECENT_PLAYED, SectionStyle.PORTRAIT),
                s(SectionId.RECENT_ADDED, SectionStyle.WIDE),
            )
            "minimal" -> listOf(s(SectionId.MOST_PLAYED, SectionStyle.RANKED), s(SectionId.RECENT_PLAYED, SectionStyle.LIST))
            "mix" -> listOf(s(SectionId.DAILY_MIX), s(SectionId.RECENT_ADDED, SectionStyle.LARGE), s(SectionId.MOST_PLAYED, SectionStyle.WIDE))
            "artists" -> listOf(
                s(SectionId.ARTIST_SPOTLIGHT), s(SectionId.ARTISTS, SectionStyle.ARTIST_BENTO), s(SectionId.RECENT_PLAYED, SectionStyle.LIST),
            )
            "discover" -> listOf(s(SectionId.DISCOVER), s(SectionId.RECENT_PLAYED, SectionStyle.MOSAIC))
            "wall" -> listOf(s(SectionId.ALBUM_WALL))
            "stats" -> listOf(s(SectionId.STATS), s(SectionId.MOST_PLAYED, SectionStyle.RANKED))
            "shortcuts" -> {
                shortcuts = ShortcutId.entries.toList()
                listOf(
                    s(SectionId.SHORTCUTS, SectionStyle.TILES), s(SectionId.RECENT_ADDED, SectionStyle.MOSAIC),
                    s(SectionId.MOST_PLAYED, SectionStyle.RANKED),
                )
            }
            "compact" -> listOf(
                s(SectionId.RECENT_PLAYED, SectionStyle.LIST), s(SectionId.RECENT_ADDED, SectionStyle.GRID),
                s(SectionId.MOST_PLAYED, SectionStyle.RANKED), s(SectionId.PLAYLISTS, SectionStyle.LIST),
            )
            // Two section shapes only by default: rows of covers and one ranked list.
            else -> listOf(
                s(SectionId.HERO), s(SectionId.SHORTCUTS, SectionStyle.ROUND), s(SectionId.RECENT_PLAYED, SectionStyle.LARGE),
                s(SectionId.RECENT_ADDED, SectionStyle.LARGE), s(SectionId.MOST_PLAYED, SectionStyle.RANKED),
                s(SectionId.PLAYLISTS, SectionStyle.LARGE), s(SectionId.ARTISTS, SectionStyle.LARGE),
            )
        }
        val hidden = SectionId.entries.filter { id -> shown.none { it.id == id } }
            .map { HomeSection(it, false, stylesFor(it).first()) }
        return HomeLayout(if (name in PRESETS) name else DEFAULT_PRESET, shown + hidden, shortcuts, emptyList())
    }

    /** Keeps the user's pins when another preset is applied. */
    fun applyPreset(current: HomeLayout, name: String): HomeLayout = preset(name).copy(pins = current.pins)

    fun move(layout: HomeLayout, from: Int, to: Int): HomeLayout = layout.copy(sections = QueueOps.move(layout.sections, from, to))

    fun setVisible(layout: HomeLayout, id: SectionId, visible: Boolean): HomeLayout =
        layout.copy(sections = layout.sections.map { if (it.id == id) it.copy(visible = visible) else it })

    fun setStyle(layout: HomeLayout, id: SectionId, style: SectionStyle): HomeLayout =
        if (style !in stylesFor(id)) layout
        else layout.copy(sections = layout.sections.map { if (it.id == id) it.copy(style = style) else it })

    fun toggleShortcut(layout: HomeLayout, id: ShortcutId): HomeLayout {
        val list = if (id in layout.shortcuts) layout.shortcuts - id else layout.shortcuts + id
        return layout.copy(shortcuts = list.take(9))
    }

    /** Pinning also shows the pinned section, so the pin is never invisible. */
    fun togglePin(layout: HomeLayout, pin: String): HomeLayout =
        if (pin in layout.pins) layout.copy(pins = layout.pins - pin)
        else setVisible(layout.copy(pins = layout.pins + pin), SectionId.PINNED, true)

    // Plain text storage, readable in backups and testable without Android.

    fun encode(layout: HomeLayout): String = buildString {
        append("v1\n")
        append("preset=").append(layout.preset).append('\n')
        append("sections=").append(layout.sections.joinToString(";") { "${it.id.name}:${it.style.name}:${if (it.visible) 1 else 0}" }).append('\n')
        append("shortcuts=").append(layout.shortcuts.joinToString(",") { it.name }).append('\n')
        append("pins=").append(layout.pins.joinToString("\u001F")).append('\n')
    }

    fun decode(text: String?): HomeLayout {
        if (text.isNullOrBlank() || !text.startsWith("v1")) return preset(DEFAULT_PRESET)
        val map = text.lines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }.toMap()
        val parsed = map["sections"].orEmpty().split(';').mapNotNull { part ->
            val bits = part.split(':')
            if (bits.size != 3) return@mapNotNull null
            val id = runCatching { SectionId.valueOf(bits[0]) }.getOrNull() ?: return@mapNotNull null
            val style = runCatching { SectionStyle.valueOf(bits[1]) }.getOrNull()?.takeIf { it in stylesFor(id) } ?: stylesFor(id).first()
            HomeSection(id, bits[2] == "1", style)
        }.distinctBy { it.id }
        // Sections added in later versions appear at the end, hidden.
        val sections = parsed + SectionId.entries.filter { id -> parsed.none { it.id == id } }
            .map { HomeSection(it, false, stylesFor(it).first()) }
        val shortcuts = map["shortcuts"].orEmpty().split(',').mapNotNull { runCatching { ShortcutId.valueOf(it.trim()) }.getOrNull() }.distinct()
        val pins = map["pins"].orEmpty().split('\u001F').filter { it.isNotBlank() }
        return HomeLayout(map["preset"].orEmpty().ifEmpty { DEFAULT_PRESET }, sections, shortcuts, pins)
    }
}
