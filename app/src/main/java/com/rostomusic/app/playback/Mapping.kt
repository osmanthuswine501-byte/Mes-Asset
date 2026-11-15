package com.rostomusic.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rostomusic.app.data.db.Song

fun Song.toMediaItem(unknownArtist: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist.ifEmpty { unknownArtist })
                .setAlbumTitle(album)
                .setArtworkUri(artUri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        )
        .build()

fun browsableItem(id: String, title: String, subtitle: String? = null, mediaType: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, art: android.net.Uri? = null): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtworkUri(art)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(mediaType)
                .build()
        )
        .build()
