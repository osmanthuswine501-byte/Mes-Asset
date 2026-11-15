package com.aura.musicplayer.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material 3 Expressive Motion Physics System
 * Driven by Spring tokens: Spatial (movement, size, shape with overshoot/bounce)
 * and Effects (color, opacity with critical damping, zero overshoot).
 */
@Immutable
data class AuraMotionScheme(
    val fastSpatial: AnimationSpec<Float> = spring(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMedium
    ),
    val defaultSpatial: AnimationSpec<Float> = spring(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessLow
    ),
    val slowSpatial: AnimationSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessVeryLow
    ),
    val fastEffects: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    ),
    val defaultEffects: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    ),
    val slowEffects: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
)

val LocalAuraMotionScheme = staticCompositionLocalOf { AuraMotionScheme() }

object AuraMotion {
    val Scheme: AuraMotionScheme
        get() = AuraMotionScheme()
}
