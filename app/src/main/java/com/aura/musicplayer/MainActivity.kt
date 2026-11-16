package com.aura.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aura.musicplayer.theme.AuraMusicPlayerTheme
import com.aura.musicplayer.ui.AuraMainScreen
import com.aura.musicplayer.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraMusicPlayerTheme {
                AuraMainScreen(viewModel = musicViewModel)
            }
        }
    }
}
