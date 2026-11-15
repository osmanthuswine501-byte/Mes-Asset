package com.rostomusic.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class PlayerVisual { COVER, VINYL }
enum class ProgressStyle { LINE, WAVE }
enum class RowSubtitle { ARTIST_DURATION, ARTIST, ARTIST_ALBUM }
enum class LyricsAlign { START, CENTER }

data class Settings(
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: Int = 0xFF22C55E.toInt(),
    val playerVisual: PlayerVisual = PlayerVisual.COVER,
    val progressStyle: ProgressStyle = ProgressStyle.LINE,
    val rowSubtitle: RowSubtitle = RowSubtitle.ARTIST_DURATION,
    val showRemaining: Boolean = false,
    val haptics: Boolean = true,
    /** Material You: colours taken from the wallpaper (Android 12 and later). */
    val dynamicColor: Boolean = false,
    val minDurationSec: Int = 30,
    val excludedFolders: Set<String> = emptySet(),
    val fadeSec: Int = 0,
    val speed: Float = 1f,
    val replayGain: Boolean = false,
    val preampDb: Float = 0f,
    val resumeOnStart: Boolean = true,
    val lyricsScale: Float = 1f,
    val lyricsAlign: LyricsAlign = LyricsAlign.START,
    val keepScreenOn: Boolean = true,
    val onlineLyrics: Boolean = false,
    val explicitLyrics: Boolean = true,
    val artistInfoOnline: Boolean = true,
    val lrcTreeUri: String = "",
    val eqEnabled: Boolean = false,
    val eqBands: String = "",
    val eqPreset: String = "flat",
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val lastQueue: String = "",
    val lastIndex: Int = 0,
    val lastPosition: Long = 0,
    val searchHistory: List<String> = emptyList(),
    val homeLayout: String = "",
)

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = intPreferencesKey("accent")
        val playerVisual = stringPreferencesKey("player_visual")
        val progressStyle = stringPreferencesKey("progress_style")
        val rowSubtitle = stringPreferencesKey("row_subtitle")
        val showRemaining = booleanPreferencesKey("show_remaining")
        val haptics = booleanPreferencesKey("haptics")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val minDurationSec = intPreferencesKey("min_duration")
        val excludedFolders = stringSetPreferencesKey("excluded_folders")
        val fadeSec = intPreferencesKey("fade_sec")
        val speed = floatPreferencesKey("speed")
        val replayGain = booleanPreferencesKey("replay_gain")
        val preampDb = floatPreferencesKey("preamp_db")
        val resumeOnStart = booleanPreferencesKey("resume_on_start")
        val lyricsScale = floatPreferencesKey("lyrics_scale")
        val lyricsAlign = stringPreferencesKey("lyrics_align")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val onlineLyrics = booleanPreferencesKey("online_lyrics")
        val explicitLyrics = booleanPreferencesKey("explicit_lyrics")
        val artistInfoOnline = booleanPreferencesKey("artist_info_online")
        val lrcTreeUri = stringPreferencesKey("lrc_tree_uri")
        val eqEnabled = booleanPreferencesKey("eq_enabled")
        val eqBands = stringPreferencesKey("eq_bands")
        val eqPreset = stringPreferencesKey("eq_preset")
        val bassBoost = intPreferencesKey("bass_boost")
        val virtualizer = intPreferencesKey("virtualizer")
        val lastQueue = stringPreferencesKey("last_queue")
        val lastIndex = intPreferencesKey("last_index")
        val lastPosition = longPreferencesKey("last_position")
        val searchHistory = stringPreferencesKey("search_history")
        val homeLayout = stringPreferencesKey("home_layout")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { p ->
        val d = Settings()
        Settings(
            onboardingDone = p[K.onboardingDone] ?: d.onboardingDone,
            themeMode = enumOr(p[K.themeMode], d.themeMode),
            accent = p[K.accent] ?: d.accent,
            playerVisual = enumOr(p[K.playerVisual], d.playerVisual),
            progressStyle = enumOr(p[K.progressStyle], d.progressStyle),
            rowSubtitle = enumOr(p[K.rowSubtitle], d.rowSubtitle),
            showRemaining = p[K.showRemaining] ?: d.showRemaining,
            haptics = p[K.haptics] ?: d.haptics,
            dynamicColor = p[K.dynamicColor] ?: d.dynamicColor,
            minDurationSec = p[K.minDurationSec] ?: d.minDurationSec,
            excludedFolders = p[K.excludedFolders] ?: d.excludedFolders,
            fadeSec = p[K.fadeSec] ?: d.fadeSec,
            speed = p[K.speed] ?: d.speed,
            replayGain = p[K.replayGain] ?: d.replayGain,
            preampDb = p[K.preampDb] ?: d.preampDb,
            resumeOnStart = p[K.resumeOnStart] ?: d.resumeOnStart,
            lyricsScale = p[K.lyricsScale] ?: d.lyricsScale,
            lyricsAlign = enumOr(p[K.lyricsAlign], d.lyricsAlign),
            keepScreenOn = p[K.keepScreenOn] ?: d.keepScreenOn,
            onlineLyrics = p[K.onlineLyrics] ?: d.onlineLyrics,
            explicitLyrics = p[K.explicitLyrics] ?: d.explicitLyrics,
            artistInfoOnline = p[K.artistInfoOnline] ?: d.artistInfoOnline,
            lrcTreeUri = p[K.lrcTreeUri] ?: d.lrcTreeUri,
            eqEnabled = p[K.eqEnabled] ?: d.eqEnabled,
            eqBands = p[K.eqBands] ?: d.eqBands,
            eqPreset = p[K.eqPreset] ?: d.eqPreset,
            bassBoost = p[K.bassBoost] ?: d.bassBoost,
            virtualizer = p[K.virtualizer] ?: d.virtualizer,
            lastQueue = p[K.lastQueue] ?: d.lastQueue,
            lastIndex = p[K.lastIndex] ?: d.lastIndex,
            lastPosition = p[K.lastPosition] ?: d.lastPosition,
            searchHistory = (p[K.searchHistory] ?: "").split('\n').filter { it.isNotBlank() },
            homeLayout = p[K.homeLayout] ?: d.homeLayout,
        )
    }

    suspend fun current(): Settings = flow.first()

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setOnboardingDone(v: Boolean) = edit { it[K.onboardingDone] = v }
    suspend fun setThemeMode(v: ThemeMode) = edit { it[K.themeMode] = v.name }
    suspend fun setAccent(v: Int) = edit { it[K.accent] = v }
    suspend fun setPlayerVisual(v: PlayerVisual) = edit { it[K.playerVisual] = v.name }
    suspend fun setProgressStyle(v: ProgressStyle) = edit { it[K.progressStyle] = v.name }
    suspend fun setRowSubtitle(v: RowSubtitle) = edit { it[K.rowSubtitle] = v.name }
    suspend fun setShowRemaining(v: Boolean) = edit { it[K.showRemaining] = v }
    suspend fun setHaptics(v: Boolean) = edit { it[K.haptics] = v }
    suspend fun setDynamicColor(v: Boolean) = edit { it[K.dynamicColor] = v }
    suspend fun setMinDuration(v: Int) = edit { it[K.minDurationSec] = v }
    suspend fun setExcludedFolders(v: Set<String>) = edit { it[K.excludedFolders] = v }
    suspend fun setFadeSec(v: Int) = edit { it[K.fadeSec] = v }
    suspend fun setSpeed(v: Float) = edit { it[K.speed] = v }
    suspend fun setReplayGain(v: Boolean) = edit { it[K.replayGain] = v }
    suspend fun setPreampDb(v: Float) = edit { it[K.preampDb] = v }
    suspend fun setResumeOnStart(v: Boolean) = edit { it[K.resumeOnStart] = v }
    suspend fun setLyricsScale(v: Float) = edit { it[K.lyricsScale] = v }
    suspend fun setLyricsAlign(v: LyricsAlign) = edit { it[K.lyricsAlign] = v.name }
    suspend fun setKeepScreenOn(v: Boolean) = edit { it[K.keepScreenOn] = v }
    suspend fun setOnlineLyrics(v: Boolean) = edit { it[K.onlineLyrics] = v }
    suspend fun setExplicitLyrics(v: Boolean) = edit { it[K.explicitLyrics] = v }
    suspend fun setArtistInfoOnline(v: Boolean) = edit { it[K.artistInfoOnline] = v }
    suspend fun setLrcTreeUri(v: String) = edit { it[K.lrcTreeUri] = v }
    suspend fun setEqEnabled(v: Boolean) = edit { it[K.eqEnabled] = v }
    suspend fun setEqBands(v: String, preset: String) = edit { it[K.eqBands] = v; it[K.eqPreset] = preset }
    suspend fun setBassBoost(v: Int) = edit { it[K.bassBoost] = v }
    suspend fun setVirtualizer(v: Int) = edit { it[K.virtualizer] = v }
    suspend fun saveQueue(ids: String, index: Int, position: Long) = edit {
        it[K.lastQueue] = ids; it[K.lastIndex] = index; it[K.lastPosition] = position
    }
    suspend fun savePosition(index: Int, position: Long) = edit { it[K.lastIndex] = index; it[K.lastPosition] = position }
    suspend fun addSearch(q: String) = edit { p ->
        val cur = (p[K.searchHistory] ?: "").split('\n').filter { it.isNotBlank() && !it.equals(q, true) }
        p[K.searchHistory] = (listOf(q) + cur).take(8).joinToString("\n")
    }
    suspend fun clearSearchHistory() = edit { it[K.searchHistory] = "" }
    suspend fun setHomeLayout(encoded: String) = edit { it[K.homeLayout] = encoded }

    /** Reads, changes and saves the home layout in one step. */
    suspend fun updateHome(change: (com.rostomusic.app.domain.HomeLayout) -> com.rostomusic.app.domain.HomeLayout) = edit { p ->
        val current = com.rostomusic.app.domain.HomeConfig.decode(p[K.homeLayout])
        p[K.homeLayout] = com.rostomusic.app.domain.HomeConfig.encode(change(current))
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}
