package com.rostomusic.app

import com.rostomusic.app.domain.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test
    fun parsesStandardLines() {
        val lyrics = LrcParser.parse("[ti:Song]\n[00:12.50]First line\n[01:02.3]Second line\n[02:00]Third")
        assertTrue(lyrics.synced)
        assertEquals(3, lyrics.lines.size)
        assertEquals(12_500L, lyrics.lines[0].timeMs)
        assertEquals("First line", lyrics.lines[0].text)
        assertEquals(62_300L, lyrics.lines[1].timeMs)
        assertEquals(120_000L, lyrics.lines[2].timeMs)
    }

    @Test
    fun expandsLinesWithSeveralTimestamps() {
        val lyrics = LrcParser.parse("[00:30.00][00:10.00]Chorus\n[00:20.00]Verse")
        assertEquals(listOf(10_000L, 20_000L, 30_000L), lyrics.lines.map { it.timeMs })
        assertEquals(listOf("Chorus", "Verse", "Chorus"), lyrics.lines.map { it.text })
    }

    @Test
    fun appliesOffsetTag() {
        val lyrics = LrcParser.parse("[offset:+500]\n[00:10.00]Line")
        assertEquals(500L, lyrics.offsetMs)
        assertEquals(9_500L, lyrics.lines[0].timeMs)
        val late = LrcParser.parse("[offset:-250]\n[00:10.00]Line")
        assertEquals(10_250L, late.lines[0].timeMs)
    }

    @Test
    fun parsesWordByWordTimings() {
        val lyrics = LrcParser.parse("[00:05.00]<00:05.00>Hello <00:05.80>big <00:06.40>world")
        val line = lyrics.lines.single()
        assertEquals("Hello big world", line.text)
        assertEquals(3, line.words.size)
        assertEquals(5_800L, line.words[1].timeMs)
        assertEquals("big ", line.words[1].text)
    }

    @Test
    fun keepsPlainTextUnsynced() {
        val lyrics = LrcParser.parse("First\n\n\nSecond\n")
        assertFalse(lyrics.synced)
        assertEquals(listOf("First", "", "Second"), lyrics.lines.map { it.text })
        assertTrue(lyrics.lines.all { it.timeMs == -1L })
    }

    @Test
    fun handlesWindowsLineEndingsAndThreeDigitFractions() {
        val lyrics = LrcParser.parse("\uFEFF[00:01.250]A\r\n[00:02:50]B\r\n")
        assertEquals(1_250L, lyrics.lines[0].timeMs)
        assertEquals(2_500L, lyrics.lines[1].timeMs)
    }

    @Test
    fun findsCurrentLine() {
        val lines = LrcParser.parse("[00:10.00]A\n[00:20.00]B\n[00:30.00]C").lines
        assertEquals(-1, LrcParser.indexAt(lines, 5_000))
        assertEquals(0, LrcParser.indexAt(lines, 10_000))
        assertEquals(1, LrcParser.indexAt(lines, 29_999))
        assertEquals(2, LrcParser.indexAt(lines, 99_000))
        assertEquals(-1, LrcParser.indexAt(emptyList(), 1_000))
    }

    @Test
    fun exportRoundTrips() {
        val source = "[00:10.00]A\n[00:20.50]B"
        val out = LrcParser.toLrc(LrcParser.parse(source).lines).trim()
        assertEquals(source, out)
        assertEquals(LrcParser.parse(source).lines, LrcParser.parse(out).lines)
    }

    @Test
    fun emptyTextIsEmpty() {
        assertTrue(LrcParser.parse("").isEmpty)
        assertTrue(LrcParser.parse("   \n ").isEmpty)
    }
}
