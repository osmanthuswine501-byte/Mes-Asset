package com.rostomusic.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.RIcon
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.ui.Routes

/** Top of every tab: a search field that opens the search screen, and the settings gear on its right. */
@Composable
fun TopSearchBar(nav: NavController, modifier: Modifier = Modifier) {
    val c = Rosto.colors
    Row(
        modifier.padding(horizontal = Space.screen).padding(top = Space.x3).fillMaxWidth().height(56.dp)
            .clip(CircleShape).background(c.surfaceContainerHigh)
            .clickable(role = Role.Button) { nav.navigate(Routes.SEARCH) { launchSingleTop = true } }
            .padding(start = Space.x4, end = Space.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RIcon(R.drawable.ic_search, null, tint = c.textSecondary, size = 24.dp)
        Spacer(Modifier.width(Space.x3))
        Box(Modifier.weight(1f)) {
            Text(stringResource(R.string.search_your_music), style = Rosto.type.body, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconBtn(
            R.drawable.ic_settings, stringResource(R.string.nav_settings), { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            tint = c.textPrimary, container = c.surfaceContainerHighest, size = 48.dp, iconSize = 22.dp,
        )
    }
}
