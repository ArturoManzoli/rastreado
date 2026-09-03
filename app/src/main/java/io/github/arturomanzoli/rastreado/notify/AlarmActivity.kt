package io.github.arturomanzoli.rastreado.notify

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import io.github.arturomanzoli.rastreado.R
import io.github.arturomanzoli.rastreado.RastreadoApp
import io.github.arturomanzoli.rastreado.ui.theme.RastreadoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The ringing screen shown when a parcel goes out for delivery.
 *
 * Reached through the notification's full-screen intent, which is the only sanctioned way to put
 * an activity in front of a locked device from the background.
 */
class AlarmActivity : ComponentActivity() {

    private val player by lazy { AlarmPlayer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showOverLockScreen()

        val code = intent.getStringExtra(EXTRA_CODE).orEmpty()
        val label = intent.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() } ?: code

        // Cancelling the notification silences the tone the channel started, so this screen's
        // looping alarm takes over instead of playing on top of it.
        NotificationManagerCompat.from(this).cancel(Notifications.alarmNotificationId(code))

        val container = (application as RastreadoApp).container
        lifecycleScope.launch {
            val settings = container.settingsRepository.settings.first()
            player.start(vibrate = settings.alarmVibrate)
        }

        setContent {
            RastreadoTheme {
                AlarmScreen(
                    parcelLabel = label,
                    onDismiss = { finish() },
                    onSnooze = {
                        AlarmReceiver.scheduleSnooze(this, code)
                        finish()
                    },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        player.stop()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_CODE = "parcel_code"
        private const val EXTRA_LABEL = "parcel_label"

        fun intent(context: Context, code: String, label: String): Intent =
            Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_CODE, code)
                putExtra(EXTRA_LABEL, label)
            }
    }
}

@Composable
private fun AlarmScreen(parcelLabel: String, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.errorContainer) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.LocalShipping,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.notification_out_for_delivery_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = parcelLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(48.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_dismiss_alarm))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_snooze_alarm))
            }
        }
    }
}
