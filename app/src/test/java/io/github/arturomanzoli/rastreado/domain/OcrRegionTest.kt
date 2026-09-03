package io.github.arturomanzoli.rastreado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The region picker on the add screen depends on two things being true: a region resolves to the
 * lines it visually covers, and a dragged region never escapes the photo.
 */
class OcrRegionTest {

    /** An order screen as ML Kit lays it out: one line per row, top to bottom. */
    private val lines = listOf(
        row("Pedidos", top = 0.05f),
        row("Fone de Ouvido Bluetooth 5.3 Sem Fio TWS", top = 0.20f, right = 0.95f),
        row("com Cancelamento de Ruído", top = 0.26f, right = 0.70f),
        row("R$ 89,90", top = 0.34f, right = 0.35f),
        row("LP00837062153412", top = 0.50f, right = 0.60f),
        row("Objeto em trânsito", top = 0.60f, right = 0.65f),
    )

    private fun row(text: String, top: Float, left: Float = 0.05f, right: Float = 0.50f) =
        OcrLine(text, PhotoRegion(left = left, top = top, right = right, bottom = top + 0.04f))

    @Test
    fun `a region resolves to the code it covers`() {
        val region = PhotoRegion(0.02f, 0.47f, 0.65f, 0.56f)
        val text = OcrFieldExtractor.textIn(lines, region)
        assertEquals("LP00837062153412", text)
        assertEquals("LP00837062153412", OcrFieldExtractor.codeIn(text))
    }

    @Test
    fun `a region spanning two rows joins a wrapped product title`() {
        val region = PhotoRegion(0.02f, 0.18f, 0.98f, 0.31f)
        assertEquals(
            "Fone de Ouvido Bluetooth 5.3 Sem Fio TWS com Cancelamento de Ruído",
            OcrFieldExtractor.textIn(lines, region),
        )
    }

    @Test
    fun `rows outside the region are left out`() {
        val region = PhotoRegion(0.02f, 0.18f, 0.98f, 0.31f)
        val text = OcrFieldExtractor.textIn(lines, region)
        assertTrue("price leaked in: $text", "89,90" !in text)
        assertTrue("header leaked in: $text", "Pedidos" !in text)
    }

    @Test
    fun `a region narrower than a row still takes that whole row`() {
        // Text is chosen by row, so clipping a line horizontally does not cut it in half.
        val region = PhotoRegion(0.02f, 0.47f, 0.20f, 0.56f)
        assertEquals("LP00837062153412", OcrFieldExtractor.textIn(lines, region))
    }

    @Test
    fun `an empty region yields no code`() {
        val region = PhotoRegion(0.02f, 0.90f, 0.98f, 0.98f)
        assertEquals("", OcrFieldExtractor.textIn(lines, region))
        assertNull(OcrFieldExtractor.codeIn(""))
    }

    @Test
    fun `suggested regions land on the code and the title`() {
        val suggestion = OcrFieldExtractor.suggestRegions(lines)
        assertEquals(
            "LP00837062153412",
            OcrFieldExtractor.codeIn(OcrFieldExtractor.textIn(lines, suggestion.code)),
        )
        assertTrue(
            OcrFieldExtractor.textIn(lines, suggestion.name).startsWith("Fone de Ouvido"),
        )
    }

    @Test
    fun `suggested regions stay inside the photo when nothing is recognised`() {
        val suggestion = OcrFieldExtractor.suggestRegions(emptyList())
        listOf(suggestion.code, suggestion.name).forEach { region ->
            assertTrue("$region escapes the photo", region.left >= 0f && region.top >= 0f)
            assertTrue("$region escapes the photo", region.right <= 1f && region.bottom <= 1f)
        }
    }

    @Test
    fun `dragging past an edge stops at the edge without shrinking the region`() {
        val region = PhotoRegion(0.10f, 0.10f, 0.40f, 0.20f)
        val dragged = region.translated(dx = -0.50f, dy = 5f)
        assertEquals(0f, dragged.left, 0.0001f)
        assertEquals(1f, dragged.bottom, 0.0001f)
        assertEquals(region.width, dragged.width, 0.0001f)
        assertEquals(region.height, dragged.height, 0.0001f)
    }

    @Test
    fun `resizing cannot collapse a region below a grabbable size`() {
        val squashed = PhotoRegion(0.10f, 0.10f, 0.40f, 0.20f).resized(-1f, -1f)
        assertEquals(PhotoRegion.MIN_SIDE, squashed.width, 0.0001f)
        assertEquals(PhotoRegion.MIN_SIDE, squashed.height, 0.0001f)
    }
}
