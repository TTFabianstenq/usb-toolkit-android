package com.usbtoolkit.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.usbtoolkit.app.ui.navigation.NavRoutes
import com.usbtoolkit.app.ui.screens.*
import com.usbtoolkit.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbToolkitAppRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomItems = listOf(
        Triple(NavRoutes.HOME, "Home", Icons.Default.Home),
        Triple(NavRoutes.BROWSE, "Browse", Icons.Default.Folder),
        Triple(NavRoutes.DOWNLOADS, "Downloads", Icons.Default.Download),
        Triple(NavRoutes.DEVICE_FILES, "Device", Icons.Default.Usb),
        Triple(NavRoutes.SETTINGS, "Settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onFormat = { navController.navigate(NavRoutes.FORMAT) },
                    onBrowse = { navController.navigate(NavRoutes.BROWSE) },
                    onCopy = { navController.navigate(NavRoutes.COPY) },
                    onDownloads = { navController.navigate(NavRoutes.DOWNLOADS) },
                    onSettings = { navController.navigate(NavRoutes.SETTINGS) }
                )
            }
            composable(NavRoutes.FORMAT) {
                FormatScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.BROWSE) {
                BrowseScreen(viewModel = viewModel)
            }
            composable(NavRoutes.COPY) {
                CopyScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.DOWNLOADS) {
                DownloadsScreen(viewModel = viewModel)
            }
            composable(NavRoutes.DEVICE_FILES) {
                DeviceFilesScreen(viewModel = viewModel)
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onAbout = { navController.navigate(NavRoutes.ABOUT) }
                )
            }
            composable(NavRoutes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
