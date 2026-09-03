package io.github.arturomanzoli.rastreado.data.remote.cainiao

import io.github.arturomanzoli.rastreado.domain.ParcelStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses a payload captured verbatim from Cainiao's public endpoint for a real AliExpress parcel
 * on its Brazilian leg, so a change in the mapper is measured against what the API actually sends.
 */
class CainiaoMapperTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun fixture(name: String): CainiaoResponse {
        val text = checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader()
            .use { it.readText() }
        return json.decodeFromString(text)
    }

    private val snapshot by lazy {
        checkNotNull(CainiaoMapper.toSnapshot(fixture(LAST_MILE_FIXTURE), REQUESTED_CODE))
    }

    @Test
    fun `unknown keys in the live payload do not break parsing`() {
        val response = fixture(LAST_MILE_FIXTURE)
        assertTrue(response.success)
        assertEquals(21, response.module.single().detailList.size)
    }

    @Test
    fun `the newest event sets the parcel status`() {
        assertEquals(ParcelStatus.LAST_MILE, snapshot.status)
        assertTrue(snapshot.status.isLastMile)
    }

    @Test
    fun `the Brazilian tracking code is surfaced as the local code`() {
        assertEquals("NN368369905BR", snapshot.lastMileCode)
        assertEquals("Correios(BR)", snapshot.carrier)
    }

    @Test
    fun `the city prefix becomes the event location rather than part of its text`() {
        val newest = snapshot.events.first()
        assertEquals("SAO JOSE", newest.location)
        assertEquals("Objeto em trânsito - por favor aguarde", newest.description)
    }

    @Test
    fun `events are ordered newest first`() {
        val timestamps = snapshot.events.map { it.timestamp }
        assertEquals(timestamps.sortedDescending(), timestamps)
    }

    @Test
    fun `every event in the live payload is classified`() {
        val unclassified = snapshot.events.filter { it.status == ParcelStatus.UNKNOWN }
        assertEquals(emptyList<String>(), unclassified.map { it.actionCode })
    }

    @Test
    fun `the customs and warehouse legs keep their own statuses`() {
        val byCode = snapshot.events.associateBy { it.actionCode }
        assertEquals(ParcelStatus.IMPORT_CUSTOMS, byCode.getValue("CC_IM_SUCCESS").status)
        assertEquals(ParcelStatus.EXPORT_CUSTOMS, byCode.getValue("CC_EX_START").status)
        assertEquals(ParcelStatus.PENDING, byCode.getValue("GWMS_ACCEPT").status)
    }

    @Test
    fun `route and progress come through as sent`() {
        assertEquals("China", snapshot.originCountry)
        assertEquals("Brasil", snapshot.destinationCountry)
        assertNotNull(snapshot.progress)
        assertEquals(0.45f, snapshot.progress!!, 0.001f)
    }

    @Test
    fun `an empty detail list is reported as no tracking yet`() {
        val response = fixture(UNKNOWN_FIXTURE)
        assertNull(CainiaoMapper.toSnapshot(response, "ZH363796677HK"))
    }

    private companion object {
        const val REQUESTED_CODE = "LP00837062153412"
        const val LAST_MILE_FIXTURE = "cainiao_lp_last_mile.json"
        const val UNKNOWN_FIXTURE = "cainiao_unknown_code.json"
    }
}
