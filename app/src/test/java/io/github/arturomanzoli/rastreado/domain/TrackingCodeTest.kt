package io.github.arturomanzoli.rastreado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingCodeTest {

    @Test
    fun `normalize strips the separators a label carries`() {
        assertEquals("LP00837062153412", TrackingCode.normalize("lp0083 7062-153412"))
        assertEquals("NN368369905BR", TrackingCode.normalize(" nn 368 369 905 br "))
    }

    @Test
    fun `accepts the two shapes an AliExpress parcel to Brazil actually carries`() {
        assertTrue(TrackingCode.isPlausible("LP00837062153412"))
        assertTrue(TrackingCode.isPlausible("NN368369905BR"))
    }

    @Test
    fun `rejects prose and codes of implausible length`() {
        assertFalse(TrackingCode.isPlausible("Objeto em transito"))
        assertFalse(TrackingCode.isPlausible("LP123"))
        assertFalse(TrackingCode.isPlausible(""))
    }

    @Test
    fun `finds the S10 code before the looser shapes`() {
        val found = TrackingCode.findAll("Rastreio LP00837062153412 e NN368369905BR")
        assertEquals(listOf("NN368369905BR", "LP00837062153412"), found)
    }

    @Test
    fun `does not join codes split across lines`() {
        assertNull(TrackingCode.findFirst("LP008370\n62153412"))
    }

    @Test
    fun `pulls the last-mile code out of the localised realMailNo label`() {
        val code = TrackingCode.extractLastMileCode("Último número de rastreamento:\tNN368369905BR")
        assertEquals("NN368369905BR", code)
    }

    @Test
    fun `has no last-mile code when Cainiao sends the field blank`() {
        assertNull(TrackingCode.extractLastMileCode(null))
        assertNull(TrackingCode.extractLastMileCode("  "))
    }
}
