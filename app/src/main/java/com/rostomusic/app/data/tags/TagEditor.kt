package com.rostomusic.app.data.tags

import android.content.Context
import android.content.IntentSender
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.domain.LrcParser
import com.rostomusic.app.domain.ReleaseInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.TagTextField
import org.jaudiotagger.tag.id3.framebody.AbstractFrameBodyUrlLink
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import org.jaudiotagger.tag.id3.framebody.FrameBodyWXXX
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.framebody.FrameBodySYLT
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.nio.charset.Charset
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class TagFields(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val year: String = "",
    val genre: String = "",
    val track: String = "",
    val disc: String = "",
    val composer: String = "",
    val bpm: String = "",
    val comment: String = "",
)

data class ArtworkInfo(val width: Int, val height: Int, val format: String, val bytes: Int)

data class AudioDetails(
    val tags: TagFields,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val bitsPerSample: Int = 0,
    val bitrateKbps: Int = 0,
    val replayGainDb: Float? = null,
    val readable: Boolean = true,
    /** Codec name, e.g. AAC or FLAC, and the decoder MIME type it comes from. */
    val codec: String = "",
    val codecMime: String = "",
    val container: FileInspector.Container = FileInspector.Container(),
    val encoder: String = "",
    val artwork: ArtworkInfo? = null,
    /** Every other tag found in the file, as label to value. */
    val extraTags: List<Pair<String, String>> = emptyList(),
    val releaseTypes: List<String> = emptyList(),
)

data class EmbeddedLyrics(val synced: String?, val plain: String?)

sealed interface CoverChange {
    data object Keep : CoverChange
    data object Remove : CoverChange
    class Set(val bytes: ByteArray, val mime: String) : CoverChange
}

@Singleton
class TagEditor @Inject constructor(@ApplicationContext private val context: Context) {

    init {
        runCatching {
            TagOptionSingleton.getInstance().isAndroid = true
            Logger.getLogger("org.jaudiotagger").level = Level.OFF
        }
    }

