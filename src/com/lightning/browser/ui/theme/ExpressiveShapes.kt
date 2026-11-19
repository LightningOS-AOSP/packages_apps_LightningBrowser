package com.lightning.browser.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Circle with [lobes] bumps. amplitude 0 = circle. rotationDeg rotates the bump pattern. */
@Immutable
class LobedShape(
    private val lobes: Int,
    private val amplitude: Float,
    private val rotationDeg: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val n = 72
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rMax = min(size.width, size.height) / 2f
        val rot = Math.toRadians(rotationDeg.toDouble())
        val pts = List(n) { k ->
            val t = 2.0 * Math.PI * k / n
            val r = rMax * (1f + amplitude * cos(lobes * (t - rot)).toFloat()) / (1f + amplitude)
            Offset(cx + (r * cos(t)).toFloat(), cy + (r * sin(t)).toFloat())
        }
        val path = Path()
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 0 until n) {
            val p0 = pts[(i - 1 + n) % n]
            val p1 = pts[i]
            val p2 = pts[(i + 1) % n]
            val p3 = pts[(i + 2) % n]
            path.cubicTo(
                p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                p2.x, p2.y,
            )
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is LobedShape &&
        lobes == other.lobes && amplitude == other.amplitude && rotationDeg == other.rotationDeg

    override fun hashCode() = (lobes * 31 + amplitude.hashCode()) * 31 + rotationDeg.hashCode()
}

object ExpressiveShapes {
    val Cookie9 = LobedShape(9, 0.06f)
    val Clover4 = LobedShape(4, 0.10f, 45f)
    val Flower6 = LobedShape(6, 0.09f)
    val Scallop12 = LobedShape(12, 0.045f)
    val Burst8 = LobedShape(8, 0.07f, 22.5f)
    val Penta5 = LobedShape(5, 0.07f, -90f)

    private val cycle = listOf(Cookie9, Clover4, Flower6, Scallop12, Burst8, Penta5)

    fun cycle(index: Int): Shape = cycle[index.mod(cycle.size)]

    /** Diagonal-asymmetric card: alternates by index. */
    fun diagonal(index: Int, big: Int = 44, small: Int = 12): Shape =
        if (index % 2 == 0) {
            RoundedCornerShape(big.dp, small.dp, big.dp, small.dp)
        } else {
            RoundedCornerShape(small.dp, big.dp, small.dp, big.dp)
        }

    /** Connected-list shape: big outer corners, tight inner corners. */
    fun grouped(index: Int, count: Int): Shape {
        val l = 28.dp
        val s = 6.dp
        return when {
            count <= 1 -> RoundedCornerShape(l)
            index == 0 -> RoundedCornerShape(l, l, s, s)
            index == count - 1 -> RoundedCornerShape(s, s, l, l)
            else -> RoundedCornerShape(s)
        }
    }
}