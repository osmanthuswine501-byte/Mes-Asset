package com.rostomusic.app.core.design

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The app's touch feedback: the touched element darkens slightly inside its own bounds, then fades
 * back. Replaces Material's ripple, whose large circles spilled over the bar and the rows.
 */
object PressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressNode(interactionSource)
    override fun hashCode(): Int = -0x5eed
    override fun equals(other: Any?): Boolean = other === this
}

private class PressNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val level = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { i ->
                when (i) {
                    is PressInteraction.Press -> launch { level.animateTo(1f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessHigh)) }
                    is PressInteraction.Release, is PressInteraction.Cancel -> launch { level.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val v = level.value
        if (v > 0f) {
            val ink = currentValueOf(LocalRostoColors).textPrimary
            drawRect(ink.copy(alpha = 0.10f * v))
        }
    }
}

/**
 * Press feedback for buttons, chips, tiles and covers (emil-design-eng): the element shrinks to 0.97
 * while held and comes back in 160 ms. Put it first in the chain so the background scales too, and
 * pass the same [source] to the element's `clickable`. Reduced motion keeps only the tint.
 */
fun @OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Modifier.pressScale(source: InteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed && !reduce) 0.97f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** A clickable that also scales on press (see [pressScale]). */
fun Modifier.pressClickable(enabled: Boolean = true, role: Role? = null, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    pressScale(source).clickable(source, LocalIndication.current, enabled = enabled, role = role, onClick = onClick)
}

/**
 * Material 3 Expressive shape morph: the corner starts at [rest] and springs to [pressed] while the
 * element is held or [selected], then back. Fully round buttons and chips become soft squares.
 * Reduced motion jumps straight to the end value.
 */
@androidx.compose.runtime.Composable
fun morphCorner(
    source: InteractionSource,
    rest: androidx.compose.ui.unit.Dp = Radius.disc,
    pressed: androidx.compose.ui.unit.Dp = Radius.morph,
    selected: Boolean = false,
): androidx.compose.ui.unit.Dp {
    val down by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val target = if (down || selected) pressed else rest
    val corner by androidx.compose.animation.core.animateDpAsState(
        target,
        if (reduce) androidx.compose.animation.core.snap()
        else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "morphCorner",
    )
    return corner.coerceAtLeast(0.dp)
}
