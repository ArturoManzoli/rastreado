package io.github.arturomanzoli.rastreado.domain

/**
 * Turns a provider's raw event into a [ParcelStatus].
 *
 * Cainiao's `actionCode` is a stable machine code and is trusted first. The event text is only
 * consulted when the code is missing or unrecognised, which happens whenever a Brazilian carrier
 * injects a message Cainiao passes through without classifying it.
 */
object StatusResolver {

    private val ACTION_CODES: Map<String, ParcelStatus> = mapOf(
        // Seller and forwarder warehouse, before the parcel leaves China.
        "GWMS_ACCEPT" to ParcelStatus.PENDING,
        "GWMS_PACKAGE" to ParcelStatus.PENDING,
        "PRE_READY_TO_SHIP" to ParcelStatus.PENDING,
        "WAIT_ACCEPT" to ParcelStatus.PENDING,
        "GWMS_OUTBOUND" to ParcelStatus.WAREHOUSE,
        "PU_PICKUP_SUCCESS" to ParcelStatus.WAREHOUSE,
        "CW_SIGN_IN_SUCCESS" to ParcelStatus.WAREHOUSE,
        "CW_INBOUND" to ParcelStatus.WAREHOUSE,
        "CW_OUTBOUND" to ParcelStatus.WAREHOUSE,
        "CW_COMMON_PROCESSING1" to ParcelStatus.WAREHOUSE,

        // Origin sorting centre.
        "SC_INBOUND_SUCCESS" to ParcelStatus.SORTING,
        "SC_OUTBOUND_SUCCESS" to ParcelStatus.SORTING,

        // Chinese export clearance.
        "CC_EX_START" to ParcelStatus.EXPORT_CUSTOMS,
        "CC_EX_SUCCESS" to ParcelStatus.EXPORT_CUSTOMS,

        // International line haul.
        "LH_HO_IN_SUCCESS" to ParcelStatus.IN_TRANSIT,
        "LH_HO_AIRLINE" to ParcelStatus.IN_TRANSIT,
        "LH_DEPART" to ParcelStatus.IN_TRANSIT,
        "TD_TRANS_ARRIVE_C" to ParcelStatus.IN_TRANSIT,
        "LH_ARRIVE" to ParcelStatus.ARRIVED_DESTINATION,
        "COMMON_INTRANSIT" to ParcelStatus.ARRIVED_DESTINATION,

        // Brazilian import clearance, where parcels sit longest.
        "CC_HO_IN_SUCCESS" to ParcelStatus.IMPORT_CUSTOMS,
        "CC_IM_START" to ParcelStatus.IMPORT_CUSTOMS,
        "CC_IM_SUCCESS" to ParcelStatus.IMPORT_CUSTOMS,
        "CC_HO_OUT_SUCCESS" to ParcelStatus.IMPORT_CUSTOMS,

        // Brazilian last mile.
        "GTMS_ACCEPT" to ParcelStatus.LAST_MILE,
        "GTMS_SC_DEPART" to ParcelStatus.LAST_MILE,
        "GTMS_DO_DEPART" to ParcelStatus.OUT_FOR_DELIVERY,
        "GSTA_INFORM_BUYER" to ParcelStatus.AWAITING_PICKUP,
        "GTMS_WAIT_SELF_PICK" to ParcelStatus.AWAITING_PICKUP,
        "GTMS_STA_SIGNED" to ParcelStatus.AWAITING_PICKUP,
        "GTMS_STA_SIGN_FAILURE" to ParcelStatus.FAILED_ATTEMPT,
        "GTMS_SIGNED" to ParcelStatus.DELIVERED,
        "SIGNIN_SUCCESS" to ParcelStatus.DELIVERED,
        "EXCEPTION" to ParcelStatus.EXCEPTION,
    )

    /**
     * Text probes for the events Cainiao relays verbatim from a Brazilian carrier.
     *
     * Ordered most specific first: "nao foi possivel entregar" has to lose to nothing else, and
     * the delivered probes must be tested before the looser "entrega" substrings.
     */
    private val TEXT_PROBES: List<Pair<ParcelStatus, List<String>>> = listOf(
        ParcelStatus.FAILED_ATTEMPT to listOf(
            "tentativa de entrega",
            "nao foi possivel entregar",
            "carteiro nao atendido",
            "destinatario ausente",
            "endereco incorreto",
            "endereco insuficiente",
            "delivery attempted",
            "failed delivery",
            "unsuccessful delivery",
        ),
        ParcelStatus.EXCEPTION to listOf(
            "objeto devolvido",
            "devolvido ao remetente",
            "extraviado",
            "avariado",
            "roubo",
            "furto",
            "sinistro",
            "returned to sender",
            "damaged",
            "lost",
        ),
        ParcelStatus.DELIVERED to listOf(
            "objeto entregue",
            "entregue ao destinatario",
            "entrega efetuada",
            "delivered",
        ),
        ParcelStatus.AWAITING_PICKUP to listOf(
            "aguardando retirada",
            "disponivel para retirada",
            "aguardando ser retirado",
            "retirar na unidade",
            "available for pickup",
            "awaiting collection",
        ),
        ParcelStatus.OUT_FOR_DELIVERY to listOf(
            "saiu para entrega",
            "em rota de entrega",
            "objeto a caminho",
            "out for delivery",
            "with delivery courier",
        ),
        ParcelStatus.IMPORT_CUSTOMS to listOf(
            "aguardando pagamento",
            "fiscalizacao aduaneira",
            "tributado",
            "liberado pela alfandega",
            "desembaraco aduaneiro",
            "aguardando parecer",
            "customs",
        ),
        ParcelStatus.LAST_MILE to listOf(
            "objeto postado",
            "objeto em transito",
            "encaminhado para",
            "recebido pela transportadora",
        ),
        ParcelStatus.IN_TRANSIT to listOf(
            "in transit",
            "departed",
            "arrived at",
        ),
    )

    /** Resolves [actionCode] first, then falls back to probing [description]. */
    fun resolve(actionCode: String?, description: String?): ParcelStatus {
        fromActionCode(actionCode)?.let { return it }
        return fromDescription(description) ?: ParcelStatus.UNKNOWN
    }

    fun fromActionCode(actionCode: String?): ParcelStatus? {
        val code = actionCode?.trim()?.uppercase().orEmpty()
        if (code.isEmpty()) return null
        return ACTION_CODES[code]
    }

    fun fromDescription(description: String?): ParcelStatus? {
        val text = description?.let(::foldForMatching).orEmpty()
        if (text.isEmpty()) return null
        return TEXT_PROBES.firstOrNull { (_, probes) -> probes.any { it in text } }?.first
    }

    /** Lowercases and strips the accents the probes are written without. */
    private fun foldForMatching(raw: String): String {
        val lower = raw.lowercase()
        return buildString(lower.length) {
            lower.forEach { char ->
                val index = ACCENTED.indexOf(char)
                append(if (index >= 0) UNACCENTED[index] else char)
            }
        }
    }

    private const val ACCENTED = "áàâãäéèêëíìîïóòôõöúùûüçñ"
    private const val UNACCENTED = "aaaaaeeeeiiiiooooouuuucn"
}
