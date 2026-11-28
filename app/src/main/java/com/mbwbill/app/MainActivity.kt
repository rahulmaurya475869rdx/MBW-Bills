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
                    // Home screen search bar ke liye "scanned_result" use hota hai
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

                    // Add/Edit screen multiple serials ke liye "scanned_index" aur "scanned_value" use hota hai
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
                            val previousEntry = nav.previousBackStackEntry
                            
                            // 👇 FIX: Check karo ki scan kahan se aaya hai
                            if (previousEntry?.savedStateHandle?.contains("pending_scan_index") == true) {
                                // Scan aaya hai Add/Edit Screen se (Serial Number ke liye)
                                val pendingIndex = previousEntry.savedStateHandle.get<Int>("pending_scan_index") ?: 0
                                previousEntry.savedStateHandle["scanned_index"] = pendingIndex
                                previousEntry.savedStateHandle["scanned_value"] = result
                                previousEntry.savedStateHandle.remove<Int>("pending_scan_index")
                            } else {
                                // Scan aaya hai Home Screen se (Search Bar ke liye)
                                previousEntry?.savedStateHandle?.set("scanned_result", result)
                            }
                            
                            nav.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
