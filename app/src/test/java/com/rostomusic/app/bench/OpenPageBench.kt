package com.rostomusic.app.bench

import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import com.rostomusic.app.MainActivity
import com.rostomusic.app.RostoApplication
import com.rostomusic.app.data.db.RostoDatabase
import com.rostomusic.app.data.db.Song
import com.rostomusic.app.data.repo.LibraryRepository
import com.rostomusic.app.data.settings.SettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.security.MessageDigest

/**
 * Frame-by-frame bench of the page openings filmed in the bug videos. Each case taps like a user,
 * then saves the screen at fixed times after the tap into build/bench/, so every frame can be checked
 * before a version is sent. Times are those of the app's own clock (animations); pictures and the
 * fake Internet run in real time alongside.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class OpenPageBench {
    @get:Rule val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<RostoApplication>()
    private val out = File("build/bench").apply { mkdirs() }

    @Before
    fun seed() {
        FakeNet.install()
        FakeNet.gate = java.util.concurrent.CountDownLatch(1)
        // No playback service on the bench: the player stays disconnected, as before a song is chosen.
        shadowOf(app).declareComponentUnbindable(android.content.ComponentName(app, com.rostomusic.app.playback.PlaybackService::class.java))
        Coil.setImageLoader(
            ImageLoader.Builder(app).components { add(FakeArtFetcher.Factory(app)) }.crossfade(true).build()
        )
        val prefetcher = app.artistPrefetcher
        val library = field<LibraryRepository>(prefetcher, "library")
        val settings = field<SettingsStore>(prefetcher, "settings")
        val db = field<RostoDatabase>(library, "db")
        // Eminem's photo and bio were fetched on an earlier day; La Fouine and Ceasar have none.
        // Lion Hill was never looked up: its page has to ask the (slow) Internet.
        artistCache("Eminem", "https://img.test/eminem.jpg", "Marshall Bruce Mathers III alias Eminem est un rappeur americain, auteur-compositeur et producteur.")
        artistCache("La Fouine", "", "")
        artistCache("Ceasar", "", "")
        runBlocking {
            settings.setOnboardingDone(true)
            db.songs().upsert(SONGS)
        }
        waitUntil(5000) { library.songs.value.size == SONGS.size && library.artists.value.isNotEmpty() }
        // Let the background prefetch of the artist photos run, as it does after the app starts.
        repeat(40) { Thread.sleep(50); idle() }
    }

    @Test
    fun artistWithPhoto() = openFromTab("Artistes", "Eminem", "artist_photo")

    @Test
    fun artistWithoutPhoto() = openFromTab("Artistes", "La Fouine", "artist_nophoto")

    @Test
    fun artistBioLate() = openFromTab("Artistes", "Lion Hill", "artist_late")

    @Test
    fun album() = openFromTab("Albums", "Milliard", "album")

    @Test
    fun navBar() {
        launch()
        shot("nav_0_home")
        for ((i, tab) in listOf("Albums", "Morceaux", "Bibliothèque", "Artistes", "Accueil").withIndex()) {
            compose.onAllNodesWithContentDescription(tab).onFirst().performClick()
            for (t in listOf(0, 50, 120, 300)) {
                advanceTo(t)
                shot("nav_${i + 1}_${tab}_${"%03d".format(t)}ms")
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    private var clock = 0

    private fun openFromTab(tab: String, item: String, name: String) {
        launch()
        compose.onAllNodesWithContentDescription(tab).onFirst().performClick()
        advance(800)
        shot("${name}_00_list")
        compose.onAllNodesWithText(item).onFirst().performClick()
        clock = 0
        val frames = if (name == "artist_late") FRAMES + listOf(1100, 1200, 1300, 1400, 1600, 2000) else FRAMES
        for (t in frames) {
            advanceTo(t)
            shot("${name}_1_open_${"%04d".format(t)}ms")
            if (t == 300) bars(name)
            // Lion Hill's biography arrives one second after the page opened.
            if (t == 1000) { FakeNet.gate.countDown() }
        }
        ActivityScenario::class.java // keep import
        FakeNet.gate.countDown()
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        clock = 0
        for (t in listOf(0, 50, 100, 200, 300, 500)) {
            advanceTo(t)
            shot("${name}_2_back_${"%04d".format(t)}ms")
        }
    }

    private var scenario: ActivityScenario<MainActivity>? = null

    private fun launch() {
        compose.mainClock.autoAdvance = false
        scenario = ActivityScenario.launch(MainActivity::class.java)
        advance(1500)
    }

    /** Moves the app clock to [t] ms after the tap, one 16 ms frame at a time, with real time passing too. */
    private fun advanceTo(t: Int) {
        if (t > clock) advance(t - clock)
        clock = t
    }

    private fun advance(ms: Int) {
        var left = ms
        while (left > 0) {
            val step = minOf(16, left)
            Thread.sleep(step.toLong())
            idle()
            compose.mainClock.advanceTimeBy(step.toLong())
            left -= step
        }
        idle()
    }

    /** Writes whether the status / navigation bar icons are dark (for a light page) at 300 ms. */
    private fun bars(name: String) {
        scenario?.onActivity { a ->
            val c = androidx.core.view.WindowCompat.getInsetsController(a.window, a.window.decorView)
            File(out, "bars.txt").appendText("$name: status icons ${if (c.isAppearanceLightStatusBars) "DARK" else "LIGHT"}, nav icons ${if (c.isAppearanceLightNavigationBars) "DARK" else "LIGHT"}\n")
        }
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun shot(name: String) {
        compose.onRoot().captureRoboImage(File(out, "$name.png").path)
    }

    private fun waitUntil(ms: Long, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + ms
        while (!cond() && System.currentTimeMillis() < end) { Thread.sleep(20); idle() }
        check(cond()) { "bench data not ready" }
    }

    private fun artistCache(name: String, image: String, bio: String) {
        val dir = File(app.filesDir, "artist_info").apply { mkdirs() }
        val hash = MessageDigest.getInstance("SHA-1").digest(name.lowercase().toByteArray()).joinToString("") { "%02x".format(it) }
        File(dir, "$hash.json").writeText(
            JSONObject().put("image", image).put("bio", bio).put("url", "").put("at", System.currentTimeMillis()).toString()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> field(owner: Any, name: String): T =
        owner.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(owner) as T

    companion object {
        val FRAMES = listOf(0, 16, 33, 50, 83, 116, 150, 200, 250, 300, 400, 600, 1000)

        private fun song(id: Long, title: String, artist: String, album: String, albumId: Long, track: Int) = Song(
            id = id, title = title, artist = artist, album = album, albumArtist = artist, albumId = albumId,
            genre = "Rap", year = 2025, track = track, disc = 1, duration = 180_000, size = 4_000_000,
            path = "/music/$artist/$title.mp3", folder = "/music/$artist", mime = "audio/mpeg", bitrate = 320_000,
            composer = "", dateAdded = 1_700_000_000L + id, dateModified = 1_700_000_000L + id,
        )

        val SONGS = listOf(
            song(1, "Milliard", "Ceasar", "Milliard", 10, 1),
            song(2, "Lose Yourself", "Eminem", "8 Mile", 11, 1),
            song(3, "Without Me", "Eminem", "The Eminem Show", 12, 2),
            song(4, "Mockingbird", "Eminem", "Encore", 13, 3),
            song(5, "Rostova", "La Fouine", "Rostova", 14, 1),
            song(6, "Ma meilleure", "La Fouine", "Drôle de parcours", 15, 1),
            song(7, "Tia vola", "Lion Hill", "Angala Fitia", 16, 1),
        )
    }
}
