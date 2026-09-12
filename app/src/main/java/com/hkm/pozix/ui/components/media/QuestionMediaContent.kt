package com.hkm.pozix.ui.components.media

import android.content.Context
import android.graphics.Color.parseColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hkm.pozix.data.model.DiagramEdge
import com.hkm.pozix.data.model.DiagramLabel
import com.hkm.pozix.data.model.DiagramNode
import com.hkm.pozix.data.model.QuestionMedia
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders all typed visual blocks supported by a quiz question.
 *
 * This component is deliberately self-contained and cheap to recompose: the
 * Canvas only receives sanitized immutable data and never parses Markdown,
 * evaluates HTML, or creates a WebView. Images are loaded with Coil and all
 * generated visuals are deterministic vector drawings.
 */
@Composable
fun QuestionMediaContent(
    media: List<QuestionMedia>,
    modifier: Modifier = Modifier
) {
    if (media.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        media.forEachIndexed { index, item ->
            key("${item.type}:${item.effectiveUri}:${item.preset}:$index") {
                QuestionMediaCard(item)
            }
        }
    }
}

@Composable
private fun QuestionMediaCard(media: QuestionMedia) {
    val shape = RoundedCornerShape(16.dp)
    val canvasBackground = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (media.type) {
                "image" -> QuestionImage(media, canvasBackground, shape)
                "geometry", "diagram", "mind_map" -> VisualCanvas(
                    media = media,
                    background = canvasBackground,
                    shape = shape
                )
            }

            media.caption?.takeIf { it.isNotBlank() }?.let { caption ->
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun QuestionImage(
    media: QuestionMedia,
    background: Color,
    shape: RoundedCornerShape
) {
    val context = LocalContext.current
    val imageModel = remember(context, media.effectiveUri) {
        resolveImageModel(context, media.effectiveUri)
    }
    var loadFailed by remember(imageModel) { mutableStateOf(false) }
    val ratio = (media.aspectRatio ?: 1.55f).coerceIn(0.75f, 2.4f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = media.altText?.takeIf { it.isNotBlank() },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            placeholder = ColorPainter(background),
            error = ColorPainter(background),
            onSuccess = { loadFailed = false },
            onError = { loadFailed = true }
        )

        if (imageModel == null || loadFailed) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
    }
}

private fun resolveImageModel(context: Context, uri: String?): Any? {
    val value = uri?.trim().orEmpty()
    if (value.isBlank()) return null
    if (!value.startsWith("asset://", ignoreCase = true)) return value

    val assetId = value.substringAfter("asset://")
    if (!assetId.matches(Regex("[A-Za-z0-9._-]{1,128}"))) return null
    val root = File(context.filesDir, "quiz_media").canonicalFile
    val candidate = File(root, assetId).canonicalFile
    return candidate.takeIf { it.parentFile == root && it.isFile }
}

@Composable
private fun VisualCanvas(
    media: QuestionMedia,
    background: Color,
    shape: RoundedCornerShape
) {
    val textMeasurer = rememberTextMeasurer()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val outline = MaterialTheme.colorScheme.outline
    val ratio = when (media.type) {
        "mind_map" -> 1.7f
        "diagram" -> 1.55f
        else -> 1.45f
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(shape)
            .background(background)
    ) {
        when (media.type) {
            "geometry" -> drawGeometry(
                media = media,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                textMeasurer = textMeasurer,
                onSurface = onSurface,
                outline = outline
            )

            "diagram", "mind_map" -> drawDiagram(
                media = media,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                textMeasurer = textMeasurer,
                onSurface = onSurface,
                onPrimary = onPrimary,
                outline = outline
            )
        }
    }
}

private data class Point(val x: Float, val y: Float)

private fun DrawScope.drawGeometry(
    media: QuestionMedia,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    onSurface: Color,
    outline: Color
) {
    val preset = media.preset.orEmpty()
    when (preset) {
        "cube", "cuboid", "prism" -> drawCuboid(media, primary, secondary, tertiary, textMeasurer, onSurface, outline)
        "pyramid" -> drawPyramid(media, primary, secondary, textMeasurer, onSurface, outline)
        "cylinder" -> drawCylinder(primary, secondary, outline)
        "cone" -> drawCone(primary, secondary, outline)
        "sphere" -> drawSphere(primary, secondary, outline)
        "coordinate_axes" -> drawCoordinateAxes(primary, secondary, tertiary, outline)
        "triangle" -> drawTriangle(media, primary, secondary, textMeasurer, onSurface, outline)
        "angle" -> drawAngle(primary, secondary, textMeasurer, onSurface, outline)
    }
}

private fun DrawScope.drawCuboid(
    media: QuestionMedia,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    onSurface: Color,
    outline: Color
) {
    val points = linkedMapOf(
        "A" to Point(0.18f, 0.72f),
        "B" to Point(0.64f, 0.72f),
        "C" to Point(0.82f, 0.55f),
        "D" to Point(0.36f, 0.55f),
        "A1" to Point(0.18f, 0.37f),
        "B1" to Point(0.64f, 0.37f),
        "C1" to Point(0.82f, 0.20f),
        "D1" to Point(0.36f, 0.20f)
    )
    val p = points.mapValues { (_, point) -> normalized(point) }

    drawFace(p.valuesOf("A1", "B1", "C1", "D1"), secondary.copy(alpha = 0.18f), outline)
    drawFace(p.valuesOf("A", "B", "C", "D"), primary.copy(alpha = 0.10f), outline)
    drawFace(p.valuesOf("A", "D", "D1", "A1"), tertiary.copy(alpha = 0.12f), outline)

    val hidden = setOf("A1B1", "B1C1", "C1D1", "D1A1")
    val edges = listOf(
        "AB" to "BC", "BC" to "CD", "CD" to "DA", "DA" to "AB",
        "A1B1" to "B1C1", "B1C1" to "C1D1", "C1D1" to "D1A1", "D1A1" to "A1B1",
        "AA1" to "BB1", "BB1" to "CC1", "CC1" to "DD1", "DD1" to "AA1"
    )
    edges.forEach { (first, second) ->
        val from = first.takeWhile { it.isLetterOrDigit() }
        val to = first.dropWhile { it.isLetterOrDigit() }
        val key = first
        val pair = edgePointPair(key, p)
        if (pair != null) {
            val isBack = key in hidden
            if (!isBack || media.showHiddenEdges) {
                drawVisualEdge(
                    from = pair.first,
                    to = pair.second,
                    key = key,
                    highlighted = media.highlightEdges.any { it.equals(key, ignoreCase = true) },
                    color = if (isBack) outline.copy(alpha = 0.65f) else outline,
                    hidden = isBack
                )
            }
        }
    }

    val defaultLabels = mapOf(
        "A" to p.getValue("A"), "B" to p.getValue("B"), "C" to p.getValue("C"), "D" to p.getValue("D"),
        "A1" to p.getValue("A1"), "B1" to p.getValue("B1"), "C1" to p.getValue("C1"), "D1" to p.getValue("D1")
    )
    defaultLabels.forEach { (label, point) -> drawCanvasLabel(label, point, textMeasurer, onSurface) }
    media.labels.forEach { label ->
        drawCanvasLabel(label.text, normalized(Point(label.x, label.y)), textMeasurer, onSurface)
    }
}

private fun DrawScope.drawFace(points: List<Offset>, color: Color, outline: Color) {
    if (points.size < 3) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, color)
    drawPath(path, outline.copy(alpha = 0.72f), style = Stroke(width = 2.2f))
}

