package com.harouat.syncedauth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harouat.syncedauth.ui.screens.AddAccountScreen
import com.harouat.syncedauth.ui.screens.HomeScreen
import com.harouat.syncedauth.ui.screens.QrScanScreen
import com.harouat.syncedauth.ui.theme.SyncedAuthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyncedAuthTheme {
                val vm: MainViewModel = viewModel()
                // Pin LTR so the app doesn't mirror on Arabic-locale devices
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AppRoot(vm)
                }
            }
        }
    }
}

private const val ROUTE_HOME = "home"
private const val ROUTE_ADD = "add"
private const val ROUTE_SCAN = "scan"

@Composable
private fun AppRoot(vm: MainViewModel) {
    var route by rememberSaveable { mutableStateOf(ROUTE_HOME) }

    BackHandler(enabled = route != ROUTE_HOME) { route = ROUTE_HOME }

    when (route) {
        ROUTE_ADD -> AddAccountScreen(
            vm = vm,
            onClose = { route = ROUTE_HOME },
            onScanQr = { route = ROUTE_SCAN },
        )
        ROUTE_SCAN -> QrScanScreen(vm = vm, onClose = { route = ROUTE_HOME })
        else -> HomeScreen(vm = vm, onAddAccount = { route = ROUTE_ADD })
    }
}
