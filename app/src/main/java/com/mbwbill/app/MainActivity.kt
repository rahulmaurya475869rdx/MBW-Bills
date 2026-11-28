package com.mbwbill.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mbwbill.app.ui.AddEditScreen
import com.mbwbill.app.ui.DetailScreen
import com.mbwbill.app.ui.HomeScreen
import com.mbwbill.app.ui.ScannerScreen
import com.mbwbill.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val nav = rememberNavController()
            val vm: MainViewModel = viewModel()

            NavHost(navController = nav, startDestination = "home") {
                composable("home") { entry ->
                    val scannedResult by entry.savedStateHandle
                        .getStateFlow("scanned_result", "")
                        .collectAsState()

                    LaunchedEffect(scannedResult) {
                        if (scannedResult.isNotBlank()) {
                            vm.setQuery(scannedResult)
                            entry.savedStateHandle.remove<String>("scanned_result")
                        }
                    }

                    HomeScreen(
                        viewModel = vm,
                        onAdd = { nav.navigate("edit/0") },
                        onOpen = { id -> nav.navigate("detail/$id") },
                        onScan = { nav.navigate("scanner") }
                    )
                }

                composable(
                    "edit/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L

                    // Scan result with index — stored in savedStateHandle
                    val scannedIndex by entry.savedStateHandle
                        .getStateFlow("scanned_index", -1)
                        .collectAsState()
                    val scannedValue by entry.savedStateHandle
                        .getStateFlow("scanned_value", "")
                        .collectAsState()

                    AddEditScreen(
                        customerId = id,
                        viewModel = vm,
                        onBack = { nav.popBackStack() },
                        onScanSerial = { index ->
                            // scanIndex ko savedStateHandle me save karo
                            entry.savedStateHandle["pending_scan_index"] = index
                            nav.navigate("scanner")
                        },
                        scannedIndex = scannedIndex,
                        scannedValue = scannedValue,
                        onScannedConsumed = {
                            entry.savedStateHandle.remove<Int>("scanned_index")
                            entry.savedStateHandle.remove<String>("scanned_value")
                        }
                    )
                }

                composable(
                    "detail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    DetailScreen(
                        customerId = id,
                        viewModel = vm,
                        onBack = { nav.popBackStack() },
                        onEdit = { nav.navigate("edit/$id") }
                    )
                }

                composable("scanner") {
                    ScannerScreen(
                        onBack = { nav.popBackStack() },
                        onScanned = { result ->
                            val editEntry = nav.previousBackStackEntry
                            val pendingIndex = editEntry?.savedStateHandle
                                ?.get<Int>("pending_scan_index") ?: 0
                            editEntry?.savedStateHandle?.set("scanned_index", pendingIndex)
                            editEntry?.savedStateHandle?.set("scanned_value", result)
                            editEntry?.savedStateHandle?.remove<Int>("pending_scan_index")
                            nav.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
