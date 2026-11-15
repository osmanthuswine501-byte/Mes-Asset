# Module App : Aura Music Player

Module principal Jetpack Compose pour Aura Music Player.

## Structure des Paquetages

- `com.aura.musicplayer.theme` : Définition des tokens M3E (couleurs HCT, typographie, échelle de coins 35-shapes, physique des ressorts `AuraMotionScheme`, thème Compose).
- `com.aura.musicplayer.components` : Composants natifs Jetpack Compose M3E (`AuraExpressiveSlider`, `AuraFlowerVisualizer`, `AuraPlaybackFloatingToolbar`, `AuraConnectedButtonGroup`, `AuraSplitButton`, `AuraLoadingIndicator`, `AuraMultiBrowseCarousel`, `AuraIcons`).
- `com.aura.musicplayer.model` : Modèles de données réactifs (`Track`, `PlaybackStatus`, `RepeatMode`, `AudioQuality`, `EqualizerBand`).
- `com.aura.musicplayer.viewmodel` : `MusicViewModel` avec simulation temporelle de lecture, scrubbing, sauts de pistes, gestion de favoris et égalisation DSP.
- `com.aura.musicplayer.ui` : Écrans (`NowPlayingScreen`, `LibraryScreen`, `EqualizerBottomSheet`, `AuraMainScreen`).
