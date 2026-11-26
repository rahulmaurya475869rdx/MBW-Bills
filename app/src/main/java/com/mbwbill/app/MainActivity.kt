package com.mbwbill.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.mbwbill.app.ui.AddEditScreen
import com.mbwbill.app.ui.DetailScreen
import com.mbwbill.app.ui.HomeScreen
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
                composable("home") {
                    HomeScreen(
                        viewModel = vm,
                        onAdd = { nav.navigate("edit/0") },
                        onOpen = { id -> nav.navigate("detail/$id") }
                    )
                }
                composable(
                    "edit/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    AddEditScreen(
                        customerId = id,
                        viewModel = vm,
                        onBack = { nav.popBackStack() }
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
            }
        }
    }
}
