package com.rostomusic.app.data.repo

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.rostomusic.app.core.design.measureCoverTone
import com.rostomusic.app.data.settings.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.coroutineScope
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gets every artist's photo ready before their page is opened: looks it up (Deezer), downloads the
 * picture to the disk cache at banner size, and measures its colour. Three artists at a time, only
 * when the online artist option is on. An artist page can then open directly with its photo and
 * its colour instead of switching from the album cover to the photo while the user watches.
 */
@Singleton
class ArtistPrefetcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val library: LibraryRepository,
    private val info: ArtistInfoRepository,
    private val settings: SettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val done = Collections.synchronizedSet(HashSet<String>())
    private var started = false

    /** Each artist already looked up: the photo's address, or "" when there is none. Absent while pending. */
    private val _photos = kotlinx.coroutines.flow.MutableStateFlow<Map<String, String>>(emptyMap())
    val photos: kotlinx.coroutines.flow.StateFlow<Map<String, String>> = _photos

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(
                library.artists.map { list -> list.map { it.name }.filter { it.isNotBlank() } }.distinctUntilChanged(),
                settings.flow.map { it.artistInfoOnline }.distinctUntilChanged(),
            ) { names, on -> if (on) names else emptyList() }
                .collectLatest { names -> prefetch(names.filterNot { it in done }) }
        }
    }

    private suspend fun prefetch(names: List<String>) = coroutineScope {
        val gate = Semaphore(3)
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = (400 * metrics.density).toInt()
        for (name in names) launch {
            gate.withPermit {
                val url = runCatching { info.get(name) }.getOrNull()?.imageUrl?.takeIf { it.isNotEmpty() }
                if (url != null) {
                    // Full banner picture on disk only, so covers in memory are not pushed out.
                    val banner = ImageRequest.Builder(context).data(url).size(width, height)
                        .memoryCachePolicy(CachePolicy.DISABLED).build()
                    runCatching { context.imageLoader.execute(banner) }
                    runCatching { measureCoverTone(context, url) }
                }
                _photos.update { it + (name to (url ?: "")) }
                done += name
            }
        }
    }
}
