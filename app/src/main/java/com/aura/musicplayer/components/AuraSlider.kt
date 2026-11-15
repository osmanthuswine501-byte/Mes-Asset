package com.aura.musicplayer.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class AuraSliderSize(
    val trackHeight: Dp,
    val handleHeight: Dp,
    val handleWidth: Dp,
    val cornerRadius: Dp,
    val gap: Dp
) {
    XS(16.dp, 44.dp, 4.dp, 8.dp, 6.dp),
    S(24.dp, 44.dp, 4.dp, 8.dp, 6.dp),
    M(40.dp, 52.dp, 4.dp, 12.dp, 6.dp),
    L(56.dp, 68.dp, 4.dp, 16.dp, 6.dp),
    XL(96.dp, 108.dp, 4.dp, 28.dp, 8.dp)
}

/**
 * Official Material 3 Expressive Slider Component (m3.material.io/components/sliders/specs).
 * Features:
 * - Active & Inactive rounded tracks with official gap for the vertical bar handle.
 * - Precision scrubbing with drag & tap gestures.
 * - Bidirectionality compliance: Audio progress is always LTR.
 * - Spring physics animation during release or jump.
 */
@Composable
fun AuraExpressiveSlider(
    value: Float, // 0f .. 1f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: AuraSliderSize = AuraSliderSize.S,
    enabled: Boolean = true,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
    handleColor: Color = MaterialTheme.colorScheme.primary,
    onValueChangeFinished: (() -> Unit)? = null
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(value) }

    val currentFraction = if (isDragging) dragProgress else value
    val animatedFraction by animateFloatAsState(
        targetValue = currentFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f),
        label = "SliderFractionAnim"
    )

    // Audio scrub bars must strictly render LTR per M3 guidelines
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(size.handleHeight.coerceAtLeast(48.dp))
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = currentFraction,
                        range = 0f..1f
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / this@pointerInput.size.width.toFloat()).coerceIn(0f, 1f)
                        onValueChange(newProgress)
                        onValueChangeFinished?.invoke()
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / this@pointerInput.size.width.toFloat()).coerceIn(0f, 1f)
                            onValueChange(dragProgress)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / this@pointerInput.size.width.toFloat()).coerceIn(0f, 1f)
                            onValueChange(dragProgress)
                        },
                        onDragEnd = {
                            isDragging = false
                            onValueChangeFinished?.invoke()
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(size.handleHeight)
                    .padding(horizontal = size.handleWidth / 2)
            ) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height

                val trackHeightPx = size.trackHeight.toPx()
                val handleHeightPx = size.handleHeight.toPx()
                val handleWidthPx = size.handleWidth.toPx()
                val cornerRadiusPx = size.cornerRadius.toPx()
                val gapPx = size.gap.toPx()

                val trackTop = (canvasHeight - trackHeightPx) / 2f
                val handleTop = (canvasHeight - handleHeightPx) / 2f

                val displayFraction = if (isDragging) dragProgress else animatedFraction
                val handleCenterX = canvasWidth * displayFraction

                // 1. Draw Active Track (Left)
                val activeTrackRight = (handleCenterX - gapPx / 2f).coerceAtLeast(0f)
                if (activeTrackRight > 0f) {
                    val activePath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(
                                    left = 0f,
                                    top = trackTop,
                                    right = activeTrackRight,
                                    bottom = trackTop + trackHeightPx
                                ),
                                topLeft = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                bottomLeft = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                topRight = CornerRadius(cornerRadiusPx.coerceAtMost(activeTrackRight / 2f), cornerRadiusPx),
                                bottomRight = CornerRadius(cornerRadiusPx.coerceAtMost(activeTrackRight / 2f), cornerRadiusPx)
                            )
                        )
                    }
                    drawPath(activePath, color = activeTrackColor)
                }

                // 2. Draw Inactive Track (Right)
                val inactiveTrackLeft = (handleCenterX + gapPx / 2f).coerceAtMost(canvasWidth)
                if (inactiveTrackLeft < canvasWidth) {
                    val inactivePath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(
                                    left = inactiveTrackLeft,
                                    top = trackTop,
                                    right = canvasWidth,
                                    bottom = trackTop + trackHeightPx
                                ),
                                topLeft = CornerRadius(cornerRadiusPx.coerceAtMost((canvasWidth - inactiveTrackLeft) / 2f), cornerRadiusPx),
                                bottomLeft = CornerRadius(cornerRadiusPx.coerceAtMost((canvasWidth - inactiveTrackLeft) / 2f), cornerRadiusPx),
                                topRight = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                bottomRight = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                        )
                    }
                    drawPath(inactivePath, color = inactiveTrackColor)
                }

                // 3. Draw Handle (Vertical Bar)
                val handleLeft = (handleCenterX - handleWidthPx / 2f).coerceIn(0f, canvasWidth - handleWidthPx)
                drawRoundRect(
                    color = handleColor,
                    topLeft = Offset(handleLeft, handleTop),
                    size = Size(handleWidthPx, handleHeightPx),
                    cornerRadius = CornerRadius(handleWidthPx / 2f, handleWidthPx / 2f)
                )
            }
        }
    }
}
