package io.github.arturomanzoli.rastreado.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.arturomanzoli.rastreado.AppContainer
import io.github.arturomanzoli.rastreado.RastreadoApp

@Composable
fun rememberContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) { (context.applicationContext as RastreadoApp).container }
}
