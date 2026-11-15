package com.rostomusic.app.domain

object M3u {
    data class Entry(val path: String, val title: String = "", val durationSec: Long = -1)

    fun parse(text: String): List<Entry> {
        val out = ArrayList<Entry>()
        var title = ""
        var dur = -1L
        for (raw in text.lines()) {
            val line = raw.trim().trim('﻿')
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val body = line.substring(8)
                val comma = body.indexOf(',')
                dur = (if (comma >= 0) body.substring(0, comma) else body).trim().substringBefore(' ').toLongOrNull() ?: -1
                title = if (comma >= 0) body.substring(comma + 1).trim() else ""
                continue
            }
            if (line.startsWith("#")) continue
            out += Entry(line, title, dur)
            title = ""
            dur = -1
        }
        return out
    }

    fun build(entries: List<Entry>): String = buildString {
        append("#EXTM3U\n")
        for (e in entries) {
            append("#EXTINF:").append(e.durationSec).append(',').append(e.title).append('\n')
            append(e.path).append('\n')
        }
    }

    fun fileName(path: String): String = path.replace('\\', '/').substringAfterLast('/')
}
