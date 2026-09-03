package io.github.arturomanzoli.rastreado.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.arturomanzoli.rastreado.RastreadoApp
import io.github.arturomanzoli.rastreado.data.remote.ProviderResult
import io.github.arturomanzoli.rastreado.data.settings.AlarmMode
import kotlinx.coroutines.flow.first

/** Polls every tracked parcel and announces whatever changed. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as RastreadoApp).container
        val settings = container.settingsRepository.settings.first()

        val outcomes = container.parcelRepository.syncAll(providerLanguageTag())

        outcomes.forEach { outcome ->
            if (!settings.notificationsEnabled || !outcome.parcel.notificationsEnabled) return@forEach

            val ringAlarm = outcome.becameOutForDelivery &&
                settings.alarmMode == AlarmMode.ALARM &&
                outcome.parcel.alarmEnabled

            if (ringAlarm) {
                container.notifications.notifyOutForDelivery(outcome.parcel)
            } else {
                container.notifications.notifyUpdate(outcome.parcel, outcome.newEvents)
            }
        }

        // Only a connectivity failure is worth retrying ahead of the next period; a malformed or
        // rejected response will look the same in a minute's time.
        val retryable = outcomes.any { it.failure?.reason == ProviderResult.Reason.NETWORK }
        return if (retryable) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_PERIODIC_NAME = "parcel-sync-periodic"
        const val UNIQUE_ONE_SHOT_NAME = "parcel-sync-now"
    }
}
