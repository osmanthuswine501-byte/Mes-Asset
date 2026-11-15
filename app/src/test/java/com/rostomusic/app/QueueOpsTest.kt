package com.rostomusic.app

import com.rostomusic.app.domain.M3u
import com.rostomusic.app.domain.QueueOps
import com.rostomusic.app.domain.QueueOps.Repeat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class QueueOpsTest {
    private val queue = (1..20).toList()

    @Test
    fun shuffleKeepsCurrentFirstAndLosesNothing() {
        val shuffled = QueueOps.shuffledKeepingFirst(queue, keepIndex = 7, random = Random(1))
        assertEquals(8, shuffled.first())
        assertEquals(queue.sorted(), shuffled.sorted())
        assertNotEquals(queue, shuffled)
    }

    @Test
    fun shuffleHandlesEdges() {
        assertEquals(emptyList<Int>(), QueueOps.shuffledKeepingFirst(emptyList<Int>(), 0))
        assertEquals(listOf(5), QueueOps.shuffledKeepingFirst(listOf(5), 3))
    }

    @Test
    fun restoreGivesBackOriginalOrder() {
        val shuffled = QueueOps.shuffledKeepingFirst(queue, 3, Random(2))
        assertEquals(queue, QueueOps.restoreOrder(shuffled, queue))
    }

    @Test
    fun restoreDropsRemovedAndAppendsAdded() {
        val current = listOf(9, 4, 99, 2, 7)
        assertEquals(listOf(2, 4, 7, 9, 99), QueueOps.restoreOrder(current, queue))
    }

    @Test
    fun restoreKeepsDuplicates() {
        val original = listOf(1, 2, 1, 3)
        assertEquals(original, QueueOps.restoreOrder(listOf(3, 1, 1, 2), original))
    }

    @Test
    fun moveReordersAndIgnoresBadIndexes() {
        assertEquals(listOf(2, 3, 1), QueueOps.move(listOf(1, 2, 3), 0, 2))
        assertEquals(listOf(3, 1, 2), QueueOps.move(listOf(1, 2, 3), 2, 0))
        assertEquals(listOf(1, 2, 3), QueueOps.move(listOf(1, 2, 3), 0, 9))
    }

    @Test
    fun repeatModesCycle() {
        assertEquals(Repeat.ALL, QueueOps.nextRepeat(Repeat.OFF))
        assertEquals(Repeat.ONE, QueueOps.nextRepeat(Repeat.ALL))
        assertEquals(Repeat.OFF, QueueOps.nextRepeat(Repeat.ONE))
    }

    @Test
    fun nextIndexFollowsRepeatMode() {
        assertEquals(1, QueueOps.nextIndex(0, 3, Repeat.OFF))
        assertEquals(-1, QueueOps.nextIndex(2, 3, Repeat.OFF))
        assertEquals(0, QueueOps.nextIndex(2, 3, Repeat.ALL))
        assertEquals(2, QueueOps.nextIndex(2, 3, Repeat.ONE))
        assertEquals(-1, QueueOps.nextIndex(0, 0, Repeat.ALL))
    }

    @Test
    fun dailyMixIsStableForOneDay() {
        assertEquals(QueueOps.dailyMix(queue, 42, 5), QueueOps.dailyMix(queue, 42, 5))
        assertEquals(5, QueueOps.dailyMix(queue, 42, 5).size)
        assertNotEquals(QueueOps.dailyMix(queue, 42, 5), QueueOps.dailyMix(queue, 43, 5))
    }

    @Test
    fun m3uRoundTrips() {
        val entries = listOf(M3u.Entry("/music/a.mp3", "Artist - A", 200), M3u.Entry("/music/b.flac", "Artist - B", 180))
        assertEquals(entries, M3u.parse(M3u.build(entries)))
        assertEquals(listOf(M3u.Entry("C:\\Music\\c.mp3")), M3u.parse("# comment\r\nC:\\Music\\c.mp3\r\n"))
        assertEquals("c.mp3", M3u.fileName("C:\\Music\\c.mp3"))
    }
}
