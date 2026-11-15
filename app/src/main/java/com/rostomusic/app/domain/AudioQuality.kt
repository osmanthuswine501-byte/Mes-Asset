package com.rostomusic.app.domain

/** The short quality label shown under each song, e.g. "16-bit", "EAC3" or "M4A 150 kbps". */
object AudioQuality {
    private val LOSSLESS_EXT = setOf("flac", "wav", "aif", "aiff", "ape", "wv", "dsf", "dff")
    private val LOSSLESS_MIME = setOf("audio/flac", "audio/alac", "audio/raw", "audio/x-wav", "audio/wav")

    fun label(path: String, codecMime: String, bitDepth: Int, bitrateBps: Int): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        dolby(codecMime)?.let { return it }
        val lossless = ext in LOSSLESS_EXT || codecMime.lowercase() in LOSSLESS_MIME
        if (lossless) return if (bitDepth > 0) "$bitDepth-bit" else ReleaseInfo.codecName(codecMime).ifEmpty { ext.uppercase() }
        val kbps = bitrateBps / 1000
        val name = ext.uppercase().ifEmpty { ReleaseInfo.codecName(codecMime) }
        return if (kbps > 0) "$name $kbps kbps" else name
    }

    /** Dolby codec as written on downloaded files: EAC3, AC3, AC4, TrueHD. */
    fun dolby(codecMime: String): String? = when (codecMime.lowercase()) {
        "audio/eac3", "audio/e-ac-3", "audio/eac3-joc" -> "EAC3"
        "audio/ac3", "audio/ac-3" -> "AC3"
        "audio/ac4", "audio/ac-4" -> "AC4"
        "audio/true-hd", "audio/vnd.dolby.mlp", "audio/mlp" -> "TrueHD"
        else -> null
    }
}
