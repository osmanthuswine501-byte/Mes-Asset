package com.rostomusic.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.rostomusic.app.core.art.EmbeddedArtFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RostoApplication : Application(), ImageLoaderFactory {
    @javax.inject.Inject lateinit var artistPrefetcher: com.rostomusic.app.data.repo.ArtistPrefetcher

    override fun onCreate() {
        super.onCreate()
        // Artist photos and their colours are fetched in the background, ahead of their pages.
        artistPrefetcher.start()
    }

    /** Covers are decoded once, then served from memory or from small files on disk. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(EmbeddedArtFetcher.Factory()) }
            .crossfade(true)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("covers")).maxSizeBytes(64L * 1024 * 1024).build() }
            .respectCacheHeaders(false)
            .build()
}
