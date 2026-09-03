package io.github.arturomanzoli.rastreado.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.arturomanzoli.rastreado.data.settings.ThemeMode

data class StatusPalette(
    val delivered: Color,
    val outForDelivery: Color,
    val customs: Color,
)

val LocalStatusPalette = staticCompositionLocalOf {
    StatusPalette(DeliveredGreenLight, OutForDeliveryLight, CustomsBlueLight)
}

@Composable
fun RastreadoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        dark -> DarkScheme
        else -> LightScheme
    }

    val statusPalette = if (dark) {
        StatusPalette(DeliveredGreenDark, OutForDeliveryDark, CustomsBlueDark)
    } else {
        StatusPalette(DeliveredGreenLight, OutForDeliveryLight, CustomsBlueLight)
    }

    CompositionLocalProvider(LocalStatusPalette provides statusPalette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
