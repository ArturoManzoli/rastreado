package io.github.arturomanzoli.rastreado.data.remote.cainiao

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CainiaoResponse(
    val success: Boolean = false,
    val module: List<CainiaoParcel> = emptyList(),
)

@Serializable
data class CainiaoParcel(
    val mailNo: String = "",
    /** Localised label plus the last-mile code, tab separated. */
    val realMailNo: String? = null,
    val originCountry: String? = null,
    val destCountry: String? = null,
    val status: String? = null,
    val statusDesc: String? = null,
    val mailNoSource: String? = null,
    val processInfo: CainiaoProcessInfo? = null,
    val latestTrace: CainiaoTrace? = null,
    @SerialName("globalCombinedLogisticsTraceDTO")
    val combinedTrace: CainiaoTrace? = null,
    val detailList: List<CainiaoTrace> = emptyList(),
)

@Serializable
data class CainiaoProcessInfo(
    val progressStatus: String? = null,
    val progressRate: Float? = null,
    val type: String? = null,
)

@Serializable
data class CainiaoTrace(
    /** Epoch milliseconds. */
    val time: Long = 0L,
    val timeStr: String? = null,
    val desc: String? = null,
    /** The human-readable event text; carries a `[CITY]` prefix on Brazilian legs. */
    val standerdDesc: String? = null,
    val actionCode: String? = null,
    val timeZone: String? = null,
    val group: CainiaoTraceGroup? = null,
    val currentStatusCpInfo: CainiaoCarrier? = null,
)

@Serializable
data class CainiaoTraceGroup(
    val nodeCode: String? = null,
    val nodeDesc: String? = null,
)

@Serializable
data class CainiaoCarrier(
    val cpName: String? = null,
    val phone: String? = null,
    val url: String? = null,
)
