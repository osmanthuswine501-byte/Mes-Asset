package com.rostomusic.app.domain

data class LyricWord(val timeMs: Long, val text: String)

data class LyricLine(val timeMs: Long, val text: String, val words: List<LyricWord> = emptyList())

/** [synced] false means plain text: every line has timeMs = -1. */
data class Lyrics(val lines: List<LyricLine>, val synced: Boolean, val offsetMs: Long = 0) {
    val isEmpty: Boolean get() = lines.none { it.text.isNotBlank() }
}

/** Parses standard LRC, multi-timestamp lines, the offset tag and enhanced (word by word) LRC. */
object LrcParser {
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val wordTag = Regex("""<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>""")
    private val metaTag = Regex("""^\[([a-zA-Z#]+):(.*)]$""")

    fun isSynced(text: String): Boolean =
        text.lineSequence().any { line -> timeTag.find(line.trim())?.range?.first == 0 }

    fun parse(raw: String): Lyrics {
        val text = raw.replace("\r\n", "\n").replace('\r', '\n').trim('﻿', ' ', '\n')
        if (!isSynced(text)) return parsePlain(text)

        var offset = 0L
        val out = ArrayList<LyricLine>()
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val startsWithTime = timeTag.find(line)?.range?.first == 0
            if (!startsWithTime) {
                val meta = metaTag.matchEntire(line)
                if (meta != null && meta.groupValues[1].equals("offset", ignoreCase = true)) {
                    offset = meta.groupValues[2].trim().removePrefix("+").toLongOrNull() ?: 0L
                }
                continue
            }
            var idx = 0
            val times = ArrayList<Long>()
            while (idx < line.length) {
                val m = timeTag.find(line, idx)
                if (m == null || m.range.first != idx) break
                times += toMs(m)
                idx = m.range.last + 1
                while (idx < line.length && line[idx] == ' ') idx++
            }
            val first = times.first()
            val (plain, words) = parseWords(line.substring(idx), first)
            for (t in times) {
                val shifted = if (t == first) words else words.map { it.copy(timeMs = it.timeMs - first + t) }
                out += LyricLine(t, plain, shifted)
            }
        }
        // A positive offset tag means the lyrics must show earlier.
        val shifted = out.map { l ->
            l.copy(
                timeMs = (l.timeMs - offset).coerceAtLeast(0),
                words = l.words.map { it.copy(timeMs = (it.timeMs - offset).coerceAtLeast(0)) },
            )
        }.sortedBy { it.timeMs }
        return Lyrics(shifted, synced = true, offsetMs = offset)
    }

    private fun parsePlain(text: String): Lyrics {
        val lines = ArrayList<LyricLine>()
        var lastBlank = true
        for (l in text.lines()) {
            val t = l.trim()
            if (metaTag.matches(t)) continue
            if (t.isEmpty()) {
                if (!lastBlank) lines += LyricLine(-1, "")
                lastBlank = true
            } else {
                lines += LyricLine(-1, t)
                lastBlank = false
            }
        }
        while (lines.isNotEmpty() && lines.last().text.isEmpty()) lines.removeAt(lines.size - 1)
        return Lyrics(lines, synced = false)
    }

    private fun parseWords(rest: String, lineTime: Long): Pair<String, List<LyricWord>> {
        val matches = wordTag.findAll(rest).toList()
        if (matches.isEmpty()) return rest.trim() to emptyList()
        val words = ArrayList<LyricWord>()
        val head = rest.substring(0, matches.first().range.first)
        if (head.isNotBlank()) words += LyricWord(lineTime, head)
        for ((i, m) in matches.withIndex()) {
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else rest.length
            val seg = rest.substring(m.range.last + 1, end)
            if (seg.isNotBlank()) words += LyricWord(toMs(m), seg)
        }
        val plain = words.joinToString("") { it.text }.replace(Regex("\\s+"), " ").trim()
        return plain to words
    }

    private fun toMs(m: MatchResult): Long {
        val min = m.groupValues[1].toLong()
        val sec = m.groupValues[2].toLong()
        val frac = m.groupValues[3]
        val ms = when (frac.length) {
            0 -> 0L
            1 -> frac.toLong() * 100
            2 -> frac.toLong() * 10
            else -> frac.take(3).toLong()
        }
        return min * 60_000 + sec * 1000 + ms
    }

    /** Index of the line being sung at [positionMs], or -1 before the first line. */
    fun indexAt(lines: List<LyricLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.size - 1
        var res = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                res = mid
                lo = mid + 1
            } else hi = mid - 1
        }
        return res
    }

    fun formatTime(ms: Long): String {
        val t = ms.coerceAtLeast(0)
        val min = t / 60_000
        val sec = (t % 60_000) / 1000
        val cs = (t % 1000) / 10
        return "%02d:%02d.%02d".format(min, sec, cs)
    }

    fun toLrc(lines: List<LyricLine>): String = buildString {
        for (l in lines) {
            if (l.timeMs < 0) {
                append(l.text)
            } else {
                append('[').append(formatTime(l.timeMs)).append(']')
                if (l.words.isEmpty()) append(l.text)
                else for (w in l.words) append('<').append(formatTime(w.timeMs)).append('>').append(w.text)
            }
            append('\n')
        }
    }
}
