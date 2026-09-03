package io.github.arturomanzoli.rastreado

import android.app.Application
import io.github.arturomanzoli.rastreado.work.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class RastreadoApp : Application() {

    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.ensureChannels()

        // Re-scheduling only when the schedule itself changes, so unrelated preferences such as
        // the theme do not reset the periodic work's timer.
        scope.launch {
            container.settingsRepository.settings
                .distinctUntilChangedBy { it.syncInterval to it.syncOnlyOnUnmetered }
                .collect { SyncScheduler.schedule(this@RastreadoApp, it) }
        }
    }
}
