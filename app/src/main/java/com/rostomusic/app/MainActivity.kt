package com.rostomusic.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.rostomusic.app.ui.AppViewModel
import com.rostomusic.app.ui.RostoApp
import com.rostomusic.app.ui.onboarding.audioPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // Pick up files added or removed since the last launch.
            handleIntent(intent)
        }
        setContent { RostoApp(app) }
    }

    override fun onStart() {
        super.onStart()
        // Pick up files added or removed while the app was away, including ones Android has not indexed.
        if (ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED) {
            app.library.watchMediaStore(applicationContext)
            app.library.refreshOnReturn()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) app.expandPlayer()
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "open_player"
    }
}
