package io.github.arturomanzoli.rastreado.domain

/** Canonical parcel status, independent of any provider's own vocabulary. */
enum class ParcelStatus {
    UNKNOWN,
    PENDING,
    WAREHOUSE,
    SORTING,
    EXPORT_CUSTOMS,
    IN_TRANSIT,
    ARRIVED_DESTINATION,
    IMPORT_CUSTOMS,
    LAST_MILE,
    OUT_FOR_DELIVERY,
    AWAITING_PICKUP,
    FAILED_ATTEMPT,
    EXCEPTION,
    DELIVERED,
    ;

    /** True once the parcel needs no further polling. */
    val isTerminal: Boolean get() = this == DELIVERED

    /** True for the states that happen on Brazilian soil. */
    val isLastMile: Boolean
        get() = this in setOf(IMPORT_CUSTOMS, LAST_MILE, OUT_FOR_DELIVERY, AWAITING_PICKUP, FAILED_ATTEMPT)

    /** True when the state warrants the user's attention rather than a silent update. */
    val needsAttention: Boolean get() = this in setOf(AWAITING_PICKUP, FAILED_ATTEMPT, EXCEPTION)
}
