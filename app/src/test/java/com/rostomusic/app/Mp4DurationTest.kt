package com.rostomusic.app

import com.rostomusic.app.domain.Mp4Duration
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

class Mp4DurationTest {
    private fun box(type: String, body: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply { writeInt(body.size + 8); writeBytes(type); write(body) }
        return out.toByteArray()
    }

    private fun bytes(block: DataOutputStream.() -> Unit): ByteArray = ByteArrayOutputStream().also { DataOutputStream(it).block() }.toByteArray()

    private fun file(vararg boxes: ByteArray): File = File.createTempFile("mp4test", ".m4a").apply {
        deleteOnExit(); writeBytes(boxes.fold(ByteArray(0)) { a, b -> a + b })
    }

    private fun mvhd(scale: Int, duration: Int) = box("mvhd", bytes { writeInt(0); writeInt(0); writeInt(0); writeInt(scale); writeInt(duration); write(ByteArray(80)) })
    private fun mdhd(scale: Int) = box("mdhd", bytes { writeInt(0); writeInt(0); writeInt(0); writeInt(scale); writeInt(0); writeInt(0) })

    @Test fun movieHeader() {
        val f = file(box("ftyp", "M4A ".toByteArray()), box("moov", mvhd(1000, 171_000)))
        assertEquals(171_000L, Mp4Duration.millis(f))
    }

    @Test fun fragmentHeader() {
        val mehd = box("mehd", bytes { writeInt(0); writeInt(96_000 * 200) })
        val f = file(box("moov", mvhd(96_000, 0) + box("mvex", mehd)))
        assertEquals(200_000L, Mp4Duration.millis(f))
    }

    @Test fun fragmentsWithoutHeaders() {
        val trak = box("trak", box("mdia", mdhd(48_000)))
        fun fragment(samples: Int) = box(
            "moof",
            box("traf", box("tfhd", bytes { writeInt(0x000008); writeInt(1); writeInt(1536) }) +
                box("trun", bytes { writeInt(0); writeInt(samples) })),
        ) + box("mdat", ByteArray(16))
        val f = file(box("moov", mvhd(1000, 0) + trak), fragment(1000), fragment(875))
        // 1875 samples * 1536 / 48000 Hz = 60 s
        assertEquals(60_000L, Mp4Duration.millis(f))
    }

    @Test fun segmentIndex() {
        val sidx = box("sidx", bytes {
            writeInt(0); writeInt(1); writeInt(44_100); writeInt(0); writeInt(0); writeShort(0); writeShort(2)
            repeat(2) { writeInt(100); writeInt(44_100 * 90); writeInt(0) }
        })
        val f = file(box("moov", mvhd(1000, 0)), sidx)
        assertEquals(180_000L, Mp4Duration.millis(f))
    }

    @Test fun notMp4() {
        val f = File.createTempFile("mp4test", ".m4a").apply { deleteOnExit(); writeText("hello") }
        assertEquals(0L, Mp4Duration.millis(f))
    }
}
