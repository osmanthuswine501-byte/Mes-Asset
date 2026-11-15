# Rostomusic

Lecteur de musique local pour Android (Kotlin, Jetpack Compose, Media3). Sans compte, sans publicité, sans pistage.

## Compiler

1. Ouvrir le dossier dans Android Studio (version récente), laisser Gradle se synchroniser.
2. Brancher un téléphone Android 7 ou plus récent, puis lancer `app`.

En ligne de commande : `./gradlew assembleDebug` (ou `gradle assembleDebug`). L'APK sort dans `app/build/outputs/apk/debug/`.

## Fonctions

- Formats : MP3, AAC/M4A, FLAC, WAV, OGG, Opus ; ALAC, AIFF et WMA selon le décodeur du téléphone.
- Accueil bento, recherche (titres, artistes, albums, playlists, paroles), bibliothèque en 6 onglets avec tri, filtres et défilement alphabétique.
- Lecteur plein écran : pochette ou vinyle animé avec bras, couleur tirée de la pochette, ligne fine ou forme d'onde, gestes, badges format et débit.
- Paroles synchronisées : tags SYLT/USLT, fichiers .lrc voisins, LRC mot à mot, décalage par morceau, toucher une ligne pour y aller, éditeur avec synchronisation au tap, export .lrc, recherche LRCLIB optionnelle.
- File d'attente réordonnable, aléatoire, répétition, fondu, vitesse 0,5× à 2×, ±10 s, minuterie, marque-pages, ReplayGain.
- Égaliseur, renfort des basses, spatialisation.
- Infos complètes du morceau et édition des tags et de la pochette.
- Statistiques, playlists manuelles et automatiques, import/export M3U, sauvegarde JSON.
- Notification, écran verrouillé, Bluetooth, Android Auto, widget d'écran d'accueil.

## Structure

- `domain/` : logique pure (analyseur LRC, file d'attente, M3U), testée dans `app/src/test`.
- `data/` : base Room, réglages, scan, tags, paroles, sauvegardes.
- `playback/` : service Media3, effets audio, minuterie, forme d'onde.
- `core/design/` : jetons de couleur, typographie, espacements et composants.
- `ui/` : écrans.

Police Plus Jakarta Sans (OFL), icônes Lucide (ISC).