    /** Opens the file with the tag library, falling back to a private copy when the path is not readable. */
    private fun <T> withAudioFile(song: Song, block: (AudioFile) -> T): T? {
        val direct = File(song.path)
        if (direct.canRead()) {
            runCatching { return block(AudioFileIO.read(direct)) }
        }
        val tmp = File(context.cacheDir, "read_${song.id}.${song.extension.ifEmpty { "mp3" }}")
        return try {
            context.contentResolver.openInputStream(song.uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } } ?: return null
            block(AudioFileIO.read(tmp))
        } catch (e: Throwable) {
            null
        } finally {
            tmp.delete()
        }
    }

    suspend fun readDetails(song: Song): AudioDetails = withContext(Dispatchers.IO) {
        val decoder = decoderInfo(song)
        val container = runCatching {
            context.contentResolver.openInputStream(song.uri)?.use { FileInspector.inspect(it) }
        }.getOrNull() ?: FileInspector.Container()
        val fromLib = withAudioFile(song) { af ->
            val tag: Tag? = af.tag
            val h = af.audioHeader
            fun f(key: FieldKey) = runCatching { tag?.getFirst(key).orEmpty().trim() }.getOrDefault("")
            val extras = tag?.let { collectExtras(it) }.orEmpty()
            val encoderEntry = extras.firstOrNull { it.first in ENCODER_LABELS }
            AudioDetails(
                tags = TagFields(
                    title = f(FieldKey.TITLE), artist = f(FieldKey.ARTIST), album = f(FieldKey.ALBUM),
                    albumArtist = f(FieldKey.ALBUM_ARTIST), year = f(FieldKey.YEAR), genre = f(FieldKey.GENRE),
                    track = f(FieldKey.TRACK), disc = f(FieldKey.DISC_NO), composer = f(FieldKey.COMPOSER),
                    bpm = f(FieldKey.BPM), comment = f(FieldKey.COMMENT),
                ),
                sampleRate = runCatching { h.sampleRateAsNumber }.getOrDefault(0).takeIf { it > 0 } ?: decoder.sampleRate,
                channels = runCatching { parseChannels(h.channels) }.getOrDefault(0).takeIf { it > 0 } ?: decoder.channels,
                bitsPerSample = runCatching { h.bitsPerSample }.getOrDefault(0).takeIf { it > 0 } ?: decoder.bits,
                bitrateKbps = runCatching { h.bitRateAsNumber.toInt() }.getOrDefault(0),
                replayGainDb = tag?.let { readGain(it) },
                codec = ReleaseInfo.codecName(decoder.mime).ifEmpty { runCatching { h.encodingType.orEmpty() }.getOrDefault("") },
                codecMime = decoder.mime,
                container = container,
                encoder = encoderEntry?.second.orEmpty(),
                artwork = runCatching { tag?.firstArtwork?.binaryData }.getOrNull()?.let { artworkInfo(it) } ?: embeddedArtwork(song),
                extraTags = extras.filter { it !== encoderEntry && it.first !in RELEASE_TYPE_LABELS },
                releaseTypes = extras.filter { it.first in RELEASE_TYPE_LABELS }.map { it.second },
            )
        }
        fromLib ?: fallbackDetails(song, decoder, container)
    }

    private data class DecoderInfo(val mime: String = "", val sampleRate: Int = 0, val channels: Int = 0, val bits: Int = 0)

    /** What the platform decoder reports about the audio track. */
    data class Quality(val codecMime: String, val bits: Int, val explicit: Boolean)

    /** Codec MIME type, bit depth and the explicit-content flag, cheap enough to read for the whole library. */
    fun readQuality(song: Song): Quality {
        val d = decoderInfo(song)
        var bits = d.bits
        var explicit = false
        withAudioFile(song) { f ->
            if (bits == 0) bits = runCatching { f.audioHeader.bitsPerSample }.getOrDefault(0)
            explicit = f.tag?.let(::isExplicit) ?: false
        }
        return Quality(d.mime, bits, explicit)
    }

    /**
     * True when the tags mark the song as explicit: the iTunes rating atom (rtng 1 or 4), or an
     * ITUNESADVISORY / EXPLICIT field (TXXX, Vorbis comment or MP4 freeform) set to 1, yes or true.
     */
    private fun isExplicit(tag: Tag): Boolean {
        val fields = runCatching { tag.fields }.getOrNull() ?: return false
        while (fields.hasNext()) {
            val field = runCatching { fields.next() }.getOrNull() ?: break
            var id = runCatching { field.id }.getOrNull().orEmpty().uppercase()
            var value = runCatching { (field as? TagTextField)?.content }.getOrNull() ?: runCatching { field.toString() }.getOrNull()
            val body = (field as? AbstractID3v2Frame)?.body
            if (body is FrameBodyTXXX) { id = body.description.uppercase(); value = body.text }
            val v = value?.trim()?.trim('\u0000')?.lowercase().orEmpty()
            if (id == "RTNG" && (v == "1" || v == "4")) return true
            if ((id.endsWith("ITUNESADVISORY") || id.endsWith("EXPLICIT")) && (v == "1" || v == "yes" || v == "true" || v == "explicit")) return true
        }
        return false
    }

    private fun decoderInfo(song: Song): DecoderInfo {
        val ex = MediaExtractor()
        return try {
            ex.setDataSource(context, song.uri, null)
            for (i in 0 until ex.trackCount) {
                val fmt = ex.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (!mime.startsWith("audio/")) continue
                fun int(key: String) = if (fmt.containsKey(key)) runCatching { fmt.getInteger(key) }.getOrDefault(0) else 0
                val encoding = int("pcm-encoding")
                val bits = int("bits-per-sample").takeIf { it > 0 } ?: when (encoding) {
                    3 -> 8; 2 -> 16; 21 -> 24; 22, 4 -> 32; else -> 0
                }
                return DecoderInfo(mime, int(MediaFormat.KEY_SAMPLE_RATE), int(MediaFormat.KEY_CHANNEL_COUNT), bits)
            }
            DecoderInfo()
        } catch (e: Throwable) {
            DecoderInfo()
        } finally {
            ex.release()
        }
    }

    private fun artworkInfo(bytes: ByteArray): ArtworkInfo? {
        if (bytes.isEmpty()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return ArtworkInfo(opts.outWidth.coerceAtLeast(0), opts.outHeight.coerceAtLeast(0), FileInspector.imageFormat(bytes), bytes.size)
    }

    private fun embeddedArtwork(song: Song): ArtworkInfo? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, song.uri)
            mmr.embeddedPicture?.let { artworkInfo(it) }
        } catch (e: Throwable) {
            null
        } finally {
            runCatching { mmr.release() }
        }
    }

    /**
     * Every text tag that is not already shown in the main list (title, artist, album...),
     * with a readable label. Lyrics and pictures are left out.
     */
    private fun collectExtras(tag: Tag): List<Pair<String, String>> {
        val shown = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<TagField, Boolean>())
        for (key in MAIN_KEYS) runCatching { shown.addAll(tag.getFields(key)) }
        val out = ArrayList<Pair<String, String>>()
        val fields = runCatching { tag.fields }.getOrNull() ?: return out
        while (fields.hasNext()) {
            val field = runCatching { fields.next() }.getOrNull() ?: break
            if (field in shown || field.isBinary) continue
            val id = runCatching { field.id }.getOrNull().orEmpty()
            if (id.uppercase() in SKIPPED_IDS) continue
            var label = id
            var value = runCatching { (field as? TagTextField)?.content }.getOrNull()
            val body = (field as? AbstractID3v2Frame)?.body
            when (body) {
                is FrameBodyTXXX -> { label = body.description; value = body.text }
                is FrameBodyUSLT, is FrameBodySYLT -> continue
                is FrameBodyWXXX -> { label = body.description.ifBlank { "URL" }; value = body.urlLink }
                is AbstractFrameBodyUrlLink -> value = body.urlLink
            }
            if (value == null) value = runCatching { field.toString() }.getOrNull()
            val clean = value?.trim()?.trim('\u0000').orEmpty()
            if (clean.isEmpty() || clean.length > 4000) continue
            out += friendlyLabel(label) to clean
        }
        return out.distinct()
    }

    private fun friendlyLabel(id: String): String {
        val key = id.substringAfterLast(':').trim()
        LABELS[key.uppercase()]?.let { return it }
        LABELS[id]?.let { return it }
        return key.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }.ifEmpty { id }
    }

    private fun parseChannels(value: String?): Int {
        val v = value?.trim()?.lowercase().orEmpty()
        return v.toIntOrNull() ?: when {
            v.contains("mono") -> 1
            v.contains("stereo") -> 2
            else -> 0
        }
    }

    /** Uses the platform decoder information when the tag library cannot open the file. */
    private fun fallbackDetails(song: Song, decoder: DecoderInfo, container: FileInspector.Container): AudioDetails {
        val tags = TagFields(
            title = song.title, artist = song.artist, album = song.album, albumArtist = song.albumArtist,
            year = if (song.year > 0) song.year.toString() else "", genre = song.genre,
            track = if (song.track > 0) song.track.toString() else "", disc = if (song.disc > 0) song.disc.toString() else "",
            composer = song.composer,
        )
        return AudioDetails(
            tags, decoder.sampleRate, decoder.channels, decoder.bits, song.bitrate / 1000, null, readable = false,
            codec = ReleaseInfo.codecName(decoder.mime), codecMime = decoder.mime, container = container,
            artwork = embeddedArtwork(song),
        )
    }

    private fun readGain(tag: Tag): Float? {
        val candidates = ArrayList<String>()
        runCatching {
            if (tag is AbstractID3v2Tag) {
                for (field in tag.getFields("TXXX")) {
                    val body = (field as? AbstractID3v2Frame)?.body as? FrameBodyTXXX ?: continue
                    if (body.description.equals("replaygain_track_gain", ignoreCase = true)) candidates += body.text
                }
            } else {
                candidates += tag.getFields("REPLAYGAIN_TRACK_GAIN").map { it.toString() }
            }
        }
        return candidates.firstNotNullOfOrNull { c ->
            Regex("""-?\d+(\.\d+)?""").find(c)?.value?.toFloatOrNull()
        }
    }

    suspend fun readReplayGain(song: Song): Float? = withContext(Dispatchers.IO) {
        withAudioFile(song) { af -> af.tag?.let { readGain(it) } }
    }

    suspend fun readEmbeddedLyrics(song: Song): EmbeddedLyrics = withContext(Dispatchers.IO) {
        withAudioFile(song) { af ->
            val tag = af.tag ?: return@withAudioFile EmbeddedLyrics(null, null)
            var synced: String? = null
            runCatching {
                if (tag is AbstractID3v2Tag) {
                    val frame = tag.getFirstField("SYLT") as? AbstractID3v2Frame
                    val body = frame?.body as? FrameBodySYLT
                    if (body != null) synced = decodeSylt(body)
                }
            }
            val text = runCatching { tag.getFirst(FieldKey.LYRICS) }.getOrNull()?.takeIf { it.isNotBlank() }
            if (synced == null && text != null && LrcParser.isSynced(text)) EmbeddedLyrics(text, null)
            else EmbeddedLyrics(synced, text)
        } ?: EmbeddedLyrics(null, null)
    }

    /** Converts an ID3 synchronised lyrics frame into LRC text. Only millisecond timestamps are supported. */
    private fun decodeSylt(body: FrameBodySYLT): String? {
        if (body.timeStampFormat != 2) return null
        val data = body.lyrics ?: return null
        val enc = body.textEncoding.toInt()
        val charset: Charset = when (enc) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        val wide = enc == 1 || enc == 2
        val entries = ArrayList<Pair<Long, String>>()
        var i = 0
        while (i < data.size) {
            var end = i
            if (wide) {
                while (end + 1 < data.size && !(data[end].toInt() == 0 && data[end + 1].toInt() == 0)) end += 2
            } else {
                while (end < data.size && data[end].toInt() != 0) end++
            }
            val text = String(data, i, (end - i).coerceAtLeast(0), charset)
            i = end + if (wide) 2 else 1
            if (i + 4 > data.size) break
            val time = ((data[i].toLong() and 0xFF) shl 24) or ((data[i + 1].toLong() and 0xFF) shl 16) or
                ((data[i + 2].toLong() and 0xFF) shl 8) or (data[i + 3].toLong() and 0xFF)
            i += 4
            entries += time to text
        }
        if (entries.isEmpty()) return null
        val wordLevel = entries.any { it.second.contains('\n') } && entries.any { !it.second.contains('\n') }
        return buildString {
            if (!wordLevel) {
                for ((t, text) in entries) {
                    val clean = text.replace("\n", " ").trim()
                    append('[').append(LrcParser.formatTime(t)).append(']').append(clean).append('\n')
                }
            } else {
                var lineOpen = false
                for ((t, text) in entries) {
                    val startsNew = text.startsWith("\n") || !lineOpen
                    if (startsNew) {
                        if (lineOpen) append('\n')
                        append('[').append(LrcParser.formatTime(t)).append(']')
                        lineOpen = true
                    }
                    append('<').append(LrcParser.formatTime(t)).append('>').append(text.replace("\n", ""))
                    if (text.endsWith("\n") && text.length > 1) {
                        append('\n')
                        lineOpen = false
                    }
                }
                append('\n')
            }
        }
    }

    suspend fun readCover(song: Song): ByteArray? = withContext(Dispatchers.IO) {
        withAudioFile(song) { af -> runCatching { af.tag?.firstArtwork?.binaryData }.getOrNull() }
    }

    /** Android 11+ asks the user to allow the change; older versions rely on the storage permission. */
    fun writeRequest(songs: List<Song>): IntentSender? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createWriteRequest(context.contentResolver, songs.map { it.uri }).intentSender
        } else null

    /** Edits a private copy, then writes it back over the original through the media store. */
    suspend fun write(song: Song, fields: TagFields, cover: CoverChange): Result<Unit> = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "edit_${song.id}.${song.extension.ifEmpty { "mp3" }}")
        runCatching {
            context.contentResolver.openInputStream(song.uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                ?: error("unreadable")
            val af = AudioFileIO.read(tmp)
            val tag = af.tagOrCreateAndSetDefault
            fun put(key: FieldKey, value: String) {
                runCatching { if (value.isBlank()) tag.deleteField(key) else tag.setField(key, value.trim()) }
            }
            put(FieldKey.TITLE, fields.title)
            put(FieldKey.ARTIST, fields.artist)
            put(FieldKey.ALBUM, fields.album)
            put(FieldKey.ALBUM_ARTIST, fields.albumArtist)
            put(FieldKey.YEAR, fields.year)
            put(FieldKey.GENRE, fields.genre)
            put(FieldKey.TRACK, fields.track)
            put(FieldKey.DISC_NO, fields.disc)
            put(FieldKey.COMPOSER, fields.composer)
            put(FieldKey.BPM, fields.bpm)
            put(FieldKey.COMMENT, fields.comment)
            when (cover) {
                CoverChange.Keep -> Unit
                CoverChange.Remove -> tag.deleteArtworkField()
                is CoverChange.Set -> {
                    tag.deleteArtworkField()
                    val art = ArtworkFactory.getNew()
                    art.binaryData = cover.bytes
                    art.mimeType = cover.mime
                    art.pictureType = 3
                    tag.setField(art)
                }
            }
            af.commit()
            context.contentResolver.openOutputStream(song.uri, "wt")?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
                ?: error("unwritable")
            rescanFile(song.path)
        }.also { tmp.delete() }
    }

    private suspend fun rescanFile(path: String) = suspendCancellableCoroutine { cont ->
        MediaScannerConnection.scanFile(context, arrayOf(path), null) { _: String?, _: Uri? ->
            if (cont.isActive) cont.resume(Unit)
        }
    }

    private companion object {
        val MAIN_KEYS = listOf(
            FieldKey.TITLE, FieldKey.ARTIST, FieldKey.ALBUM, FieldKey.ALBUM_ARTIST, FieldKey.YEAR, FieldKey.GENRE,
            FieldKey.TRACK, FieldKey.TRACK_TOTAL, FieldKey.DISC_NO, FieldKey.DISC_TOTAL, FieldKey.COMPOSER,
            FieldKey.BPM, FieldKey.COMMENT, FieldKey.LYRICS, FieldKey.COVER_ART,
        )
        val SKIPPED_IDS = setOf("COVR", "METADATA_BLOCK_PICTURE", "COVERART", "APIC", "PIC", "USLT", "SYLT", "LYRICS", "UNSYNCEDLYRICS", "©LYR")
        const val ENCODER = "Encodeur"
        const val RELEASE_TYPE = "Type de sortie"
        val ENCODER_LABELS = setOf(ENCODER)
        val RELEASE_TYPE_LABELS = setOf(RELEASE_TYPE, "Musicbrainz album type", "MusicBrainz Album Type")
        val LABELS = mapOf(
            "©TOO" to ENCODER, "TSSE" to ENCODER, "ENCODER" to ENCODER, "ENCODING" to ENCODER, "ENCODED-BY" to "Encodé par",
            "TENC" to "Encodé par", "©ENC" to "Encodé par", "ENCODEDBY" to "Encodé par",
            "DESC" to "Description", "DESCRIPTION" to "Description", "LDES" to "Description longue", "PODCASTDESC" to "Description",
            "PURL" to "URL", "CPRT" to "Copyright", "TCOP" to "Copyright", "COPYRIGHT" to "Copyright",
            "TPUB" to "Label", "LABEL" to "Label", "ORGANIZATION" to "Label", "PUBLISHER" to "Label",
            "TSRC" to "ISRC", "ISRC" to "ISRC", "TLAN" to "Langue", "LANGUAGE" to "Langue",
            "RTNG" to "Contenu explicite", "ITUNESADVISORY" to "Contenu explicite", "EXPLICIT" to "Contenu explicite",
            "RELEASETYPE" to RELEASE_TYPE, "RELEASE TYPE" to RELEASE_TYPE, "MUSICBRAINZ ALBUM TYPE" to RELEASE_TYPE, "MUSICBRAINZ_ALBUMTYPE" to RELEASE_TYPE,
            "TDRL" to "Date de sortie", "RELEASEDATE" to "Date de sortie", "TDOR" to "Date originale", "ORIGINALDATE" to "Date originale",
            "TPE3" to "Chef d'orchestre", "CONDUCTOR" to "Chef d'orchestre", "TEXT" to "Parolier", "LYRICIST" to "Parolier",
            "TPE4" to "Remixé par", "REMIXER" to "Remixé par", "TIT1" to "Groupement", "©GRP" to "Groupement", "GROUPING" to "Groupement",
            "TIT3" to "Sous-titre", "SUBTITLE" to "Sous-titre", "TMOO" to "Ambiance", "MOOD" to "Ambiance", "TKEY" to "Tonalité", "KEY" to "Tonalité",
            "TMED" to "Support", "MEDIA" to "Support", "BARCODE" to "Code-barres", "CATALOGNUMBER" to "Référence catalogue",
            "SOAR" to "Tri artiste", "SONM" to "Tri titre", "SOAL" to "Tri album", "SOAA" to "Tri artiste de l'album",
            "TSOP" to "Tri artiste", "TSOT" to "Tri titre", "TSOA" to "Tri album",
            "PCST" to "Podcast", "CATG" to "Catégorie", "KEYW" to "Mots-clés", "STIK" to "Type de média", "CPIL" to "Compilation", "TCMP" to "Compilation",
            "COMPILATION" to "Compilation", "PGAP" to "Sans blanc", "©WRT" to "Compositeur", "WOAR" to "Site de l'artiste", "WOAS" to "Source",
            "WCOM" to "Achat", "WPUB" to "Site du label",
            "REPLAYGAIN_TRACK_GAIN" to "ReplayGain piste", "REPLAYGAIN_TRACK_PEAK" to "Crête piste",
            "REPLAYGAIN_ALBUM_GAIN" to "ReplayGain album", "REPLAYGAIN_ALBUM_PEAK" to "Crête album",
            "COMPATIBLE_BRANDS" to "Marques compatibles", "MAJOR_BRAND" to "Marque principale", "MINOR_VERSION" to "Version mineure",
        )
    }
}
