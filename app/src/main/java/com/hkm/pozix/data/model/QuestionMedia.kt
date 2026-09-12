package com.hkm.pozix.data.model

import kotlinx.serialization.Serializable

/**
 * Optional visual content attached to a question.
 *
 * The player intentionally renders a small, typed contract instead of executing
 * HTML/JavaScript supplied by an imported quiz. That keeps imported quizzes safe,
 * deterministic and usable offline for generated diagrams.
 */
@Serializable
data class QuestionMedia(
    val type: String,
    val uri: String? = null,
    val source: String? = null,
    val url: String? = null,
    val altText: String? = null,
    val caption: String? = null,
    val aspectRatio: Float? = null,
    val preset: String? = null,
    val nodes: List<DiagramNode> = emptyList(),
    val edges: List<DiagramEdge> = emptyList(),
    val labels: List<DiagramLabel> = emptyList(),
    val showHiddenEdges: Boolean = false,
    val highlightEdges: List<String> = emptyList()
) {
    /** Canonical source after accepting the common AI aliases. */
    val effectiveUri: String?
        get() = uri?.takeIf { it.isNotBlank() }
            ?: source?.takeIf { it.isNotBlank() }
            ?: url?.takeIf { it.isNotBlank() }
}

@Serializable
data class DiagramNode(
    val id: String,
    val label: String,
    /** Normalized horizontal position in the [0, 1] range. */
    val x: Float,
    /** Normalized vertical position in the [0, 1] range. */
    val y: Float,
    val colorHex: String? = null
)

@Serializable
data class DiagramEdge(
    val from: String,
    val to: String,
    val label: String? = null,
    val hidden: Boolean = false
)

@Serializable
data class DiagramLabel(
    val text: String,
    val x: Float,
    val y: Float
)
