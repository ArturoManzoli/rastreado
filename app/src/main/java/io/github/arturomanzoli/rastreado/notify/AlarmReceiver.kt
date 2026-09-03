package io.github.arturomanzoli.rastreado.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import io.github.arturomanzoli.rastreado.RastreadoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Handles the alarm notification's actions and re-rings a snoozed alarm. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(EXTRA_CODE).orEmpty()
        if (code.isEmpty()) return

        when (intent.action) {
            ACTION_DISMISS -> cancel(context, code)

            ACTION_SNOOZE -> {
                cancel(context, code)
                scheduleSnooze(context, code)
            }

            ACTION_RING -> ring(context, code)
        }
    }

    private fun cancel(context: Context, code: String) {
        NotificationManagerCompat.from(context).cancel(Notifications.alarmNotificationId(code))
    }

    /**
     * Re-posts the alarm after a snooze. The parcel is re-read rather than carried in the intent
     * so a snoozed alarm reflects any status the parcel reached while it was silenced.
     */
    private fun ring(context: Context, code: String) {
        val pending = goAsync()
        val container = (context.applicationContext as RastreadoApp).container
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val parcel = container.parcelRepository.observeParcel(code).first() ?: return@launch
                if (parcel.status.isTerminal || !parcel.alarmEnabled) return@launch
                container.notifications.notifyOutForDelivery(parcel)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_DISMISS = "io.github.arturomanzoli.rastreado.DISMISS_ALARM"
        private const val ACTION_SNOOZE = "io.github.arturomanzoli.rastreado.SNOOZE_ALARM"
        private const val ACTION_RING = "io.github.arturomanzoli.rastreado.RING_ALARM"
        private const val EXTRA_CODE = "parcel_code"

        private const val SNOOZE_MILLIS = 15 * 60 * 1000L

        private const val PENDING_FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun dismissIntent(context: Context, code: String): PendingIntent =
            broadcast(context, code, ACTION_DISMISS)

        fun snoozeIntent(context: Context, code: String): PendingIntent =
            broadcast(context, code, ACTION_SNOOZE)

        fun scheduleSnooze(context: Context, code: String) {
            val manager = context.getSystemService(AlarmManager::class.java) ?: return
            val triggerAt = SystemClock.elapsedRealtime() + SNOOZE_MILLIS
            val operation = broadcast(context, code, ACTION_RING)

            // Exact scheduling needs a permission the user can revoke; an inexact snooze is a
            // few minutes late at worst, which is better than no snooze at all.
            val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
            if (canBeExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, operation)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, operation)
            }
        }

        private fun broadcast(context: Context, code: String, action: String): PendingIntent {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_CODE, code)
            }
            return PendingIntent.getBroadcast(context, action.hashCode() + code.hashCode(), intent, PENDING_FLAGS)
        }
    }
}
