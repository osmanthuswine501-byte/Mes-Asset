package com.rostomusic.app.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SleepState {
    data object Off : SleepState
    data class Until(val endAt: Long) : SleepState
    data object EndOfTrack : SleepState
}

@Singleton
class SleepTimer @Inject constructor() {
    private val _state = MutableStateFlow<SleepState>(SleepState.Off)
    val state: StateFlow<SleepState> = _state

    fun start(minutes: Int) {
        _state.value = SleepState.Until(System.currentTimeMillis() + minutes * 60_000L)
    }

    fun endOfTrack() {
        _state.value = SleepState.EndOfTrack
    }

    fun cancel() {
        _state.value = SleepState.Off
    }
}
