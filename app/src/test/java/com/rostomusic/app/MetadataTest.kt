package com.rostomusic.app

import com.rostomusic.app.data.tags.FileInspector
import com.rostomusic.app.domain.ExplicitFilter
import com.rostomusic.app.domain.LrcParser
import com.rostomusic.app.domain.ReleaseInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataTest {
    @Test
    fun masksSwearWordsKeepingFirstLetter() {
        assertEquals("Oh m**** alors", ExplicitFilter.mask("Oh merde alors"))
        assertEquals("P*****, c'est f******", ExplicitFilter.mask("Putain, c'est fucking"))
        assertEquals("E*****", ExplicitFilter.mask("Enculé"))
    }

    @Test
    fun leavesCleanWordsAlone() {
        val clean = "Le bitume, une compétition, Scunthorpe et le connecteur"
        assertEquals(clean, ExplicitFilter.mask(clean))
        assertEquals("", ExplicitFilter.mask(""))
    }

    @Test
    fun masksEveryLineAndWord() {
        val lyrics = ExplicitFilter.mask(LrcParser.parse("[00:01.00]<00:01.00>quelle <00:01.50>merde"))
        assertEquals("quelle m****", lyrics.lines[0].text)
        assertEquals("m****", lyrics.lines[0].words[1].text)
    }

    @Test
    fun detectsEp() {
        assertTrue(ReleaseInfo.isEp("THE IDOL EP", emptyList()))
        assertTrue(ReleaseInfo.isEp("Aura - EP", emptyList()))
        assertTrue(ReleaseInfo.isEp("Aura (EP)", emptyList()))
        assertTrue(ReleaseInfo.isEp("Aura", listOf("ep")))
        assertTrue(ReleaseInfo.isEp("Aura", listOf("album; EP")))
        assertFalse(ReleaseInfo.isEp("Steps", emptyList()))
        assertFalse(ReleaseInfo.isEp("Milliard", listOf("single")))
    }

    @Test
    fun namesDolbyStreams() {
        assertEquals("Dolby Atmos", ReleaseInfo.dolbyLabel("audio/eac3-joc"))
        assertEquals("Dolby Digital Plus", ReleaseInfo.dolbyLabel("audio/eac3"))
        assertEquals("Dolby Digital", ReleaseInfo.dolbyLabel("audio/ac3"))
        assertNull(ReleaseInfo.dolbyLabel("audio/flac"))
        assertEquals("AAC", ReleaseInfo.codecName("audio/mp4a-latm"))
        assertEquals("E-AC-3", ReleaseInfo.codecName("audio/eac3"))
    }

    @Test
    fun readsMp4Brands() {
        val box = byteArrayOf(0, 0, 0, 24) + "ftypdash".toByteArray() + byteArrayOf(0, 0, 0, 0) + "iso6mp41".toByteArray()
        val c = FileInspector.inspect(box + ByteArray(16))
        assertEquals("MPEG-4", c.name)
        assertEquals("dash", c.majorBrand)
        assertEquals("0", c.minorVersion)
        assertEquals(listOf("iso6", "mp41"), c.compatibleBrands)
        assertEquals("MPEG-DASH", c.profile)
    }

    @Test
    fun recognisesOtherContainers() {
        assertEquals("FLAC", FileInspector.inspect("fLaC".toByteArray() + ByteArray(20)).name)
        assertEquals("MPEG audio", FileInspector.inspect("ID3".toByteArray() + byteArrayOf(4) + ByteArray(20)).name)
        assertEquals("PNG", FileInspector.imageFormat(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())))
        assertEquals("JPEG", FileInspector.imageFormat(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
    }
}