private fun DrawScope.drawPyramid(
    media: QuestionMedia,
    primary: Color,
    secondary: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    onSurface: Color,
    outline: Color
) {
    val base = listOf(
        normalized(Point(0.20f, 0.72f)),
        normalized(Point(0.70f, 0.72f)),
        normalized(Point(0.84f, 0.55f)),
        normalized(Point(0.34f, 0.55f))
    )
    val apex = normalized(Point(0.52f, 0.16f))
    drawFace(base, secondary.copy(alpha = 0.16f), outline)
    base.forEachIndexed { index, point ->
        val next = base[(index + 1) % base.size]
        drawLine(primary.copy(alpha = 0.82f), point, apex, strokeWidth = 2.4f)
        drawLine(outline, point, next, strokeWidth = 2.4f)
    }
    listOf("A" to base[0], "B" to base[1], "C" to base[2], "D" to base[3], "S" to apex)
        .forEach { (label, point) -> drawCanvasLabel(label, point, textMeasurer, onSurface) }
    media.labels.forEach { label ->
        drawCanvasLabel(label.text, normalized(Point(label.x, label.y)), textMeasurer, onSurface)
    }
}

private fun DrawScope.drawCylinder(primary: Color, secondary: Color, outline: Color) {
    val left = size.width * 0.26f
    val right = size.width * 0.74f
    val top = size.height * 0.22f
    val bottom = size.height * 0.78f
    val ovalHeight = size.height * 0.16f
    drawRect(secondary.copy(alpha = 0.14f), topLeft = Offset(left, top + ovalHeight / 2), size = Size(right - left, bottom - top - ovalHeight))
    drawOval(
        color = secondary.copy(alpha = 0.22f),
        topLeft = Offset(left, top),
        size = Size(right - left, ovalHeight)
    )
    drawOval(
        color = Color.Transparent,
        topLeft = Offset(left, bottom - ovalHeight),
        size = Size(right - left, ovalHeight),
        style = Stroke(width = 2.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
    )
    drawLine(outline, Offset(left, top + ovalHeight / 2), Offset(left, bottom - ovalHeight / 2), strokeWidth = 2.2f)
    drawLine(outline, Offset(right, top + ovalHeight / 2), Offset(right, bottom - ovalHeight / 2), strokeWidth = 2.2f)
    drawOval(
        color = primary.copy(alpha = 0.7f),
        topLeft = Offset(left, top),
        size = Size(right - left, ovalHeight),
        style = Stroke(width = 2.4f)
    )
}

private fun DrawScope.drawCone(primary: Color, secondary: Color, outline: Color) {
    val left = size.width * 0.25f
    val right = size.width * 0.75f
    val baseY = size.height * 0.76f
    val baseHeight = size.height * 0.16f
    val apex = Offset(size.width * 0.5f, size.height * 0.18f)
    drawPath(Path().apply {
        moveTo(apex.x, apex.y)
        lineTo(left, baseY)
        lineTo(right, baseY)
        close()
    }, secondary.copy(alpha = 0.18f))
    drawLine(primary, apex, Offset(left, baseY + baseHeight / 2), strokeWidth = 2.4f)
    drawLine(primary, apex, Offset(right, baseY + baseHeight / 2), strokeWidth = 2.4f)
    val baseTop = baseY - baseHeight / 2
    drawOval(
        color = outline,
        topLeft = Offset(left, baseTop),
        size = Size(right - left, baseHeight),
        style = Stroke(width = 2.3f)
    )
    drawArc(
        color = outline,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(left, baseTop),
        size = Size(right - left, baseHeight),
        style = Stroke(width = 2.0f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
    )
}

private fun DrawScope.drawSphere(primary: Color, secondary: Color, outline: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) * 0.28f
    drawCircle(secondary.copy(alpha = 0.16f), radius, center)
    drawCircle(primary.copy(alpha = 0.75f), radius, center, style = Stroke(width = 2.4f))
    drawOval(
        color = outline.copy(alpha = 0.75f),
        topLeft = Offset(center.x - radius, center.y - radius * 0.35f),
        size = Size(radius * 2f, radius * 0.7f),
        style = Stroke(width = 2f)
    )
    drawOval(
        color = outline.copy(alpha = 0.75f),
        topLeft = Offset(center.x - radius * 0.35f, center.y - radius),
        size = Size(radius * 0.7f, radius * 2f),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawCoordinateAxes(primary: Color, secondary: Color, tertiary: Color, outline: Color) {
    val origin = Offset(size.width * 0.24f, size.height * 0.72f)
    val xEnd = Offset(size.width * 0.84f, size.height * 0.72f)
    val yEnd = Offset(size.width * 0.24f, size.height * 0.18f)
    val zEnd = Offset(size.width * 0.55f, size.height * 0.50f)
    drawArrow(outline, origin, xEnd)
    drawArrow(primary, origin, yEnd)
    drawArrow(secondary, origin, zEnd)
}

private fun DrawScope.drawTriangle(
    media: QuestionMedia,
    primary: Color,
    secondary: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    onSurface: Color,
    outline: Color
) {
    val points = listOf(
        normalized(Point(0.18f, 0.76f)),
        normalized(Point(0.82f, 0.76f)),
        normalized(Point(0.52f, 0.18f))
    )
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, secondary.copy(alpha = 0.12f))
    drawPath(path, primary, style = Stroke(width = 2.6f, join = StrokeJoin.Round))
    listOf("A" to points[0], "B" to points[1], "C" to points[2])
        .forEach { (label, point) -> drawCanvasLabel(label, point, textMeasurer, onSurface) }
    media.labels.forEach { label -> drawCanvasLabel(label.text, normalized(Point(label.x, label.y)), textMeasurer, onSurface) }
}

private fun DrawScope.drawAngle(
    primary: Color,
    secondary: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    onSurface: Color,
    outline: Color
) {
    val vertex = Offset(size.width * 0.30f, size.height * 0.70f)
    val armOne = Offset(size.width * 0.82f, size.height * 0.70f)
    val armTwo = Offset(size.width * 0.58f, size.height * 0.22f)
    drawLine(primary, vertex, armOne, strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(secondary, vertex, armTwo, strokeWidth = 3f, cap = StrokeCap.Round)
    drawArc(
        color = outline,
        startAngle = -42f,
        sweepAngle = 42f,
        useCenter = false,
        topLeft = Offset(vertex.x - 80f, vertex.y - 80f),
        size = Size(160f, 160f),
        style = Stroke(width = 2.2f)
    )
    drawCanvasLabel("θ", Offset(vertex.x + 70f, vertex.y - 45f), textMeasurer, onSurface)
}

private fun DrawScope.drawDiagram(
    media: QuestionMedia,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    onSurface: Color,
    onPrimary: Color,
    outline: Color
) {
    val nodes = media.nodes
    val byId = nodes.associateBy { it.id }
    media.edges.forEach { edge ->
        val from = byId[edge.from] ?: return@forEach
        val to = byId[edge.to] ?: return@forEach
        drawVisualEdge(
            from = normalized(Point(from.x, from.y)),
            to = normalized(Point(to.x, to.y)),
            key = "${edge.from}:${edge.to}",
            highlighted = false,
            color = outline.copy(alpha = if (edge.hidden) 0.48f else 0.82f),
            hidden = edge.hidden
        )
        edge.label?.takeIf { it.isNotBlank() }?.let { label ->
            val fromOffset = normalized(Point(from.x, from.y))
            val toOffset = normalized(Point(to.x, to.y))
            drawCanvasLabel(
                label,
                Offset((fromOffset.x + toOffset.x) / 2f, (fromOffset.y + toOffset.y) / 2f - 14f),
                textMeasurer,
                onSurface.copy(alpha = 0.82f)
            )
        }
    }

    nodes.forEachIndexed { index, node ->
        val center = normalized(Point(node.x, node.y))
        val nodeColor = node.colorHex.toComposeColor(
            fallback = if (index == 0 && media.type == "mind_map") primary else if (index % 2 == 0) secondary else tertiary
        )
        val textStyle = TextStyle(
            color = if (index == 0 && media.type == "mind_map") onPrimary else onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        val measured = textMeasurer.measure(AnnotatedString(node.label), textStyle)
        val nodeWidth = (measured.size.width + 26f).coerceIn(76f, 170f)
        val nodeHeight = (measured.size.height + 18f).coerceIn(34f, 58f)
        val left = (center.x - nodeWidth / 2f).coerceIn(8f, size.width - nodeWidth - 8f)
        val top = (center.y - nodeHeight / 2f).coerceIn(8f, size.height - nodeHeight - 8f)
        drawRoundRect(
            color = nodeColor.copy(alpha = 0.20f),
            topLeft = Offset(left, top),
            size = Size(nodeWidth, nodeHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = nodeColor,
            topLeft = Offset(left, top),
            size = Size(nodeWidth, nodeHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f),
            style = Stroke(width = 2.2f)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = AnnotatedString(node.label),
            topLeft = Offset(
                left + (nodeWidth - measured.size.width) / 2f,
                top + (nodeHeight - measured.size.height) / 2f
            ),
            style = textStyle
        )
    }
}

private fun DrawScope.drawVisualEdge(
    from: Offset,
    to: Offset,
    key: String,
    highlighted: Boolean,
    color: Color,
    hidden: Boolean
) {
    drawLine(
        color = if (highlighted) Color(0xFFFFB300) else color,
        start = from,
        end = to,
        strokeWidth = if (highlighted) 4f else 2.2f,
        cap = StrokeCap.Round,
        pathEffect = if (hidden) PathEffect.dashPathEffect(floatArrayOf(8f, 7f)) else null
    )
    if (!hidden) drawArrowHead(if (highlighted) Color(0xFFFFB300) else color, from, to)
}

private fun DrawScope.drawArrow(color: Color, from: Offset, to: Offset) {
    drawLine(color, from, to, strokeWidth = 2.6f, cap = StrokeCap.Round)
    drawArrowHead(color, from, to)
}

private fun DrawScope.drawArrowHead(color: Color, from: Offset, to: Offset) {
    val angle = atan2(to.y - from.y, to.x - from.x)
    val length = 12f
    val spread = 0.52f
    val first = Offset(to.x - length * cos(angle - spread), to.y - length * sin(angle - spread))
    val second = Offset(to.x - length * cos(angle + spread), to.y - length * sin(angle + spread))
    drawLine(color, to, first, strokeWidth = 2.2f, cap = StrokeCap.Round)
    drawLine(color, to, second, strokeWidth = 2.2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawCanvasLabel(
    label: String,
    point: Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    color: Color
) {
    val style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val measured = textMeasurer.measure(AnnotatedString(label), style)
    val left = (point.x + 5f).coerceIn(4f, size.width - measured.size.width - 4f)
    val top = (point.y - measured.size.height - 4f).coerceIn(4f, size.height - measured.size.height - 4f)
    drawText(textMeasurer, AnnotatedString(label), Offset(left, top), style)
}

private fun DrawScope.edgePointPair(key: String, points: Map<String, Offset>): Pair<Offset, Offset>? {
    val split = when {
        key.length == 2 -> listOf(key.substring(0, 1), key.substring(1, 2))
        key.length == 4 -> listOf(key.substring(0, 2), key.substring(2, 4))
        else -> return null
    }
    val first = points[split[0]] ?: return null
    val second = points[split[1]] ?: return null
    return first to second
}

private fun Map<String, Offset>.valuesOf(vararg keys: String): List<Offset> = keys.mapNotNull { this[it] }

private fun DrawScope.normalized(point: Point): Offset = Offset(point.x * size.width, point.y * size.height)

private fun String?.toComposeColor(fallback: Color): Color {
    val value = this?.trim()?.takeIf { it.matches(Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")) } ?: return fallback
    return runCatching { Color(parseColor(value)) }.getOrDefault(fallback)
}
