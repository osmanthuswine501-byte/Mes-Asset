package com.rostomusic.app.domain

/** Small helpers about the release a song belongs to. */
object ReleaseInfo {
    private val EP_TITLE = Regex("""(?i)(?:^|[\s\-–(\[])ep[)\]]?\s*$""")

    /** True when a release-type tag says "ep", or the album title ends with "EP", "(EP)" or "- EP". */
    fun isEp(albumTitle: String, releaseTypeTags: List<String>): Boolean =
        releaseTypeTags.any { tag -> tag.split(',', ';', '/').any { it.trim().equals("ep", ignoreCase = true) } } ||
            EP_TITLE.containsMatchIn(albumTitle.trim())

    /** Dolby label for a codec MIME type, or null when the stream is not Dolby. */
    fun dolbyLabel(mime: String?): String? = when (mime?.lowercase()) {
        "audio/eac3-joc" -> "Dolby Atmos"
        "audio/eac3", "audio/e-ac-3" -> "Dolby Digital Plus"
        "audio/ac3", "audio/ac-3" -> "Dolby Digital"
        "audio/ac4", "audio/ac-4" -> "Dolby AC-4"
        "audio/true-hd", "audio/vnd.dolby.mlp", "audio/mlp" -> "Dolby TrueHD"
        else -> null
    }

    /** Short codec name for a MIME type reported by the decoder. */
    fun codecName(mime: String?): String = when (mime?.lowercase()) {
        null, "" -> ""
        "audio/mp4a-latm", "audio/aac", "audio/aac-adts" -> "AAC"
        "audio/mpeg", "audio/mpeg-l2", "audio/mpeg-l1" -> "MP3"
        "audio/flac" -> "FLAC"
        "audio/alac" -> "ALAC"
        "audio/opus" -> "Opus"
        "audio/vorbis" -> "Vorbis"
        "audio/raw" -> "PCM"
        "audio/x-ms-wma" -> "WMA"
        "audio/amr-wb", "audio/3gpp" -> "AMR"
        "audio/ac3", "audio/ac-3" -> "AC-3"
        "audio/eac3", "audio/e-ac-3" -> "E-AC-3"
        "audio/eac3-joc" -> "E-AC-3 JOC"
        "audio/ac4", "audio/ac-4" -> "AC-4"
        else -> mime.substringAfter('/').uppercase()
    }
}
