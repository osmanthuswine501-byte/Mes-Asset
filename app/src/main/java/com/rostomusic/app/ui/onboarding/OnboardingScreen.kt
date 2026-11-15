package com.rostomusic.app.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rostomusic.app.R
import com.rostomusic.app.core.design.PrimaryButton
import com.rostomusic.app.core.design.Rosto
import com.rostomusic.app.core.design.SecondaryButton
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.data.repo.ScanState
import com.rostomusic.app.ui.LocalApp
import com.rostomusic.app.ui.player.Vinyl
import kotlinx.coroutines.launch

fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

/** First launch: explains the permission, scans the phone, then opens the app. */
@Composable
fun OnboardingScreen() {
    val app = LocalApp.current
    val c = Rosto.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scan by app.library.scanState.collectAsStateWithLifecycle()
    var denied by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[audioPermission()] == true) {
            denied = false
            scanning = true
            scope.launch {
                app.library.rescan()
                app.settingsStore.setOnboardingDone(true)
            }
        } else denied = true
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Space.x8, vertical = Space.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Vinyl(null, "rostomusic", spinning = scanning, modifier = Modifier.size(176.dp))
        Spacer(Modifier.height(Space.x8))
        Text(stringResource(R.string.app_name), style = Rosto.type.display, color = c.textPrimary)
        Spacer(Modifier.height(Space.x2))
        if (scanning) {
            val count = (scan as? ScanState.Scanning)?.count ?: 0
            Text(stringResource(R.string.onboarding_scanning), style = Rosto.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Space.x5))
            com.rostomusic.app.core.design.RostoLoading()
            Spacer(Modifier.height(Space.x3))
            Text(pluralStringResource(R.plurals.songs_found, count, count), style = Rosto.type.label, color = c.textMuted)
        } else {
            Text(
                stringResource(if (denied) R.string.onboarding_denied else R.string.onboarding_body),
                style = Rosto.type.body, color = c.textSecondary, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.x8))
            PrimaryButton(stringResource(R.string.onboarding_allow), {
                val wanted = buildList {
                    add(audioPermission())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                launcher.launch(wanted.toTypedArray())
            }, Modifier.fillMaxWidth())
            if (denied) {
                Spacer(Modifier.height(Space.x3))
                SecondaryButton(stringResource(R.string.onboarding_open_settings), {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(Space.x4))
            Text(stringResource(R.string.onboarding_privacy), style = Rosto.type.caption, color = c.textMuted, textAlign = TextAlign.Center)
        }
    }
}
