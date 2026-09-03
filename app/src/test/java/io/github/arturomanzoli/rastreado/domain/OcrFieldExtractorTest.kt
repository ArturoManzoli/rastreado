package io.github.arturomanzoli.rastreado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrFieldExtractorTest {

    /** What ML Kit typically returns for a photo of an AliExpress order screen, in pt-BR. */
    private val orderScreen = """
        Pedidos
        Fone de Ouvido Bluetooth 5.3 Sem Fio TWS com Cancelamento de Ruído
        R$ 89,90
        Rastreamento
        LP00837062153412
        Objeto em trânsito - por favor aguarde
        Loja
        Rastrear
    """.trimIndent()

    @Test
    fun `reads the code and the product title off an order screen`() {
        val result = OcrFieldExtractor.extract(orderScreen)
        assertEquals("LP00837062153412", result.code)
        assertEquals(
            "Fone de Ouvido Bluetooth 5.3 Sem Fio TWS com Cancelamento de Ruído",
            result.productName,
        )
    }

    @Test
    fun `neither the price nor interface chrome is taken for a product name`() {
        val result = OcrFieldExtractor.extract(orderScreen)
        assertTrue(result.productName?.contains("R$") != true)
        assertEquals(false, result.productName.equals("Rastreamento", ignoreCase = true))
    }

    @Test
    fun `repairs glyphs the recogniser confuses inside a code's digits`() {
        // O read for 0 and S read for 5 in "LP00837062153412".
        val result = OcrFieldExtractor.extract("Rastreio LP0O8370621S3412")
        assertEquals("LP00837062153412", result.code)
    }

    @Test
    fun `a correctly read code is never rewritten by the repair pass`() {
        val result = OcrFieldExtractor.extract("NN368369905BR")
        assertEquals("NN368369905BR", result.code)
    }

    @Test
    fun `the local code wins over the forwarder code`() {
        val result = OcrFieldExtractor.extract("LP00837062153412\nNN368369905BR")
        assertEquals("NN368369905BR", result.code)
    }

    @Test
    fun `a photo with no code yields nothing rather than a guess`() {
        val result = OcrFieldExtractor.extract("Pedidos\nLoja\nAjuda")
        assertNull(result.code)
        assertNull(result.productName)
    }
}
