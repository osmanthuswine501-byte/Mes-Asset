package com.rostomusic.app.domain

import java.io.File
import java.io.RandomAccessFile

/**
 * Reads the length of an MP4 / M4A file from its boxes. Android's own reader gives up on some files,
 * like the fragmented M4A of streaming download tools (Tidal, Dolby Atmos), and reports zero.
 *
 * Tries, in order: the movie header, the fragment header (mehd), the segment indexes (sidx),
 * then the sum of every fragment's samples (moof/traf/trun).
 */
object Mp4Duration {

    fun millis(file: File): Long = runCatching {
        RandomAccessFile(file, "r").use { read(it) }
    }.getOrDefault(0L)

    private class State {
        var movieScale = 0L
        var movieDuration = 0L
        var fragmentDuration = 0L
        var trackScale = 0L
        var sidxScale = 0L
        var sidxTotal = 0L
        var sampleTotal = 0L
        /** Default sample duration of the fragment being read; tfhd always precedes its trun. */
        var currentDefault = 0L
    }

    private fun read(f: RandomAccessFile): Long {
        val s = State()
        walk(f, 0, f.length(), s, depth = 0)
        return when {
            s.movieScale > 0 && s.movieDuration > 0 -> s.movieDuration * 1000 / s.movieScale
            s.movieScale > 0 && s.fragmentDuration > 0 -> s.fragmentDuration * 1000 / s.movieScale
            s.sidxScale > 0 && s.sidxTotal > 0 -> s.sidxTotal * 1000 / s.sidxScale
            s.trackScale > 0 && s.sampleTotal > 0 -> s.sampleTotal * 1000 / s.trackScale
            else -> 0L
        }
    }

    private val CONTAINERS = setOf("moov", "trak", "mdia", "mvex", "moof", "traf")

    private fun walk(f: RandomAccessFile, start: Long, end: Long, s: State, depth: Int) {
        var pos = start
        while (pos + 8 <= end) {
            f.seek(pos)
            var size = f.readInt().toLong() and 0xFFFFFFFFL
            val type = String(ByteArray(4).also { f.readFully(it) }, Charsets.ISO_8859_1)
            var header = 8L
            if (size == 1L) {
                size = f.readLong()
                header = 16
            } else if (size == 0L) {
                size = end - pos
            }
            if (size < header || pos + size > end + 8) return
            val body = pos + header
            val boxEnd = minOf(pos + size, end)
            when {
                type in CONTAINERS && depth < 6 -> walk(f, body, boxEnd, s, depth + 1)
                type == "mvhd" -> {
                    f.seek(body)
                    val v = f.readUnsignedByte(); f.skipBytes(3)
                    if (v == 1) { f.skipBytes(16); s.movieScale = u32(f); s.movieDuration = f.readLong() }
                    else { f.skipBytes(8); s.movieScale = u32(f); s.movieDuration = u32(f).takeIf { it != 0xFFFFFFFFL } ?: 0 }
                }
                type == "mehd" -> {
                    f.seek(body)
                    val v = f.readUnsignedByte(); f.skipBytes(3)
                    s.fragmentDuration = if (v == 1) f.readLong() else u32(f)
                }
                type == "mdhd" && s.trackScale == 0L -> {
                    f.seek(body)
                    val v = f.readUnsignedByte(); f.skipBytes(3)
                    f.skipBytes(if (v == 1) 16 else 8)
                    s.trackScale = u32(f)
                }
                type == "sidx" -> {
                    f.seek(body)
                    val v = f.readUnsignedByte(); f.skipBytes(3)
                    f.skipBytes(4)
                    val scale = u32(f)
                    f.skipBytes(if (v == 1) 16 else 8)
                    f.skipBytes(2)
                    val count = f.readUnsignedShort()
                    var total = 0L
                    repeat(count) { f.skipBytes(4); total += u32(f); f.skipBytes(4) }
                    if (s.sidxScale == 0L || s.sidxScale == scale) { s.sidxScale = scale; s.sidxTotal += total }
                }
                type == "tfhd" -> {
                    f.seek(body)
                    f.skipBytes(1)
                    val flags = (f.readUnsignedByte() shl 16) or (f.readUnsignedByte() shl 8) or f.readUnsignedByte()
                    f.skipBytes(4)
                    if (flags and 0x01 != 0) f.skipBytes(8)
                    if (flags and 0x02 != 0) f.skipBytes(4)
                    s.currentDefault = if (flags and 0x08 != 0) u32(f) else 0L
                }
                type == "trun" -> {
                    f.seek(body)
                    f.skipBytes(1)
                    val flags = (f.readUnsignedByte() shl 16) or (f.readUnsignedByte() shl 8) or f.readUnsignedByte()
                    val count = u32(f)
                    if (flags and 0x01 != 0) f.skipBytes(4)
                    if (flags and 0x04 != 0) f.skipBytes(4)
                    if (flags and 0x100 == 0) {
                        s.sampleTotal += count * s.currentDefault
                    } else {
                        val stride = 4 + listOf(0x200, 0x400, 0x800).count { flags and it != 0 } * 4
                        var i = 0L
                        while (i < count) {
                            s.sampleTotal += u32(f)
                            f.skipBytes(stride - 4)
                            i++
                        }
                    }
                }
            }
            pos += size
        }
    }

    private fun u32(f: RandomAccessFile): Long = f.readInt().toLong() and 0xFFFFFFFFL
}
