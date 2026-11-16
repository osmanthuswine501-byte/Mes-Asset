package com.aura.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.musicplayer.model.AudioQuality
import com.aura.musicplayer.model.EqualizerBand
import com.aura.musicplayer.model.OutputDevice
import com.aura.musicplayer.model.PlaybackStatus
import com.aura.musicplayer.model.RepeatMode
import com.aura.musicplayer.model.SampleMusicData
import com.aura.musicplayer.model.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {

    val playlist: List<Track> = SampleMusicData.sampleTracks

    private val _currentTrack = MutableStateFlow(playlist[0])
    val currentTrack: StateFlow<Track> = _currentTrack.asStateFlow()

    private val _playbackStatus = MutableStateFlow(PlaybackStatus.PLAYING)
    val playbackStatus: StateFlow<PlaybackStatus> = _playbackStatus.asStateFlow()

    private val _progressMs = MutableStateFlow(34000L) // Start at 34s
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()

    private val _volume = MutableStateFlow(0.75f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _favoriteTrackIds = MutableStateFlow(setOf("1", "3"))
    val favoriteTrackIds: StateFlow<Set<String>> = _favoriteTrackIds.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedAudioQuality = MutableStateFlow(AudioQuality.LOSSLESS_24BIT)
    val selectedAudioQuality: StateFlow<AudioQuality> = _selectedAudioQuality.asStateFlow()

    private val _selectedOutputDevice = MutableStateFlow(OutputDevice.BLUETOOTH_HEADPHONES)
    val selectedOutputDevice: StateFlow<OutputDevice> = _selectedOutputDevice.asStateFlow()

    private val _equalizerBands = MutableStateFlow(SampleMusicData.defaultEqualizerBands)
    val equalizerBands: StateFlow<List<EqualizerBand>> = _equalizerBands.asStateFlow()

    private val _isEqualizerOpen = MutableStateFlow(false)
    val isEqualizerOpen: StateFlow<Boolean> = _isEqualizerOpen.asStateFlow()

    private val _spatialAudioEnabled = MutableStateFlow(true)
    val spatialAudioEnabled: StateFlow<Boolean> = _spatialAudioEnabled.asStateFlow()

    private val _bassBoost = MutableStateFlow(0.6f)
    val bassBoost: StateFlow<Float> = _bassBoost.asStateFlow()

    private var playbackJob: Job? = null

    init {
        startPlaybackSimulation()
    }

    private fun startPlaybackSimulation() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_playbackStatus.value == PlaybackStatus.PLAYING) {
                    val nextMs = _progressMs.value + 1000L
                    val totalDuration = _currentTrack.value.durationMs
                    if (nextMs >= totalDuration) {
                        handleTrackCompletion()
                    } else {
                        _progressMs.value = nextMs
                    }
                }
            }
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _progressMs.value = 0L
            }
            RepeatMode.ALL -> {
                skipToNextTrack()
            }
            RepeatMode.OFF -> {
                val currentIndex = playlist.indexOfFirst { it.id == _currentTrack.value.id }
                if (currentIndex < playlist.size - 1) {
                    skipToNextTrack()
                } else {
                    _playbackStatus.value = PlaybackStatus.PAUSED
                    _progressMs.value = 0L
                }
            }
        }
    }

    fun togglePlayPause() {
        _playbackStatus.value = if (_playbackStatus.value == PlaybackStatus.PLAYING) {
            PlaybackStatus.PAUSED
        } else {
            PlaybackStatus.PLAYING
        }
    }

    fun seekToFraction(fraction: Float) {
        val targetMs = (fraction.coerceIn(0f, 1f) * _currentTrack.value.durationMs).toLong()
        _progressMs.value = targetMs
    }

    fun skipToNextTrack() {
        val currentIndex = playlist.indexOfFirst { it.id == _currentTrack.value.id }
        val nextIndex = if (_isShuffle.value) {
            playlist.indices.filter { it != currentIndex }.randomOrNull() ?: 0
        } else {
            (currentIndex + 1) % playlist.size
        }
        _currentTrack.value = playlist[nextIndex]
        _progressMs.value = 0L
        _playbackStatus.value = PlaybackStatus.PLAYING
    }

    fun skipToPreviousTrack() {
        if (_progressMs.value > 3000L) {
            _progressMs.value = 0L
            return
        }
        val currentIndex = playlist.indexOfFirst { it.id == _currentTrack.value.id }
        val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        _currentTrack.value = playlist[prevIndex]
        _progressMs.value = 0L
        _playbackStatus.value = PlaybackStatus.PLAYING
    }

    fun selectTrack(track: Track) {
        _currentTrack.value = track
        _progressMs.value = 0L
        _playbackStatus.value = PlaybackStatus.PLAYING
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleFavorite(trackId: String = _currentTrack.value.id) {
        val currentFavorites = _favoriteTrackIds.value.toMutableSet()
        if (currentFavorites.contains(trackId)) {
            currentFavorites.remove(trackId)
        } else {
            currentFavorites.add(trackId)
        }
        _favoriteTrackIds.value = currentFavorites
    }

    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectAudioQuality(quality: AudioQuality) {
        _selectedAudioQuality.value = quality
    }

    fun selectOutputDevice(device: OutputDevice) {
        _selectedOutputDevice.value = device
    }

    fun setEqualizerOpen(isOpen: Boolean) {
        _isEqualizerOpen.value = isOpen
    }

    fun updateEqualizerBand(id: Int, newGain: Float) {
        val updated = _equalizerBands.value.map { band ->
            if (band.id == id) band.copy(gainDb = newGain.coerceIn(-12f, 12f)) else band
        }
        _equalizerBands.value = updated
    }

    fun toggleSpatialAudio() {
        _spatialAudioEnabled.value = !_spatialAudioEnabled.value
    }

    fun setBassBoost(value: Float) {
        _bassBoost.value = value.coerceIn(0f, 1f)
    }
}
