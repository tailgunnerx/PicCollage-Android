package com.tailgunnerx.piccollage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A single photo cell, expressed as fractions (0..1) of the collage canvas.
 * Because cells are fractional, every layout works with any aspect ratio, and the
 * live preview, the thumbnails and the exported image all share one definition.
 */
data class CellRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** A cell resolved to pixels for a concrete canvas size + border (gap) width. */
data class PxRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}

private const val EDGE_EPS = 0.0005f

/**
 * Converts a fractional cell to pixels. Outer edges get a full [gap] of border,
 * interior edges get half a gap on each side so every gutter is exactly [gap] wide.
 */
fun CellRect.toPx(width: Float, height: Float, gap: Float): PxRect {
    val half = gap / 2f
    val l = if (left <= EDGE_EPS) gap else left * width + half
    val t = if (top <= EDGE_EPS) gap else top * height + half
    val r = if (right >= 1f - EDGE_EPS) width - gap else right * width - half
    val b = if (bottom >= 1f - EDGE_EPS) height - gap else bottom * height - half
    return PxRect(l, t, maxOf(r, l + 1f), maxOf(b, t + 1f))
}

enum class CollageType(val displayName: String, val description: String) {
    // 2 photos
    SPLIT_VERTICAL("Side by Side", "Two columns, left & right"),
    SPLIT_HORIZONTAL("Top & Bottom", "Two rows, top & bottom"),
    // 3 photos
    BIG_LEFT("Big Left", "One large left + two stacked right"),
    BIG_RIGHT("Big Right", "Two stacked left + one large right"),
    // 4 photos
    GRID_2X2("Grid 2×2", "Classic four-square grid"),
    BIG_TOP("Big Top", "One wide top + three below"),
    BIG_BOTTOM("Big Bottom", "Three on top + one wide bottom"),
    // 5 photos
    GRID_ASYMMETRIC("Asymmetric 5", "Two large + three small"),
    // 6 photos
    GRID_2X3("Grid 2×3", "Six equal cells, 2 columns × 3 rows"),
    // 9 photos
    GRID_3X3("Grid 3×3", "Nine equal cells, 3 columns × 3 rows"),
    // N photos
    CUSTOM("Custom", "Choose rows & columns, drag the lines")
}

fun evenStops(n: Int): List<Float> = List((n - 1).coerceAtLeast(0)) { (it + 1f) / n }

/** Builds a rows × cols grid. Stops are the interior line positions (fractions). */
fun gridCells(
    cols: Int,
    rows: Int,
    colStops: List<Float> = evenStops(cols),
    rowStops: List<Float> = evenStops(rows)
): List<CellRect> {
    val xs = listOf(0f) + colStops + listOf(1f)
    val ys = listOf(0f) + rowStops + listOf(1f)
    return buildList {
        for (r in 0 until rows) for (c in 0 until cols) {
            add(CellRect(xs[c], ys[r], xs[c + 1], ys[r + 1]))
        }
    }
}

private const val THIRD = 1f / 3f
private const val TWO_THIRDS = 2f / 3f

private fun threeAcross(top: Float, bottom: Float) = listOf(
    CellRect(0f, top, THIRD, bottom),
    CellRect(THIRD, top, TWO_THIRDS, bottom),
    CellRect(TWO_THIRDS, top, 1f, bottom)
)

fun cellsFor(type: CollageType, custom: CustomGridState): List<CellRect> = when (type) {
    CollageType.SPLIT_VERTICAL -> gridCells(2, 1)
    CollageType.SPLIT_HORIZONTAL -> gridCells(1, 2)
    CollageType.BIG_LEFT -> listOf(
        CellRect(0f, 0f, 0.6f, 1f),
        CellRect(0.6f, 0f, 1f, 0.5f),
        CellRect(0.6f, 0.5f, 1f, 1f)
    )
    CollageType.BIG_RIGHT -> listOf(
        CellRect(0f, 0f, 0.4f, 0.5f),
        CellRect(0f, 0.5f, 0.4f, 1f),
        CellRect(0.4f, 0f, 1f, 1f)
    )
    CollageType.GRID_2X2 -> gridCells(2, 2)
    CollageType.BIG_TOP -> listOf(CellRect(0f, 0f, 1f, 0.6f)) + threeAcross(0.6f, 1f)
    CollageType.BIG_BOTTOM -> threeAcross(0f, 0.4f) + listOf(CellRect(0f, 0.4f, 1f, 1f))
    CollageType.GRID_ASYMMETRIC -> listOf(
        CellRect(0f, 0f, 0.5f, 0.6f),
        CellRect(0.5f, 0f, 1f, 0.6f)
    ) + threeAcross(0.6f, 1f)
    CollageType.GRID_2X3 -> gridCells(2, 3)
    CollageType.GRID_3X3 -> gridCells(3, 3)
    CollageType.CUSTOM -> gridCells(custom.cols, custom.rows, custom.colStops, custom.rowStops)
}

/**
 * State for the user-defined grid: number of rows/columns plus the position of
 * every interior line (draggable in the preview).
 */
class CustomGridState(initialCols: Int = 3, initialRows: Int = 3) {
    var cols by mutableStateOf(initialCols.coerceIn(1, MAX_LINES))
        private set
    var rows by mutableStateOf(initialRows.coerceIn(1, MAX_LINES))
        private set
    val colStops = mutableStateListOf<Float>().apply { addAll(evenStops(cols)) }
    val rowStops = mutableStateListOf<Float>().apply { addAll(evenStops(rows)) }

    fun changeCols(n: Int) {
        cols = n.coerceIn(1, MAX_LINES)
        colStops.clear(); colStops.addAll(evenStops(cols))
    }

    fun changeRows(n: Int) {
        rows = n.coerceIn(1, MAX_LINES)
        rowStops.clear(); rowStops.addAll(evenStops(rows))
    }

    fun resetLines() {
        colStops.clear(); colStops.addAll(evenStops(cols))
        rowStops.clear(); rowStops.addAll(evenStops(rows))
    }

    fun moveColStop(i: Int, delta: Float) = moveStop(colStops, i, delta)
    fun moveRowStop(i: Int, delta: Float) = moveStop(rowStops, i, delta)

    /** Restores saved line positions if they are valid for the current counts. */
    fun restoreStops(savedCols: List<Float>, savedRows: List<Float>) {
        if (savedCols.size == cols - 1 && isValid(savedCols)) { colStops.clear(); colStops.addAll(savedCols) }
        if (savedRows.size == rows - 1 && isValid(savedRows)) { rowStops.clear(); rowStops.addAll(savedRows) }
    }

    private fun isValid(stops: List<Float>): Boolean =
        stops.zipWithNext().all { (a, b) -> b > a } && stops.all { it > 0f && it < 1f }

    private fun moveStop(stops: MutableList<Float>, i: Int, delta: Float) {
        if (i !in stops.indices) return
        val min = (if (i == 0) 0f else stops[i - 1]) + MIN_CELL
        val max = (if (i == stops.lastIndex) 1f else stops[i + 1]) - MIN_CELL
        if (max <= min) return
        stops[i] = (stops[i] + delta).coerceIn(min, max)
    }

    companion object {
        const val MAX_LINES = 5
        /** Smallest allowed cell size as a fraction of the canvas. */
        const val MIN_CELL = 0.08f
    }
}
