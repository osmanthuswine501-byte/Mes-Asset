# Aura Music Player — Material 3 Expressive (Jetpack Compose)

Application Android native de lecteur musical conçue de bout en bout selon les spécifications officielles [Material Design 3 Expressive](https://m3.material.io/).

## Architecture & Composants M3 Expressive

- **Thème & Fondations M3E** :
  - **Palette HCT Aura** : Surfaces lilas/lavande dynamiques (`surfaceContainerLowest` à `surfaceContainerHighest`), primaire indigo/violet (`#3F4E8D`), secondaire jaune/lime ensoleillé (`#E2E95C`), tertiaire corail/rose (`#854B64`).
  - **Typography Scale** : Traitements éditoriaux avec hiérarchie visuelle forte, chiffres display et titres emphasized.
  - **Motion Scheme Physics** : Ressorts spatiaux (`fastSpatial`, `defaultSpatial`, `slowSpatial`) avec rebond maîtrisé et amortissement critique sans dépassement pour les effets (`fastEffects`, `defaultEffects`).
  - **Catalogue Shapes M3E** : Échelle de rayons (4dp, 8dp, 12dp, 16dp, 20dp, 28dp, 32dp, 48dp, Pill 1000dp) et forme organique 12 lobes (`AuraFlowerShape`).

- **Composants Clés Implémentés** :
  - **Hero Morphing Flower Visualizer (`AuraFlowerVisualizer`)** : Fleur 12 lobes issue de l'étude de cas officielle Aura, morphant en temps réel selon le rythme, l'amplitude audio et la rotation physique.
  - **Slider Expressif (`AuraExpressiveSlider`)** : Conforme aux cotes officielles (hauteurs 16dp à 96dp, gap de 4dp à 8dp pour le handle vertical 4dp avec extrémités arrondies). Respect absolu du sens LTR pour la progression média (règle bidirectionnalité M3).
  - **Floating Toolbar (`AuraPlaybackFloatingToolbar`)** : Conteneur pilule de 64dp de hauteur, padding extérieur 8dp, espacement inter-items 4dp, bouton héroïque de lecture 56dp avec rebond spring physics.
  - **Connected Button Group (`AuraConnectedButtonGroup`)** : Espacement interne strict de 2dp, bordures extérieures arrondies (pill) et coins intérieurs 4dp/8dp avec expansion dynamique sélectionnée.
  - **Split Button (`AuraSplitButton`)** : Bouton d'action principal + déclencheur de menu avec espacement de 2dp et coins asymétriques pour le choix de qualité audio (Lossless 24-bit / Dolby Atmos).
  - **Loading Indicator (`AuraLoadingIndicator`)** : Conteneur 48dp, géométrie morphante 38dp.
  - **Multi-Browse Carousel (`AuraMultiBrowseCarousel`)** : Largeur dynamique adaptative et élévation 2.5D.
  - **Feuille Acoustique DSP & EQ (`EqualizerBottomSheet`)** : Traitement audio spatial 3D et égaliseur 5 bandes vertical.
  - **Scaffold Adaptatif (`AuraMainScreen`)** : Support compact (téléphone avec mini-player persistant) et écran large (tablette/pliables en Supporting Pane canonique).
