package io.github.arturomanzoli.rastreado.data.remote.cainiao

import io.github.arturomanzoli.rastreado.data.remote.ProviderResult
import io.github.arturomanzoli.rastreado.data.remote.TrackingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Reads the consumer tracking feed that backs AliExpress's own order pages.
 *
 * It needs no key and, for Brazil-bound parcels, relays the last-mile carrier's events verbatim,
 * which is the only way to see Correios progress now that `API Rastro` is restricted to the
 * contracted sender.
 */
class CainiaoProvider(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: String = DEFAULT_BASE_URL,
) : TrackingProvider {

    override val id: String = PROVIDER_ID

    override suspend fun fetch(code: String, languageTag: String): ProviderResult = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addQueryParameter("mailNos", code)
            .addQueryParameter("lang", languageTag)
            .addQueryParameter("language", languageTag)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            // The endpoint answers with an HTML challenge to clients that send no user agent.
            .header("User-Agent", USER_AGENT)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ProviderResult.Failure(
                        ProviderResult.Reason.SERVER,
                        "HTTP ${response.code}",
                    )
                }
                val body = response.body.string()

                val parsed = runCatching { json.decodeFromString<CainiaoResponse>(body) }
                    .getOrElse { error ->
                        return@withContext ProviderResult.Failure(
                            ProviderResult.Reason.MALFORMED,
                            error.message,
                        )
                    }

                if (!parsed.success) {
                    return@withContext ProviderResult.Failure(ProviderResult.Reason.SERVER, "success=false")
                }

                val snapshot = CainiaoMapper.toSnapshot(parsed, code)
                    ?: return@withContext ProviderResult.Empty(code)
                ProviderResult.Success(snapshot)
            }
        } catch (error: IOException) {
            ProviderResult.Failure(ProviderResult.Reason.NETWORK, error.message)
        }
    }

    companion object {
        const val PROVIDER_ID = "cainiao"
        const val DEFAULT_BASE_URL = "https://global.cainiao.com/global/detail.json"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
