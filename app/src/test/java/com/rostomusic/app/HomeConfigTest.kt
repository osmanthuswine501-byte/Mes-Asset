package com.rostomusic.app

import com.rostomusic.app.domain.HomeConfig
import com.rostomusic.app.domain.SectionId
import com.rostomusic.app.domain.SectionStyle
import com.rostomusic.app.domain.ShortcutId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeConfigTest {
    @Test fun emptyTextGivesDefault() {
        val layout = HomeConfig.decode(null)
        assertEquals("default", layout.preset)
        assertEquals(SectionId.HERO, layout.sections.first().id)
        assertEquals(SectionId.entries.size, layout.sections.size)
    }

    @Test fun everyPresetRoundTrips() {
        for (name in HomeConfig.PRESETS) {
            val layout = HomeConfig.preset(name).copy(pins = listOf("album:12", "artist:La Fouine", "folder:/storage/a:b"))
            assertEquals(name, layout, HomeConfig.decode(HomeConfig.encode(layout)))
            assertEquals(SectionId.entries.size, layout.sections.map { it.id }.distinct().size)
        }
    }

    @Test fun unknownPresetFallsBack() {
        assertEquals("default", HomeConfig.preset("nope").preset)
    }

    @Test fun stylesOutsideTheSectionAreIgnored() {
        val layout = HomeConfig.preset("default")
        assertEquals(layout, HomeConfig.setStyle(layout, SectionId.HERO, SectionStyle.GRID))
        val changed = HomeConfig.setStyle(layout, SectionId.RECENT_PLAYED, SectionStyle.RANKED)
        assertEquals(SectionStyle.RANKED, changed.section(SectionId.RECENT_PLAYED)!!.style)
    }

    @Test fun badStoredValuesAreRepaired() {
        val text = "v1\npreset=default\nsections=HERO:GRID:1;GONE:LIST:1;STATS:FIXED:1\nshortcuts=FAVORITES,BAD,FAVORITES\npins=\n"
        val layout = HomeConfig.decode(text)
        assertEquals(SectionStyle.FIXED, layout.section(SectionId.HERO)!!.style)
        assertEquals(listOf(SectionId.HERO, SectionId.STATS), layout.sections.take(2).map { it.id })
        assertFalse(layout.section(SectionId.ALBUM_WALL)!!.visible)
        assertEquals(listOf(ShortcutId.FAVORITES), layout.shortcuts)
    }

    @Test fun pinningShowsPinnedSectionAndUnpinRemoves() {
        val layout = HomeConfig.preset("default")
        assertFalse(layout.section(SectionId.PINNED)!!.visible)
        val pinned = HomeConfig.togglePin(layout, "album:3")
        assertTrue(pinned.section(SectionId.PINNED)!!.visible)
        assertEquals(listOf("album:3"), pinned.pins)
        assertTrue(HomeConfig.togglePin(pinned, "album:3").pins.isEmpty())
    }

    @Test fun presetKeepsPinsAndShortcutsCapAtNine() {
        val pinned = HomeConfig.togglePin(HomeConfig.preset("default"), "artist:X")
        assertEquals(listOf("artist:X"), HomeConfig.applyPreset(pinned, "wall").pins)
        var layout = HomeConfig.preset("shortcuts")
        assertEquals(ShortcutId.entries.size, layout.shortcuts.size)
        layout = HomeConfig.toggleShortcut(layout, ShortcutId.STATS)
        assertEquals(ShortcutId.entries.size - 1, layout.shortcuts.size)
    }

    @Test fun moveReorders() {
        val layout = HomeConfig.move(HomeConfig.preset("default"), 0, 2)
        assertEquals(SectionId.HERO, layout.sections[2].id)
    }
}
