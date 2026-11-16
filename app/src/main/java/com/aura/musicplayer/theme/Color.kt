package com.aura.musicplayer.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Obsidian Dark Flagship Palette (Aura Neon Midnight Signature)
val AuraObsidianBackground = Color(0xFF0F0D16)
val AuraObsidianOnBackground = Color(0xFFEBE6F5)
val AuraObsidianSurface = Color(0xFF0F0D16)
val AuraObsidianOnSurface = Color(0xFFEBE6F5)

val AuraNeonPrimary = Color(0xFF7C4DFF) // Electric Violet
val AuraNeonOnPrimary = Color(0xFFFFFFFF)
val AuraNeonPrimaryContainer = Color(0xFF2E1B5B)
val AuraNeonOnPrimaryContainer = Color(0xFFD6C8FF)

val AuraNeonSecondary = Color(0xFF00E5FF) // Luminous Cyan
val AuraNeonOnSecondary = Color(0xFF00363D)
val AuraNeonSecondaryContainer = Color(0xFF004F58)
val AuraNeonOnSecondaryContainer = Color(0xFF9CF4FF)

val AuraNeonTertiary = Color(0xFFE040FB) // Neon Magenta
val AuraNeonOnTertiary = Color(0xFF4A0059)
val AuraNeonTertiaryContainer = Color(0xFF6B0080)
val AuraNeonOnTertiaryContainer = Color(0xFFFFD6FE)

val AuraDarkSurfaceContainerLowest = Color(0xFF0A0910)
val AuraDarkSurfaceContainerLow = Color(0xFF161322)
val AuraDarkSurfaceContainer = Color(0xFF1D182E)
val AuraDarkSurfaceContainerHigh = Color(0xFF26203B)
val AuraDarkSurfaceContainerHighest = Color(0xFF30294A)

val AuraDarkOutline = Color(0xFF8F88A3)
val AuraDarkOutlineVariant = Color(0xFF433D54)

val AuraDarkColorScheme: ColorScheme = darkColorScheme(
    primary = AuraNeonPrimary,
    onPrimary = AuraNeonOnPrimary,
    primaryContainer = AuraNeonPrimaryContainer,
    onPrimaryContainer = AuraNeonOnPrimaryContainer,
    secondary = AuraNeonSecondary,
    onSecondary = AuraNeonOnSecondary,
    secondaryContainer = AuraNeonSecondaryContainer,
    onSecondaryContainer = AuraNeonOnSecondaryContainer,
    tertiary = AuraNeonTertiary,
    onTertiary = AuraNeonOnTertiary,
    tertiaryContainer = AuraNeonTertiaryContainer,
    onTertiaryContainer = AuraNeonOnTertiaryContainer,
    background = AuraObsidianBackground,
    onBackground = AuraObsidianOnBackground,
    surface = AuraObsidianSurface,
    onSurface = AuraObsidianOnSurface,
    surfaceVariant = Color(0xFF2D2545),
    onSurfaceVariant = Color(0xFFA59FBA),
    surfaceContainerLowest = AuraDarkSurfaceContainerLowest,
    surfaceContainerLow = AuraDarkSurfaceContainerLow,
    surfaceContainer = AuraDarkSurfaceContainer,
    surfaceContainerHigh = AuraDarkSurfaceContainerHigh,
    surfaceContainerHighest = AuraDarkSurfaceContainerHighest,
    outline = AuraDarkOutline,
    outlineVariant = AuraDarkOutlineVariant
)

// Light Palette (Alternative)
val AuraLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF5E35B1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF280680),
    secondary = Color(0xFF00B0FF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1F5FE),
    onSecondaryContainer = Color(0xFF003C8F),
    tertiary = Color(0xFFD81B60),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCE4EC),
    onTertiaryContainer = Color(0xFF880E4F),
    background = Color(0xFFF9F7FC),
    onBackground = Color(0xFF1C1A24),
    surface = Color(0xFFF9F7FC),
    onSurface = Color(0xFF1C1A24),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2EFF7),
    surfaceContainer = Color(0xFFEBE6F2),
    surfaceContainerHigh = Color(0xFFE4DFEC),
    surfaceContainerHighest = Color(0xFFDDD8E6),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)
