package com.rostomusic.app.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One place the sound can go: this phone, wired headphones, a Bluetooth headset or speaker. */
data class AudioOutput(val id: Int, val name: String, val kind: Kind, val device: AudioDeviceInfo?) {
    enum class Kind { PHONE, WIRED, BLUETOOTH, USB }
}

/**
 * The outputs connected right now and the one the app plays on. The player follows [preferred]
 * (see PlaybackService); null lets Android choose, as usual.
 */
@Singleton
class AudioRoute @Inject constructor(@ApplicationContext private val context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val _outputs = MutableStateFlow<List<AudioOutput>>(emptyList())
    val outputs: StateFlow<List<AudioOutput>> = _outputs
    private val _preferred = MutableStateFlow<AudioOutput?>(null)
    val preferred: StateFlow<AudioOutput?> = _preferred

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = refresh()
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = refresh()
    }

    init {
        runCatching { audio.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper())) }
        refresh()
    }

    fun refresh() {
        val devices = runCatching { audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() }.getOrDefault(emptyList())
        val list = devices.mapNotNull { d ->
            val kind = kindOf(d.type) ?: return@mapNotNull null
            val name = if (kind == AudioOutput.Kind.PHONE) "" else d.productName?.toString().orEmpty()
            AudioOutput(d.id, name, kind, d)
        }.distinctBy { if (it.kind == AudioOutput.Kind.PHONE) "phone" else "${it.kind}:${it.name}" }
        _outputs.value = list.sortedBy { it.kind.ordinal }
        // A device that went away is forgotten, so the sound falls back to Android's choice.
        if (_preferred.value != null && list.none { it.id == _preferred.value?.id }) _preferred.value = null
    }

    fun select(output: AudioOutput) {
        _preferred.value = output
    }

    /** The output playing now: the chosen one, else the newest headset Android routes to, else the phone. */
    fun active(list: List<AudioOutput> = _outputs.value): AudioOutput? =
        _preferred.value?.let { p -> list.firstOrNull { it.id == p.id } }
            ?: list.lastOrNull { it.kind == AudioOutput.Kind.BLUETOOTH }
            ?: list.firstOrNull { it.kind == AudioOutput.Kind.WIRED || it.kind == AudioOutput.Kind.USB }
            ?: list.firstOrNull { it.kind == AudioOutput.Kind.PHONE }

    var volume: Float
        get() = runCatching {
            audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }.getOrDefault(0f)
        set(v) {
            runCatching {
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, (v.coerceIn(0f, 1f) * max).toInt(), 0)
            }
        }

    private fun kindOf(type: Int): AudioOutput.Kind? = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutput.Kind.PHONE
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_LINE_ANALOG -> AudioOutput.Kind.WIRED
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioOutput.Kind.BLUETOOTH
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> AudioOutput.Kind.USB
        else -> if (Build.VERSION.SDK_INT >= 31 && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) {
            AudioOutput.Kind.BLUETOOTH
        } else null
    }
}
