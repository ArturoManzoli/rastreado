package io.github.arturomanzoli.rastreado.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.arturomanzoli.rastreado.data.settings.Settings
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The BCP 47 tag to ask the provider for, so its event text arrives already translated.
 *
 * Reads the effective locale, which already reflects any per-app language override.
 */
fun providerLanguageTag(): String =
    if (Locale.getDefault().language.equals("pt", ignoreCase = true)) "pt-BR" else "en-US"

object SyncScheduler {

    fun schedule(context: Context, settings: Settings) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            settings.syncInterval.minutes,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints(settings))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** A user-triggered refresh, which must not wait for the next period. */
    fun syncNow(context: Context, settings: Settings) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints(settings))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.UNIQUE_ONE_SHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun constraints(settings: Settings) = Constraints.Builder()
        .setRequiredNetworkType(
            if (settings.syncOnlyOnUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED,
        )
        .build()

    private const val BACKOFF_MINUTES = 15L
}
