package io.github.arturomanzoli.rastreado.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.arturomanzoli.rastreado.ui.add.AddParcelScreen
import io.github.arturomanzoli.rastreado.ui.detail.ParcelDetailScreen
import io.github.arturomanzoli.rastreado.ui.parcels.ParcelListScreen
import io.github.arturomanzoli.rastreado.ui.settings.SettingsScreen

private object Routes {
    const val LIST = "parcels"
    const val ADD = "parcels/add"
    const val SETTINGS = "settings"
    const val DETAIL = "parcels/{code}"

    fun detail(code: String) = "parcels/$code"
}

@Composable
fun RastreadoNavHost(
    /** Set when the app was opened from a notification, to land straight on that parcel. */
    initialParcelCode: String? = null,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(initialParcelCode) {
        if (!initialParcelCode.isNullOrBlank()) {
            navController.navigate(Routes.detail(initialParcelCode))
        }
    }

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            ParcelListScreen(
                onOpenParcel = { code -> navController.navigate(Routes.detail(code)) },
                onAddParcel = { navController.navigate(Routes.ADD) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.ADD) {
            AddParcelScreen(
                onClose = { navController.popBackStack() },
                onSaved = { code ->
                    navController.popBackStack()
                    navController.navigate(Routes.detail(code))
                },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("code") { type = NavType.StringType }),
        ) { entry ->
            ParcelDetailScreen(
                code = entry.arguments?.getString("code").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
