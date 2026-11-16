package com.aura.musicplayer.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Corner Radius Scale Tokens:
 * - Extra Small: 4dp
 * - Small: 8dp
 * - Medium: 12dp
 * - Large: 16dp
 * - Large Increased: 20dp
 * - Extra Large: 28dp
 * - Extra Large Increased: 32dp
 * - Extra Extra Large: 48dp
 * - Full: 1000dp (Pill)
 */
object AuraCornerRadius {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val LargeIncreased: Dp = 20.dp
    val ExtraLarge: Dp = 28.dp
    val ExtraLargeIncreased: Dp = 32.dp
    val ExtraExtraLarge: Dp = 48.dp
    val Full: Dp = 1000.dp
}

val AuraShapes = Shapes(
    extraSmall = RoundedCornerShape(AuraCornerRadius.ExtraSmall),
    small = RoundedCornerShape(AuraCornerRadius.Small),
    medium = RoundedCornerShape(AuraCornerRadius.Medium),
    large = RoundedCornerShape(AuraCornerRadius.Large),
    extraLarge = RoundedCornerShape(AuraCornerRadius.ExtraLarge)
)

val AuraLargeIncreasedShape = RoundedCornerShape(AuraCornerRadius.LargeIncreased)
val AuraExtraLargeIncreasedShape = RoundedCornerShape(AuraCornerRadius.ExtraLargeIncreased)
val AuraExtraExtraLargeShape = RoundedCornerShape(AuraCornerRadius.ExtraExtraLarge)
val AuraPillShape = RoundedCornerShape(AuraCornerRadius.Full)

/**
 * 12-lobed Sunny/Flower Shape from the Material 3 Expressive Shape Library (Figma / Aura study).
 * Generates an organic scalloped petal outline with smooth bezier curves.
 */
class AuraFlowerShape(
    val petalCount: Int = 12,
    val innerRadiusRatio: Float = 0.82f,
    val smoothness: Float = 0.45f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = minOf(centerX, centerY)
        val innerRadius = outerRadius * innerRadiusRatio

        val totalPoints = petalCount * 2
        val angleStep = (2.0 * PI / totalPoints).toFloat()

        val points = mutableListOf<Offset>()
        for (i in 0 until totalPoints) {
            val angle = i * angleStep - (PI / 2).toFloat()
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            points.add(Offset(x, y))
        }

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            for (i in points.indices) {
                val current = points[i]
                val next = points[(i + 1) % points.size]
                val prev = points[(i - 1 + points.size) % points.size]
                val nextNext = points[(i + 2) % points.size]

                // Hermite / Catmull-Rom smooth control points
                val cp1x = current.x + (next.x - prev.x) * smoothness
                val cp1y = current.y + (next.y - prev.y) * smoothness
                val cp2x = next.x - (nextNext.x - current.x) * smoothness
                val cp2y = next.y - (nextNext.y - current.y) * smoothness

                path.cubicTo(cp1x, cp1y, cp2x, cp2y, next.x, next.y)
            }
            path.close()
        } else {
            path.addRect(Rect(0f, 0f, size.width, size.height))
        }

        return Outline.Generic(path)
    }
}
