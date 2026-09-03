package io.github.arturomanzoli.rastreado.domain

/**
 * A rectangle in the 0..1 space of the photo it was measured on.
 *
 * Normalised rather than in pixels so the same value describes a region on the full-resolution
 * capture and on the scaled-down copy shown on screen.
 */
data class PhotoRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2f

    /**
     * True when [other] sits on a row this region covers.
     *
     * Vertical containment is tested on the centre and horizontal only on overlap, because text
     * is laid out in rows: someone dragging a box over a wrapped product title is choosing which
     * lines to take, not trimming them by the character.
     */
    fun coversRow(other: PhotoRegion): Boolean =
        other.centerY in top..bottom && other.right >= left && other.left <= right

    fun translated(dx: Float, dy: Float): PhotoRegion =
        copy(left = left + dx, top = top + dy, right = right + dx, bottom = bottom + dy).clamped()

    fun resized(dRight: Float, dBottom: Float): PhotoRegion =
        copy(right = right + dRight, bottom = bottom + dBottom).clamped()

    fun grownBy(dx: Float, dy: Float): PhotoRegion =
        PhotoRegion(left - dx, top - dy, right + dx, bottom + dy).clamped()

    /** Keeps the region inside the photo and above a size the user can still grab. */
    fun clamped(): PhotoRegion {
        val w = width.coerceIn(MIN_SIDE, 1f)
        val h = height.coerceIn(MIN_SIDE, 1f)
        val l = left.coerceIn(0f, 1f - w)
        val t = top.coerceIn(0f, 1f - h)
        return PhotoRegion(l, t, l + w, t + h)
    }

    companion object {
        const val MIN_SIDE = 0.04f

        fun of(left: Float, top: Float, width: Float, height: Float): PhotoRegion =
            PhotoRegion(left, top, left + width, top + height).clamped()
    }
}

/** One line of recognised text and where it sits in the photo. */
data class OcrLine(val text: String, val bounds: PhotoRegion)
