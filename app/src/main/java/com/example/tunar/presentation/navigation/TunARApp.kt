package com.example.tunar.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tunar.R
import com.example.tunar.di.AppContainer
import com.example.tunar.presentation.calibration.CalibrationRoute
import com.example.tunar.presentation.calibration.CalibrationViewModel
import com.example.tunar.presentation.tuner.TunerMode
import com.example.tunar.presentation.tuner.TunerRoute
import com.example.tunar.presentation.tuner.TunerViewModel

object Routes {
    const val BASIC = "basic"
    const val PRO = "pro"
    const val CALIBRATION = "calibration"
}

private enum class TopLevel(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    BASIC(Routes.BASIC, R.string.nav_basic, Icons.Filled.GraphicEq),
    PRO(Routes.PRO, R.string.nav_pro, Icons.Filled.Tune),
}

/** Raíz de la UI: barra inferior Básico/Pro (RF03) y grafo de navegación. */
@Composable
fun TunARApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (TopLevel.entries.any { it.route == currentRoute }) {
                NavigationBar {
                    TopLevel.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.BASIC,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Routes.BASIC) {
                TunerRoute(
                    viewModel = viewModel(factory = tunerFactory(container, TunerMode.BASIC)),
                    onOpenCalibration = { navController.navigate(Routes.CALIBRATION) },
                )
            }
            composable(Routes.PRO) {
                TunerRoute(
                    viewModel = viewModel(factory = tunerFactory(container, TunerMode.PRO)),
                    onOpenCalibration = { navController.navigate(Routes.CALIBRATION) },
                )
            }
            composable(Routes.CALIBRATION) {
                CalibrationRoute(
                    viewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                CalibrationViewModel(
                                    observePitch = container.observePitch(),
                                    audioRepository = container.audioRepository,
                                    configRepository = container.detectionConfigRepository,
                                )
                            }
                        },
                    ),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun tunerFactory(container: AppContainer, mode: TunerMode) = viewModelFactory {
    initializer {
        TunerViewModel(
            mode = mode,
            observePitch = container.observePitch(),
            analyzePitch = container.analyzePitch,
            editProProfile = container.editProProfile,
            profileRepository = container.tuningProfileRepository,
            detectionConfigRepository = container.detectionConfigRepository,
        )
    }
}
