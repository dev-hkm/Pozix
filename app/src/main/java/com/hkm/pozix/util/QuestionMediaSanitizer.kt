package com.hkm.pozix.util

import com.hkm.pozix.data.model.DiagramEdge
import com.hkm.pozix.data.model.DiagramLabel
import com.hkm.pozix.data.model.DiagramNode
import com.hkm.pozix.data.model.QuestionMedia
import java.util.Locale

/**
 * Normalizes the visual contract at the import boundary.
 *
 * Invalid visuals are non-fatal: the text question must remain playable. The
 * limits also stop a malformed/AI-generated payload from creating an enormous
 * composition tree or an unbounded canvas workload.
 */
object QuestionMediaSanitizer {
    const val MAX_MEDIA_PER_QUESTION = 4
    const val MAX_NODES = 32
    const val MAX_EDGES = 48
    const val MAX_LABELS = 32

    private val supportedTypes = setOf("image", "geometry", "diagram", "mind_map")
    private val supportedGeometryPresets = setOf(
        "cube",
        "cuboid",
        "prism",
        "pyramid",
        "cylinder",
        "cone",
        "sphere",
        "coordinate_axes",
        "triangle",
        "angle"
    )

    fun sanitize(media: List<QuestionMedia>): List<QuestionMedia> = media
        .asSequence()
        .mapNotNull(::sanitizeOne)
        .take(MAX_MEDIA_PER_QUESTION)
        .toList()

    private fun sanitizeOne(raw: QuestionMedia): QuestionMedia? {
        val type = normalizeType(raw.type)
        if (type !in supportedTypes) return null

        return when (type) {
            "image" -> {
                val uri = raw.effectiveUri?.trim()?.takeIf(::isAllowedImageUri) ?: return null
                raw.copy(
                    type = type,
                    uri = uri,
                    source = null,
                    url = null,
                    altText = raw.altText.cleanText(160),
                    caption = raw.caption.cleanText(240),
                    aspectRatio = raw.aspectRatio.normalizedAspectRatio()
                )
            }

            "geometry" -> {
                val preset = normalizeGeometryPreset(raw.preset) ?: return null
                raw.copy(
                    type = type,
                    preset = preset,
                    caption = raw.caption.cleanText(240),
                    labels = sanitizeLabels(raw.labels),
                    highlightEdges = raw.highlightEdges
                        .asSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .take(24)
                        .toList()
                )
            }

            "diagram", "mind_map" -> {
                val nodes = sanitizeNodes(raw.nodes)
                if (nodes.isEmpty()) return null
                val nodeIds = nodes.mapTo(hashSetOf()) { it.id }
                val edges = raw.edges
                    .asSequence()
                    .filter { it.from in nodeIds && it.to in nodeIds && it.from != it.to }
                    .map {
                        it.copy(
                            from = it.from.trim(),
                            to = it.to.trim(),
                            label = it.label.cleanText(100)
                        )
                    }
                    .distinctBy { Triple(it.from, it.to, it.label) }
                    .take(MAX_EDGES)
                    .toList()

                raw.copy(
                    type = type,
                    nodes = nodes,
                    edges = edges,
                    labels = sanitizeLabels(raw.labels),
                    caption = raw.caption.cleanText(240)
                )
            }

            else -> null
        }
    }

    private fun sanitizeNodes(nodes: List<DiagramNode>): List<DiagramNode> {
        val seen = hashSetOf<String>()
        return nodes.asSequence()
            .mapNotNull { node ->
                val id = node.id.trim().take(64)
                val label = node.label.cleanText(120)
                if (id.isEmpty() || label.isNullOrEmpty() || !seen.add(id)) return@mapNotNull null

                val x = normalizeCoordinate(node.x) ?: return@mapNotNull null
                val y = normalizeCoordinate(node.y) ?: return@mapNotNull null
                node.copy(id = id, label = label, x = x, y = y, colorHex = normalizeColor(node.colorHex))
            }
            .take(MAX_NODES)
            .toList()
    }

    private fun sanitizeLabels(labels: List<DiagramLabel>): List<DiagramLabel> = labels
        .asSequence()
        .mapNotNull { label ->
            val text = label.text.cleanText(100) ?: return@mapNotNull null
            val x = normalizeCoordinate(label.x) ?: return@mapNotNull null
            val y = normalizeCoordinate(label.y) ?: return@mapNotNull null
            label.copy(text = text, x = x, y = y)
        }
        .take(MAX_LABELS)
        .toList()

    private fun normalizeCoordinate(value: Float): Float? {
        if (!value.isFinite() || value < -1f || value > 1f) return null
        return if (value < 0f) (value + 1f) / 2f else value.coerceIn(0f, 1f)
    }

    private fun normalizeGeometryPreset(value: String?): String? {
        val normalized = value.orEmpty().trim().lowercase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_')
        return when (normalized) {
            "box" -> "cuboid"
            "axes", "coordinate_axis", "coordinate_axes_3d" -> "coordinate_axes"
            else -> normalized.takeIf { it in supportedGeometryPresets }
        }
    }

    private fun normalizeType(value: String): String = value.trim().lowercase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_')

    private fun isAllowedImageUri(value: String): Boolean = when {
        value.startsWith("https://", ignoreCase = true) -> value.length <= 2048
        value.startsWith("http://", ignoreCase = true) -> value.length <= 2048
        value.startsWith("asset://", ignoreCase = true) -> {
            value.removePrefix("asset://").matches(Regex("[A-Za-z0-9._-]{1,128}"))
        }
        else -> false
    }

    private fun String?.cleanText(maxLength: Int): String? = this
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.take(maxLength)

    private fun Float?.normalizedAspectRatio(): Float? = this
        ?.takeIf { it.isFinite() && it in 0.5f..3f }

    private fun normalizeColor(value: String?): String? {
        val color = value?.trim()?.uppercase(Locale.ROOT) ?: return null
        return color.takeIf { it.matches(Regex("#[0-9A-F]{6}([0-9A-F]{2})?")) }
    }
}
