package com.rostomusic.app.data.tags

import java.io.InputStream

/** Reads the first bytes of a file to tell the real container, whatever its extension says. */
object FileInspector {
    data class Container(
        val name: String = "",
        val profile: String = "",
        val majorBrand: String = "",
        val minorVersion: String = "",
        val compatibleBrands: List<String> = emptyList(),
    )

    fun inspect(input: InputStream): Container {
        val head = ByteArray(512)
        var read = 0
        while (read < head.size) {
            val n = input.read(head, read, head.size - read)
            if (n <= 0) break
            read += n
        }
        return inspect(head.copyOf(read))
    }

    fun inspect(b: ByteArray): Container {
        if (b.size < 12) return Container()
        fun ascii(from: Int, len: Int) = if (from + len <= b.size) String(b, from, len, Charsets.ISO_8859_1) else ""
        return when {
            ascii(4, 4) == "ftyp" -> mp4(b)
            ascii(0, 4) == "fLaC" -> Container("FLAC")
            ascii(0, 4) == "OggS" -> Container("Ogg", oggCodec(b))
            ascii(0, 4) == "RIFF" && ascii(8, 4) == "WAVE" -> Container("WAVE (RIFF)")
            ascii(0, 4) == "RF64" -> Container("WAVE (RF64)")
            ascii(0, 4) == "FORM" && ascii(8, 4) == "AIFF" -> Container("AIFF")
            ascii(0, 4) == "FORM" && ascii(8, 4) == "AIFC" -> Container("AIFF-C")
            ascii(0, 3) == "ID3" -> Container("MPEG audio", "Tags ID3v2.${b[3].toInt()}")
            ascii(0, 4) == "MAC " -> Container("Monkey's Audio")
            ascii(0, 4) == "wvpk" -> Container("WavPack")
            b[0] == 0x30.toByte() && b[1] == 0x26.toByte() && b[2] == 0xB2.toByte() && b[3] == 0x75.toByte() -> Container("ASF (Windows Media)")
            b[0] == 0x0B.toByte() && b[1] == 0x77.toByte() -> Container("AC-3 / E-AC-3")
            b[0] == 0xFF.toByte() && (b[1].toInt() and 0xF6) == 0xF0 -> Container("ADTS (AAC)")
            b[0] == 0xFF.toByte() && (b[1].toInt() and 0xE0) == 0xE0 -> Container("MPEG audio")
            else -> Container()
        }
    }

    private fun mp4(b: ByteArray): Container {
        val boxSize = ((b[0].toInt() and 0xFF) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)
        val major = String(b, 8, 4, Charsets.ISO_8859_1)
        val minor = ((b[12].toInt() and 0xFF) shl 24) or ((b[13].toInt() and 0xFF) shl 16) or ((b[14].toInt() and 0xFF) shl 8) or (b[15].toInt() and 0xFF)
        val end = boxSize.coerceIn(16, b.size)
        val compatible = (16 until end - 3 step 4).map { String(b, it, 4, Charsets.ISO_8859_1).trim() }.filter { it.isNotBlank() }
        return Container("MPEG-4", profileOf(major.trim()), major.trim(), minor.toString(), compatible)
    }

    private fun profileOf(brand: String): String = when (brand) {
        "M4A" -> "Audio iTunes"
        "M4B" -> "Livre audio iTunes"
        "M4P" -> "Audio iTunes protégé"
        "mp41" -> "MPEG-4 version 1"
        "mp42" -> "MPEG-4 version 2"
        "dash" -> "MPEG-DASH"
        "3gp4", "3gp5", "3gp6", "3g2a" -> "3GPP"
        "qt" -> "QuickTime"
        "f4a" -> "Flash audio"
        else -> if (brand.startsWith("iso")) "Base Media" else brand
    }

    private fun oggCodec(b: ByteArray): String {
        val s = String(b, Charsets.ISO_8859_1)
        return when {
            s.contains("OpusHead") -> "Opus"
            s.contains("vorbis") -> "Vorbis"
            s.contains("FLAC") -> "FLAC"
            else -> ""
        }
    }

    fun imageFormat(b: ByteArray): String = when {
        b.size >= 4 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() -> "PNG"
        b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() -> "JPEG"
        b.size >= 12 && String(b, 0, 4, Charsets.ISO_8859_1) == "RIFF" && String(b, 8, 4, Charsets.ISO_8859_1) == "WEBP" -> "WebP"
        b.size >= 3 && String(b, 0, 3, Charsets.ISO_8859_1) == "GIF" -> "GIF"
        b.size >= 2 && b[0] == 'B'.code.toByte() && b[1] == 'M'.code.toByte() -> "BMP"
        else -> "—"
    }
}
