package io.github.arturomanzoli.rastreado

import android.content.Context
import io.github.arturomanzoli.rastreado.data.ParcelRepository
import io.github.arturomanzoli.rastreado.data.local.RastreadoDatabase
import io.github.arturomanzoli.rastreado.data.remote.cainiao.CainiaoProvider
import io.github.arturomanzoli.rastreado.data.settings.SettingsRepository
import io.github.arturomanzoli.rastreado.notify.Notifications
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Wires the app's singletons by hand.
 *
 * A dependency-injection framework would earn its keep at a larger scale; here it would add a
 * compiler plugin and a worker factory to construct five objects.
 */
class AppContainer(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val database = RastreadoDatabase.build(context)

    val settingsRepository = SettingsRepository(context)

    val notifications = Notifications(context)

    val parcelRepository = ParcelRepository(
        dao = database.parcelDao(),
        provider = CainiaoProvider(httpClient, json),
    )

    private companion object {
        const val HTTP_TIMEOUT_SECONDS = 20L
    }
}
