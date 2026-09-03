package io.github.arturomanzoli.rastreado.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusResolverTest {

    @Test
    fun `action code wins over the event text`() {
        // The text alone would read as out for delivery; the code says the parcel only left a hub.
        val status = StatusResolver.resolve("GTMS_SC_DEPART", "Saiu para entrega")
        assertEquals(ParcelStatus.LAST_MILE, status)
    }

    @Test
    fun `action code is matched regardless of case and padding`() {
        assertEquals(ParcelStatus.DELIVERED, StatusResolver.resolve(" gtms_signed ", null))
    }

    @Test
    fun `unmapped code falls back to the event text`() {
        val status = StatusResolver.resolve("SOME_NEW_CODE", "[SAO PAULO] Objeto saiu para entrega ao destinatário")
        assertEquals(ParcelStatus.OUT_FOR_DELIVERY, status)
    }

    @Test
    fun `matches Brazilian event text written with accents`() {
        assertEquals(
            ParcelStatus.IMPORT_CUSTOMS,
            StatusResolver.fromDescription("Objeto aguardando pagamento de tributos"),
        )
        assertEquals(
            ParcelStatus.FAILED_ATTEMPT,
            StatusResolver.fromDescription("Não foi possível entregar o objeto - destinatário ausente"),
        )
    }

    @Test
    fun `a failed attempt is not read as a delivery`() {
        assertEquals(
            ParcelStatus.FAILED_ATTEMPT,
            StatusResolver.fromDescription("Tentativa de entrega não efetuada"),
        )
    }

    @Test
    fun `a return to sender is a problem rather than a delivery`() {
        assertEquals(
            ParcelStatus.EXCEPTION,
            StatusResolver.fromDescription("Objeto devolvido ao remetente"),
        )
    }

    @Test
    fun `unrecognised text resolves to unknown rather than guessing`() {
        assertNull(StatusResolver.fromDescription("Notificação da transportadora"))
        assertEquals(ParcelStatus.UNKNOWN, StatusResolver.resolve(null, null))
    }

    @Test
    fun `every Brazilian state is classified as last mile`() {
        val lastMile = ParcelStatus.entries.filter { it.isLastMile }
        assertEquals(
            listOf(
                ParcelStatus.IMPORT_CUSTOMS,
                ParcelStatus.LAST_MILE,
                ParcelStatus.OUT_FOR_DELIVERY,
                ParcelStatus.AWAITING_PICKUP,
                ParcelStatus.FAILED_ATTEMPT,
            ),
            lastMile,
        )
    }
}
