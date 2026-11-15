package com.rostomusic.app.core.design

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/**
 * True while the screen change involves an album, artist or playlist page. Covers only fly then:
 * switching between two tabs that show the same album must not send its cover across the screen.
 */
val LocalCoverFlight = compositionLocalOf { true }

/** The navigation transition of the screen being drawn. */
val LocalNavScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Key shared by a cover in a list and the banner of the page it opens, e.g. "album:42". */
fun coverKey(type: String, key: String) = "$type:$key"

/**
 * Covers no longer fly from a list into a page: the flight showed a small square cover over the
 * title before the banner appeared (seen on a phone, four fixes in a row). Pages now open with
 * the app's light fade (App.kt) and the banner fades in on the page colour. Kept as a no-op so
 * the lists need no change.
 */
@Composable
fun Modifier.sharedCover(key: String?): Modifier = this

/** No-op since covers stopped flying (see [sharedCover]). */
@Composable
fun Modifier.aboveSharedCovers(): Modifier = this
