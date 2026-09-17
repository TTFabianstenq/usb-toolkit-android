package com.usbtoolkit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.usbtoolkit.app.data.ThemeMode
import com.usbtoolkit.app.ui.UsbToolkitAppRoot
import com.usbtoolkit.app.ui.theme.USBToolkitTheme
import com.usbtoolkit.app.viewmodel.MainViewModel
import com.usbtoolkit.app.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as UsbToolkitApp
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(app)
            )
            val settings by viewModel.settings.collectAsState()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            USBToolkitTheme(darkTheme = darkTheme) {
                UsbToolkitAppRoot(viewModel = viewModel)
            }
        }
    }
}
