package com.rostomusic.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rostomusic.app.core.design.LocalNavScope
import com.rostomusic.app.core.design.LocalReduceMotion
import com.rostomusic.app.core.design.LocalSharedScope
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.RostoTheme
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.ui.collection.CollectionScreen
import com.rostomusic.app.ui.common.PlaylistPickerSheet
import com.rostomusic.app.ui.common.SongMenuSheet
import com.rostomusic.app.ui.equalizer.EqualizerRoute
import com.rostomusic.app.ui.home.HomeScreen
import com.rostomusic.app.ui.info.SongInfoScreen
import com.rostomusic.app.ui.library.LibraryScreen
import com.rostomusic.app.ui.lyrics.LyricsEditorScreen
import com.rostomusic.app.ui.onboarding.OnboardingScreen
import com.rostomusic.app.ui.player.BottomBar
import com.rostomusic.app.ui.player.SideNavRail
import com.rostomusic.app.ui.player.BottomBar
import com.rostomusic.app.ui.player.SideNavRailHeight
import com.rostomusic.app.ui.player.MiniPlayer
import com.rostomusic.app.ui.player.MiniPlayerHeight
import com.rostomusic.app.ui.player.PlayerScreen
import com.rostomusic.app.ui.search.SearchScreen
import com.rostomusic.app.ui.settings.SettingsScreen
import com.rostomusic.app.ui.stats.StatsScreen

