package com.rostomusic.app.domain

import kotlin.random.Random

/** Pure queue helpers, kept free of Android types so they can be unit tested. */
object QueueOps {
    enum class Repeat { OFF, ALL, ONE }

    fun nextRepeat(mode: Repeat): Repeat = when (mode) {
        Repeat.OFF -> Repeat.ALL
        Repeat.ALL -> Repeat.ONE
        Repeat.ONE -> Repeat.OFF
    }

    /** Shuffles [list] but keeps the item at [keepIndex] in first position, so playback is not interrupted. */
    fun <T> shuffledKeepingFirst(list: List<T>, keepIndex: Int, random: Random = Random.Default): List<T> {
        if (list.isEmpty()) return list
        val idx = keepIndex.coerceIn(0, list.size - 1)
        val rest = list.filterIndexed { i, _ -> i != idx }.shuffled(random)
        return listOf(list[idx]) + rest
    }

    /**
     * Restores the order the queue had before shuffling.
     * Items removed since then are dropped; items added since then are appended at the end.
     */
    fun <T> restoreOrder(current: List<T>, original: List<T>): List<T> {
        val remaining = current.toMutableList()
        val out = ArrayList<T>(current.size)
        for (item in original) {
            val i = remaining.indexOf(item)
            if (i >= 0) {
                out += item
                remaining.removeAt(i)
            }
        }
        out += remaining
        return out
    }

    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val out = list.toMutableList()
        out.add(to, out.removeAt(from))
        return out
    }

    /** Index that follows [current], or -1 when playback should stop. */
    fun nextIndex(current: Int, size: Int, repeat: Repeat): Int = when {
        size <= 0 -> -1
        repeat == Repeat.ONE -> current
        current + 1 < size -> current + 1
        repeat == Repeat.ALL -> 0
        else -> -1
    }

    /** Stable daily selection: the same [daySeed] always gives the same mix. */
    fun <T> dailyMix(items: List<T>, daySeed: Long, count: Int): List<T> =
        items.shuffled(Random(daySeed)).take(count)
}
