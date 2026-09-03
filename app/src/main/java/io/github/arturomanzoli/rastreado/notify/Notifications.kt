package io.github.arturomanzoli.rastreado.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.arturomanzoli.rastreado.MainActivity
import io.github.arturomanzoli.rastreado.R
import io.github.arturomanzoli.rastreado.domain.Parcel
import io.github.arturomanzoli.rastreado.domain.TrackingEvent

/** Owns the notification channels and every notification the app posts. */
class Notifications(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val updates = NotificationChannel(
            CHANNEL_UPDATES,
            context.getString(R.string.channel_updates_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_updates_description)
        }

        val outForDelivery = NotificationChannel(
            CHANNEL_OUT_FOR_DELIVERY,
            context.getString(R.string.channel_out_for_delivery_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_out_for_delivery_description)
            // Routing the sound through the alarm stream is what makes this audible when the
            // ringer is silenced, which is the whole point of announcing it as an alarm.
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            enableLights(true)
        }

        val problems = NotificationChannel(
            CHANNEL_PROBLEMS,
            context.getString(R.string.channel_problems_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_problems_description)
        }

        manager.createNotificationChannels(listOf(updates, outForDelivery, problems))
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** One notification per parcel, summarising the events that arrived in this sync. */
    fun notifyUpdate(parcel: Parcel, newEvents: List<TrackingEvent>) {
        if (newEvents.isEmpty()) return
        val latest = newEvents.first()
        val title = parcel.displayName()
        val body = latest.location?.let { "${latest.description} - $it" } ?: latest.description

        val builder = NotificationCompat.Builder(context, channelFor(parcel, newEvents))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openParcelIntent(parcel.code))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        if (newEvents.size > 1) {
            val style = NotificationCompat.InboxStyle()
            newEvents.take(MAX_INBOX_LINES).forEach { style.addLine(it.description) }
            style.setSummaryText(
                context.resources.getQuantityString(
                    R.plurals.notification_events_count,
                    newEvents.size,
                    newEvents.size,
                ),
            )
            builder.setStyle(style)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        post(parcel.code.hashCode(), builder.build())
    }

    /**
     * The ringing announcement. The full-screen intent is what turns it into an alarm the user
     * cannot sleep through; when the platform withholds that, the alarm-stream sound on the
     * channel still fires, so this degrades rather than going silent.
     */
    fun notifyOutForDelivery(parcel: Parcel) {
        val notification = NotificationCompat.Builder(context, CHANNEL_OUT_FOR_DELIVERY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_out_for_delivery_title))
            .setContentText(context.getString(R.string.notification_out_for_delivery_body, parcel.displayName()))
            .setContentIntent(openParcelIntent(parcel.code))
            .setFullScreenIntent(alarmScreenIntent(parcel), true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.action_dismiss_alarm),
                AlarmReceiver.dismissIntent(context, parcel.code),
            )
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.action_snooze_alarm),
                AlarmReceiver.snoozeIntent(context, parcel.code),
            )
            .build()

        post(alarmNotificationId(parcel.code), notification)
    }

    fun cancelAlarm(code: String) = manager.cancel(alarmNotificationId(code))

    private fun channelFor(parcel: Parcel, events: List<TrackingEvent>): String =
        if (events.any { it.status.needsAttention } || parcel.status.needsAttention) {
            CHANNEL_PROBLEMS
        } else {
            CHANNEL_UPDATES
        }

    private fun post(id: Int, notification: android.app.Notification) {
        if (!canPost()) return
        manager.notify(id, notification)
    }

    private fun openParcelIntent(code: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_PARCEL_CODE, code)
        }
        return PendingIntent.getActivity(context, code.hashCode(), intent, PENDING_FLAGS)
    }

    private fun alarmScreenIntent(parcel: Parcel): PendingIntent =
        PendingIntent.getActivity(
            context,
            alarmNotificationId(parcel.code),
            AlarmActivity.intent(context, parcel.code, parcel.displayName()),
            PENDING_FLAGS,
        )

    private fun Parcel.displayName(): String = name?.takeIf { it.isNotBlank() } ?: code

    companion object {
        const val CHANNEL_UPDATES = "tracking_updates"
        const val CHANNEL_OUT_FOR_DELIVERY = "out_for_delivery"
        const val CHANNEL_PROBLEMS = "tracking_problems"

        private const val MAX_INBOX_LINES = 5
        private const val ALARM_ID_OFFSET = 1_000_000

        private const val PENDING_FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun alarmNotificationId(code: String): Int = ALARM_ID_OFFSET + code.hashCode()
    }
}
