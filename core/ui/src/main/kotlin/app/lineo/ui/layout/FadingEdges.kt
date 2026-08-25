package app.lineo.ui.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Softens the top and bottom edges of a scrolling pane, so content leaving it dissolves
 * rather than ends.
 *
 * A document taller than its pane is cut by the pane's edge: half a digit at the top and
 * half a digit at the bottom, both of them sharp, and a sharp cut reads as a rendering
 * fault rather than as "there is more above this". Two short gradients in the pane's own
 * colour turn the cut into a fade — the same trick a fading scroll edge has always been.
 *
 * A gradient rather than a real blur: a blur reads the pixels behind it, which needs
 * `RenderEffect` and API 31, and it would blur the edge of the *pane* rather than the part
 * of the line that is leaving it. The gradient costs two rectangles and works everywhere.
 *
 * Drawn after the content and in [color], which must be what the pane is painted with, or
 * the fade goes to the wrong colour and shows as a band.
 *
 * @param color the pane's own background. There is no default: only the caller knows what
 *   it painted, and a guess would be visible.
 * @param height how far the fade reaches. One line of a result, roughly — far enough to
 *   dissolve a glyph and not so far that a line in the middle of the pane is dimmed.
 */
fun Modifier.fadingVerticalEdges(color: Color, height: Dp = FADE_HEIGHT): Modifier = drawWithContent {
    drawContent()
    val fade = height.toPx().coerceAtMost(size.height / 2f)
    if (fade <= 0f) return@drawWithContent
    drawRect(
        brush = Brush.verticalGradient(listOf(color, Color.Transparent), startY = 0f, endY = fade),
        size = Size(size.width, fade),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, color),
            startY = size.height - fade,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height - fade),
        size = Size(size.width, fade),
    )
}

/** Enough to dissolve a glyph: a little under a line of the result type. */
private val FADE_HEIGHT: Dp = 32.dp
