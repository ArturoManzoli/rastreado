package io.github.arturomanzoli.rastreado.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/** Plays the looping alarm while [AlarmActivity] is on screen. */
class AlarmPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    fun start(vibrate: Boolean) {
        startTone()
        if (vibrate) startVibration()
    }

    fun stop() {
        player?.let { active ->
            runCatching { active.stop() }
            active.release()
        }
        player = null
        vibrator()?.cancel()
    }

    private fun startTone() {
        if (player != null) return
        val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, tone)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure { Log.w(TAG, "Could not start the alarm tone", it) }.getOrNull()
    }

    private fun startVibration() {
        val vibrator = vibrator() ?: return
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(PATTERN, REPEAT_FROM)
        runCatching { vibrator.vibrate(effect) }
            .onFailure { Log.w(TAG, "Could not start the alarm vibration", it) }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private companion object {
        const val TAG = "AlarmPlayer"
        val PATTERN = longArrayOf(0, 600, 600)
        const val REPEAT_FROM = 0
    }
}
