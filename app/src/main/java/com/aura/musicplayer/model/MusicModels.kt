package com.aura.musicplayer.model

import androidx.compose.ui.graphics.Color

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val primaryColor: Color,
    val accentColor: Color,
    val genre: String,
    val releaseYear: Int = 2025
)

enum class PlaybackStatus {
    PLAYING,
    PAUSED,
    BUFFERING,
    STOPPED
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class AudioQuality(val label: String, val subtitle: String) {
    LOSSLESS_24BIT("Lossless 24-bit / 96kHz", "Apple Lossless / FLAC"),
    DOLBY_ATMOS("Dolby Atmos Spatial", "Immersive 3D Binaural"),
    HIGH_RES("Hi-Res Studio Master", "192kHz Uncompressed")
}

enum class OutputDevice(val deviceName: String, val type: String) {
    PHONE_SPEAKER("Phone Speakers", "Internal"),
    BLUETOOTH_HEADPHONES("Aura Sound ANC", "Bluetooth 5.4 LE"),
    CAST_AUDIO("Living Room Studio", "Cast Audio / Wi-Fi")
}

data class EqualizerBand(
    val id: Int,
    val frequencyLabel: String,
    val gainDb: Float // -12f to +12f
)

object SampleMusicData {
    val sampleTracks = listOf(
        Track(
            id = "1",
            title = "Aura Bloom (Breath Resonance)",
            artist = "Kaelen Voss & Elysian",
            album = "Somatic Frequencies",
            durationMs = 214000L, // 3:34
            primaryColor = Color(0xFF3F4E8D),
            accentColor = Color(0xFFE2E95C),
            genre = "Ambient Ambient Electronic"
        ),
        Track(
            id = "2",
            title = "Velvet Horizon",
            artist = "Solstice Pulse",
            album = "Midnight Ethereal",
            durationMs = 188000L, // 3:08
            primaryColor = Color(0xFF6C4F8D),
            accentColor = Color(0xFFFFB1C8),
            genre = "Chillwave"
        ),
        Track(
            id = "3",
            title = "Prism of Thought",
            artist = "Liora Vance",
            album = "Neural Architecture",
            durationMs = 245000L, // 4:05
            primaryColor = Color(0xFF2C637A),
            accentColor = Color(0xFF7CF4D4),
            genre = "Neo-Classical"
        ),
        Track(
            id = "4",
            title = "Golden Hour Drift",
            artist = "Arlo & The Tide",
            album = "Pacific Solitude",
            durationMs = 196000L, // 3:16
            primaryColor = Color(0xFF8A5D2E),
            accentColor = Color(0xFFFFD166),
            genre = "Organic Downtempo"
        ),
        Track(
            id = "5",
            title = "Kinetic Dreamscape",
            artist = "Synapse Echo",
            album = "Future Nostalgia",
            durationMs = 230000L, // 3:50
            primaryColor = Color(0xFF285E53),
            accentColor = Color(0xFF8EE3B4),
            genre = "Minimal Techno"
        )
    )

    val defaultEqualizerBands = listOf(
        EqualizerBand(0, "60Hz", 3.0f),
        EqualizerBand(1, "230Hz", 1.5f),
        EqualizerBand(2, "910Hz", -1.0f),
        EqualizerBand(3, "3.6kHz", 2.0f),
        EqualizerBand(4, "14kHz", 4.5f)
    )
}
