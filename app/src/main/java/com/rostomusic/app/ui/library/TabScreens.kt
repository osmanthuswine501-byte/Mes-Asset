package com.rostomusic.app.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import com.rostomusic.app.R
import com.rostomusic.app.core.design.ScreenTitle
import com.rostomusic.app.core.design.SkeletonRows
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.common.TopSearchBar

/** The Albums, Morceaux and Artistes tabs of the bar: the library lists, shown on their own. */
@Composable
private fun TabScreen(nav: NavController, title: String, content: @Composable () -> Unit) {
    val app = LocalApp.current
    val loaded by app.library.loaded.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopSearchBar(nav)
        // The tab's name, large, under the search bar: the user always knows where they are.
        ScreenTitle(title, Modifier.padding(start = Space.screen, end = Space.screen, top = Space.x4, bottom = Space.x1))
        if (loaded) content() else SkeletonRows()
    }
}

@Composable
fun AlbumsScreen(nav: NavController) {
    val vm: LibraryViewModel = hiltViewModel()
    TabScreen(nav, stringResource(R.string.nav_albums)) { AlbumsTab(vm, nav) }
}

@Composable
fun SongsScreen(nav: NavController) {
    val vm: LibraryViewModel = hiltViewModel()
    TabScreen(nav, stringResource(R.string.nav_songs)) { SongsTab(vm) }
}

@Composable
fun ArtistsScreen(nav: NavController) {
    TabScreen(nav, stringResource(R.string.nav_artists)) { ArtistsTab(nav) }
}
