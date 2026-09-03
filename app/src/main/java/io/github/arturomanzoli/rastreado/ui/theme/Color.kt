package io.github.arturomanzoli.rastreado.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** A burnt-orange brand palette, chosen to read as parcels and cardboard rather than as a default. */
internal val LightScheme = lightColorScheme(
    primary = Color(0xFF9A4A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC4),
    onPrimaryContainer = Color(0xFF321300),
    secondary = Color(0xFF705B41),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBDEBC),
    onSecondaryContainer = Color(0xFF271905),
    tertiary = Color(0xFF53643E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6EABA),
    onTertiaryContainer = Color(0xFF121F03),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF211A14),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF211A14),
    surfaceVariant = Color(0xFFF2DFD1),
    onSurfaceVariant = Color(0xFF51443A),
    outline = Color(0xFF837469),
    outlineVariant = Color(0xFFD5C3B5),
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFF753600),
    onPrimaryContainer = Color(0xFFFFDCC4),
    secondary = Color(0xFFDEC2A2),
    onSecondary = Color(0xFF3F2D17),
    secondaryContainer = Color(0xFF57432C),
    onSecondaryContainer = Color(0xFFFBDEBC),
    tertiary = Color(0xFFBACE9F),
    onTertiary = Color(0xFF263514),
    tertiaryContainer = Color(0xFF3C4C29),
    onTertiaryContainer = Color(0xFFD6EABA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191210),
    onBackground = Color(0xFFEDE0D9),
    surface = Color(0xFF191210),
    onSurface = Color(0xFFEDE0D9),
    surfaceVariant = Color(0xFF51443A),
    onSurfaceVariant = Color(0xFFD5C3B5),
    outline = Color(0xFF9E8E82),
    outlineVariant = Color(0xFF51443A),
)

/** Status accents that must stay recognisable regardless of the active colour scheme. */
internal val DeliveredGreenLight = Color(0xFF1B6B36)
internal val DeliveredGreenDark = Color(0xFF7EDB9E)
internal val OutForDeliveryLight = Color(0xFFA34E00)
internal val OutForDeliveryDark = Color(0xFFFFB77C)
internal val CustomsBlueLight = Color(0xFF00558F)
internal val CustomsBlueDark = Color(0xFF9CCAFF)