@Composable
fun RostoApp(app: AppViewModel) {
    val settings = app.settings.collectAsStateWithLifecycle().value ?: return
    RostoTheme(settings) {
        CompositionLocalProvider(LocalApp provides app, LocalSettings provides settings) {
            Box(Modifier.fillMaxSize().background(Rosto.colors.background)) {
                if (!settings.onboardingDone) OnboardingScreen() else MainScaffold(app)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainScaffold(app: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val state by app.playerState.collectAsStateWithLifecycle()
    val expanded by app.playerExpanded.collectAsStateWithLifecycle()
    val songMenu by app.songMenu.collectAsState()
    val picker by app.playlistPicker.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val reduceMotion = LocalReduceMotion.current
    val hasSong = state.song != null
    val barVisible = route != Routes.LYRICS_EDITOR
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = when {
        !barVisible -> navBar
        hasSong -> BottomBarHeight + MiniPlayerHeight + navBar
        else -> BottomBarHeight + navBar
    }

    LaunchedEffect(Unit) {
        app.messages.collect { m ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(context.getString(m.res, *m.args.toTypedArray()))
        }
    }
    LaunchedEffect(hasSong) { if (!hasSong) app.collapsePlayer() }
    BackHandler(expanded) { app.collapsePlayer() }

    val spatialSpec = if (reduceMotion) androidx.compose.animation.core.snap() else MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val fastSpatialSpec = if (reduceMotion) androidx.compose.animation.core.snap() else MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val effectsSpec = if (reduceMotion) androidx.compose.animation.core.snap() else MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val enter = if (reduceMotion) fadeIn(effectsSpec) else fadeIn(effectsSpec) + scaleIn(fastSpatialSpec, initialScale = 0.98f)
    val exit = fadeOut(effectsSpec)
    val pageIn = if (reduceMotion) fadeIn(effectsSpec) else slideInVertically(spatialSpec) { it } + fadeIn(effectsSpec)
    val pageOut = if (reduceMotion) fadeOut(effectsSpec) else slideOutVertically(spatialSpec) { it } + fadeOut(effectsSpec)
    val quality by app.library.quality.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        com.rostomusic.app.core.design.LocalSongQuality provides quality,
        com.rostomusic.app.core.design.LocalNowPlaying provides com.rostomusic.app.core.design.NowPlaying(state.song?.id, state.isPlaying),
    ) {
    val onTabNavigate: (com.rostomusic.app.ui.player.Tab) -> Unit = { tab ->
        val existing = runCatching { nav.getBackStackEntry(tab.pattern) }.getOrNull()
        if (existing != null) {
            nav.popBackStack(existing.destination.id, inclusive = false)
        } else {
            nav.navigate(tab.target) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    SharedTransitionLayout {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp
            val effectiveBottomInset = when {
                !barVisible -> navBar
                isWide && hasSong -> MiniPlayerHeight + navBar
                isWide -> navBar
                else -> bottomInset
            }
            Row(Modifier.fillMaxSize()) {
                if (barVisible && isWide) {
                    SideNavRail(route = route, onNavigate = onTabNavigate, modifier = Modifier.fillMaxHeight())
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
            CompositionLocalProvider(LocalBottomInset provides effectiveBottomInset, LocalSharedScope provides this@SharedTransitionLayout, com.rostomusic.app.core.design.LocalSongQuality provides quality, com.rostomusic.app.core.design.LocalNowPlaying provides com.rostomusic.app.core.design.NowPlaying(state.song?.id, state.isPlaying)) {
                NavHost(
                    navController = nav,
                    startDestination = Routes.HOME,
                    modifier = Modifier.fillMaxSize(),
                    // Pages opened on top of a tab (album, artist, infos, settings...) rise from the bottom and
                    // go back down, like the full player; the screen below stays still. Tabs keep the fade.
                    enterTransition = { if (targetState.destination.route.isPage()) pageIn else enter },
                    exitTransition = { if (targetState.destination.route.isPage()) ExitTransition.None else exit },
                    popEnterTransition = { if (initialState.destination.route.isPage()) EnterTransition.None else enter },
                    popExitTransition = { if (initialState.destination.route.isPage()) pageOut else exit },
                ) {
                    composable(Routes.HOME) { Padded { HomeScreen(nav) } }
                    composable(Routes.SEARCH) { Padded { SearchScreen(nav) } }
                    composable(Routes.ALBUMS) { Padded { com.rostomusic.app.ui.library.AlbumsScreen(nav) } }
                    composable(Routes.SONGS) { Padded { com.rostomusic.app.ui.library.SongsScreen(nav) } }
                    composable(Routes.ARTISTS) { Padded { com.rostomusic.app.ui.library.ArtistsScreen(nav) } }
                    composable(
                        Routes.LIBRARY,
                        arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 }),
                    ) { e -> Padded { LibraryScreen(nav, e.arguments?.getInt("tab") ?: 0) } }
                    composable(Routes.SETTINGS) { Padded { SettingsScreen(nav) } }
                    composable(
                        Routes.COLLECTION,
                        arguments = listOf(
                            navArgument("type") { type = NavType.StringType },
                            navArgument("key") { type = NavType.StringType; defaultValue = "" },
                        ),
                    ) { NavScope { CollectionScreen(nav) } }
                    composable(Routes.INFO, arguments = listOf(navArgument("id") { type = NavType.LongType })) { Padded { SongInfoScreen(nav) } }
                    composable(Routes.LYRICS_EDITOR, arguments = listOf(navArgument("id") { type = NavType.LongType })) { Padded { LyricsEditorScreen(nav) } }
                    composable(Routes.EQUALIZER) { Padded { EqualizerRoute(nav) } }
                    composable(Routes.STATS) { Padded { StatsScreen(nav) } }
                    composable(Routes.CUSTOMIZE) { Padded { com.rostomusic.app.ui.home.CustomizeScreen(nav) } }
                }
            }

            val pageTone by app.pageTone.collectAsStateWithLifecycle()
            val playerTone by app.playerTone.collectAsStateWithLifecycle()
            // Status bar and gesture bar icons: dark on a light colour, light on a dark one. They follow
            // the open page's colour (Album, Artist, Infos), the full player's, or the theme.
            SystemBarIcons(
                top = if (expanded && hasSong) playerTone ?: Rosto.colors.background else pageTone?.second ?: Rosto.colors.background,
                bottom = if (expanded && hasSong) playerTone ?: Rosto.colors.background else pageTone?.second ?: Rosto.colors.card,
            )
            if (barVisible) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
              // One dock for the mini-player and the tabs; it takes the open page's colour (Album, Artist, Song).
              val tone = pageTone?.second
              val dock: @Composable () -> Unit = {
                val dc = Rosto.colors
                // Same pace as the page under it, so the dock never lags behind the page colour.
                val bg by androidx.compose.animation.animateColorAsState(
                    if (tone != null) dc.background else dc.card,
                    // In with the page (180 ms), out as soon as it leaves (120 ms): no colour left behind.
                    if (reduceMotion) androidx.compose.animation.core.snap() else MaterialTheme.motionScheme.fastEffectsSpec(), label = "dock",
                )
                Column(
                    Modifier.fillMaxWidth()
                        .animateContentSize(
                            animationSpec = if (reduceMotion) androidx.compose.animation.core.snap()
                            else MaterialTheme.motionScheme.defaultSpatialSpec()
                        )
                        .clip(RoundedCornerShape(topStart = Radius.card, topEnd = Radius.card))
                        .background(bg)
                        .navigationBarsPadding()
                ) {
                    AnimatedVisibility(
                        visible = hasSong && !expanded,
                        enter = fadeIn(effectsSpec) + slideInVertically(spatialSpec) { it / 2 },
                        exit = fadeOut(effectsSpec),
                    ) {
                        MiniPlayer(
                            state = state,
                            sharedScope = this@SharedTransitionLayout,
                            visibilityScope = this,
                            onExpand = { app.expandPlayer() },
                        )
                    }
                    if (!isWide) {
                        BottomBar(route = route, onNavigate = onTabNavigate)
                    }
                }
              }
              CompositionLocalProvider(com.rostomusic.app.core.design.LocalSongQuality provides quality) {
                  if (tone != null) com.rostomusic.app.core.design.ToneScope(tone) { dock() } else dock()
              }
            }

                }
            }

            AnimatedVisibility(
                visible = expanded && hasSong,
                enter = slideInVertically(spatialSpec) { it } + fadeIn(effectsSpec),
                exit = slideOutVertically(spatialSpec) { it } + fadeOut(effectsSpec),
            ) {
                PlayerScreen(nav = nav, sharedScope = this@SharedTransitionLayout, visibilityScope = this)
            }

            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (expanded) navBar + Space.x4 else bottomInset),
            ) { data ->
                Snackbar(
                    containerColor = Rosto.colors.textPrimary,
                    contentColor = Rosto.colors.background,
                    shape = RoundedCornerShape(Radius.card),
                    modifier = Modifier.padding(horizontal = Space.x4),
                ) { Text(data.visuals.message, style = Rosto.type.label) }
            }
        }
    }

    songMenu?.let { SongMenuSheet(it, nav) { app.songMenu.value = null } }
    }
    picker?.let { PlaylistPickerSheet(it) { app.playlistPicker.value = null } }
}

