package com.rostomusic.app.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.rostomusic.app.data.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqBand(val centerHz: Int, val minMb: Int, val maxMb: Int)

/** Owns the system equalizer, bass boost and virtualizer attached to the player's audio session. */
@Singleton
class AudioEffects @Inject constructor() {
    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null

    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    val bands: StateFlow<List<EqBand>> = _bands

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available

    fun attach(sessionId: Int, settings: Settings) {
        release()
        try {
            val e = Equalizer(0, sessionId)
            val range = e.bandLevelRange
            _bands.value = (0 until e.numberOfBands).map { i ->
                EqBand(e.getCenterFreq(i.toShort()) / 1000, range[0].toInt(), range[1].toInt())
            }
            eq = e
            bass = runCatching { BassBoost(0, sessionId) }.getOrNull()
            virt = runCatching { Virtualizer(0, sessionId) }.getOrNull()
            _available.value = true
            apply(settings)
        } catch (t: Throwable) {
            _available.value = false
            _bands.value = emptyList()
        }
    }

    fun apply(s: Settings) {
        val e = eq ?: return
        runCatching {
            val bands = _bands.value
            val levels = levelsFor(s, bands)
            for ((i, level) in levels.withIndex()) e.setBandLevel(i.toShort(), level.toShort())
            e.enabled = s.eqEnabled
            bass?.let {
                if (it.strengthSupported) it.setStrength(s.bassBoost.coerceIn(0, 1000).toShort())
                it.enabled = s.eqEnabled && s.bassBoost > 0
            }
            virt?.let {
                if (it.strengthSupported) it.setStrength(s.virtualizer.coerceIn(0, 1000).toShort())
                it.enabled = s.eqEnabled && s.virtualizer > 0
            }
        }
    }

    fun release() {
        runCatching { eq?.release() }
        runCatching { bass?.release() }
        runCatching { virt?.release() }
        eq = null
        bass = null
        virt = null
        _available.value = false
    }

    companion object {
        /** Gains in dB from the lowest to the highest frequency; stretched to the device's band count. */
        val PRESETS: Map<String, List<Float>> = linkedMapOf(
            "flat" to listOf(0f, 0f, 0f, 0f, 0f),
            "bass" to listOf(6f, 4f, 0f, 0f, 0f),
            "treble" to listOf(0f, 0f, 0f, 3f, 6f),
            "vocal" to listOf(-2f, 0f, 4f, 3f, 0f),
            "rock" to listOf(5f, 2f, -1f, 2f, 5f),
            "pop" to listOf(-1f, 2f, 4f, 2f, -1f),
            "jazz" to listOf(3f, 1f, -1f, 1f, 3f),
            "classical" to listOf(4f, 2f, 0f, 2f, 3f),
            "electronic" to listOf(5f, 3f, 0f, 2f, 4f),
        )

        fun presetLevels(preset: String, bands: List<EqBand>): List<Int> {
            val curve = PRESETS[preset] ?: PRESETS.getValue("flat")
            if (bands.isEmpty()) return emptyList()
            return bands.indices.map { i ->
                val pos = if (bands.size == 1) 0f else i.toFloat() / (bands.size - 1) * (curve.size - 1)
                val lo = pos.toInt().coerceIn(0, curve.size - 1)
                val hi = (lo + 1).coerceAtMost(curve.size - 1)
                val db = curve[lo] + (curve[hi] - curve[lo]) * (pos - lo)
                (db * 100).toInt().coerceIn(bands[i].minMb, bands[i].maxMb)
            }
        }

        fun levelsFor(s: Settings, bands: List<EqBand>): List<Int> {
            val custom = s.eqBands.split(',').mapNotNull { it.trim().toIntOrNull() }
            return if (custom.size == bands.size && bands.isNotEmpty()) {
                custom.mapIndexed { i, v -> v.coerceIn(bands[i].minMb, bands[i].maxMb) }
            } else presetLevels(s.eqPreset, bands)
        }
    }
}
