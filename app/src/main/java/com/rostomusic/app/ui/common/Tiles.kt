package com.rostomusic.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rostomusic.app.R
import com.rostomusic.app.core.design.CoverArt
import com.rostomusic.app.core.design.IconBtn
import com.rostomusic.app.core.design.Radius
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.core.design.cookie9
import com.rostomusic.app.core.design.pressClickable
import com.rostomusic.app.core.design.sharedCover

/**
 * Tuile média M3 Expressive (Albums, Playlists, Artistes, Récents).
 * Règle 1 : la Column globale n'a pas de clickable ; le clic d'ouverture est isolé
 * sur la pochette et les textes, tandis que le bouton Play est indépendant en moyenne emphase.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaTile(
    albumId: Long,
    seed: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 136.dp,
    circle: Boolean = false,
    onPlay: (() -> Unit)? = null,
    sharedKey: String? = null,
) {
    val sized = if (width != null) modifier.width(width) else modifier
    Column(
        modifier = sized,
        horizontalAlignment = if (circle) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(if (circle) cookie9() else RoundedCornerShape(Radius.cover))
                    .pressClickable(role = Role.Button, onClick = onClick),
            ) {
                CoverArt(
                    albumId = albumId,
                    seed = seed,
                    modifier = Modifier
                        .sharedCover(sharedKey)
                        .fillMaxSize()
                        .then(if (circle) Modifier.clip(cookie9()) else Modifier),
                    radius = if (circle) 400.dp else Radius.cover,
                )
            }
            if (onPlay != null) {
                val description = stringResource(R.string.cd_play_item, title)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Space.x1),
                    contentAlignment = Alignment.Center,
                ) {
                    IconBtn(
                        icon = R.drawable.ic_play_filled,
                        contentDescription = description,
                        onClick = onPlay,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        size = 40.dp,
                        iconSize = 18.dp,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressClickable(role = Role.Button, onClick = onClick)
                .padding(top = Space.x2, bottom = Space.x1),
            horizontalAlignment = if (circle) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (circle) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (circle) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
