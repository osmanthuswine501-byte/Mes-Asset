package com.rostomusic.app.playback

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.db.WaveformEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlin.random.Random

/** Loudness bars for the waveform progress style. Measured from the audio, cached per song. */
@Singleton
class WaveformRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: RostoDatabase,
) {
    suspend fun bars(song: Song): FloatArray = withContext(Dispatchers.Default) {
        db.waveforms().get(song.id)?.let { cached ->
            val values = cached.bars.split(',').mapNotNull { it.toIntOrNull() }
            if (values.size == BARS) return@withContext FloatArray(BARS) { values[it] / 100f }
        }
        val measured = runCatching { measure(song) }.getOrNull()
        if (measured != null) {
            db.waveforms().put(WaveformEntity(song.id, measured.joinToString(",") { (it * 100).toInt().toString() }))
            measured
        } else placeholder(song.id)
    }

    /** Used while measuring and for files the decoder cannot open. Stable for a given song. */
    fun placeholder(songId: Long): FloatArray {
        val r = Random(songId)
        var v = 0.5f
        return FloatArray(BARS) {
            v = (v + (r.nextFloat() - 0.5f) * 0.5f).coerceIn(0.15f, 1f)
            v
        }
    }

    /** Decodes a short slice at regular intervals instead of the whole file, to stay fast. */
    private fun measure(song: Song): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, song.uri, null)
            var track = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    track = i
                    format = f
                    break
                }
            }
            val fmt = format ?: return null
            extractor.selectTrack(track)
            val durationUs = if (fmt.containsKey(MediaFormat.KEY_DURATION)) fmt.getLong(MediaFormat.KEY_DURATION) else song.duration * 1000
            if (durationUs <= 0) return null
            val c = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            codec = c
            c.configure(fmt, null, null, 0)
            c.start()
            val info = MediaCodec.BufferInfo()
            val out = FloatArray(BARS)
            val deadline = SystemClock.elapsedRealtime() + 10_000
            for (b in 0 until BARS) {
                if (SystemClock.elapsedRealtime() > deadline) return null
                extractor.seekTo(durationUs * b / BARS, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                c.flush()
                var fed = 0
                var done = false
                var tries = 0
                var sum = 0.0
                var n = 0
                while (!done && tries++ < 60) {
                    if (fed < 4) {
                        val inIdx = c.dequeueInputBuffer(4000)
                        if (inIdx >= 0) {
                            val buf = c.getInputBuffer(inIdx)
                            val size = if (buf != null) extractor.readSampleData(buf, 0) else -1
                            if (size < 0) {
                                c.queueInputBuffer(inIdx, 0, 0, 0, 0)
                                fed = 4
                            } else {
                                c.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                                fed++
                            }
                        }
                    }
                    val outIdx = c.dequeueOutputBuffer(info, 4000)
                    if (outIdx >= 0) {
                        val ob = c.getOutputBuffer(outIdx)
                        if (ob != null && info.size > 0) {
                            ob.position(info.offset)
                            ob.limit(info.offset + info.size)
                            val shorts = ob.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val step = (shorts.remaining() / 512).coerceAtLeast(1)
                            var i = 0
                            while (i < shorts.remaining()) {
                                val s = shorts.get(i) / 32768.0
                                sum += s * s
                                n++
                                i += step
                            }
                            done = true
                        }
                        c.releaseOutputBuffer(outIdx, false)
                    } else if (fed >= 4 && outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && tries > 30) {
                        done = true
                    }
                }
                out[b] = if (n > 0) sqrt(sum / n).toFloat() else 0f
            }
            val max = out.max()
            if (max <= 0f) return null
            return FloatArray(BARS) { (out[it] / max).coerceIn(0.08f, 1f) }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    companion object {
        const val BARS = 56
    }
}
