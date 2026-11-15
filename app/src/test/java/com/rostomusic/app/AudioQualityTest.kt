package com.rostomusic.app

import com.rostomusic.app.domain.AudioQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioQualityTest {
    @Test fun dolbyShowsCodec() = assertEquals("EAC3", AudioQuality.label("/a/Van Gogh.m4a", "audio/eac3-joc", 0, 768_000))
    @Test fun losslessShowsBits() = assertEquals("16-bit", AudioQuality.label("/a/b.flac", "audio/flac", 16, 900_000))
    @Test fun losslessWithoutBits() = assertEquals("FLAC", AudioQuality.label("/a/b.flac", "audio/flac", 0, 900_000))
    @Test fun lossyShowsBitrate() = assertEquals("M4A 150 kbps", AudioQuality.label("/a/b.m4a", "audio/mp4a-latm", 0, 150_000))
    @Test fun alacInM4a() = assertEquals("24-bit", AudioQuality.label("/a/b.m4a", "audio/alac", 24, 2_000_000))
}