/** Every screen except the five tabs of the bottom bar is a page that opens on top. */
private fun String?.isPage() = this != null && this !in setOf(Routes.HOME, Routes.ALBUMS, Routes.SONGS, Routes.LIBRARY, Routes.ARTISTS)

/** Keeps a screen below the status bar. The collection screen draws its banner behind it instead. */
@Composable
private fun AnimatedContentScope.Padded(content: @Composable () -> Unit) {
    NavScope { Box(Modifier.fillMaxSize().background(Rosto.colors.background).statusBarsPadding()) { content() } }
}

/** Gives the screen's transition to the covers that grow into the next page. */
@Composable
private fun AnimatedContentScope.NavScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavScope provides this, content = content)
}

/** Sets the status bar and navigation bar icons to dark or light from the colours under them. */
@Composable
private fun SystemBarIcons(top: androidx.compose.ui.graphics.Color, bottom: androidx.compose.ui.graphics.Color) {
    val view = androidx.compose.ui.platform.LocalView.current
    val lightTop = top.luminance() > 0.5f
    val lightBottom = bottom.luminance() > 0.5f
    SideEffect {
        var ctx = view.context
        while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
        val window = (ctx as? android.app.Activity)?.window ?: return@SideEffect
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
        if (controller.isAppearanceLightStatusBars != lightTop) controller.isAppearanceLightStatusBars = lightTop
        if (controller.isAppearanceLightNavigationBars != lightBottom) controller.isAppearanceLightNavigationBars = lightBottom
    }
}
