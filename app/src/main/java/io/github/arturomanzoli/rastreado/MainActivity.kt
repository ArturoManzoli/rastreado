package io.github.arturomanzoli.rastreado

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arturomanzoli.rastreado.data.settings.Settings
import io.github.arturomanzoli.rastreado.ui.RastreadoNavHost
import io.github.arturomanzoli.rastreado.ui.theme.RastreadoTheme

/**
 * Extends [AppCompatActivity] because the per-app language override is applied through
 * `AppCompatDelegate`, which needs an AppCompat host to take effect below Android 13.
 */
class MainActivity : AppCompatActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        val settingsFlow = (application as RastreadoApp).container.settingsRepository.settings
        val initialCode = intent?.getStringExtra(EXTRA_PARCEL_CODE)

        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = Settings())
            RastreadoTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                RastreadoNavHost(initialParcelCode = initialCode)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val notifications = (application as RastreadoApp).container.notifications
        if (notifications.canPost()) return
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val EXTRA_PARCEL_CODE = "parcel_code"
    }
}
