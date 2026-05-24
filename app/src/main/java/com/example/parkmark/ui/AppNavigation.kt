package com.example.parkmark.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.parkmark.ui.screens.HistoryScreen
import com.example.parkmark.ui.screens.MapScreen
import com.example.parkmark.ui.screens.SettingsScreen
import com.example.parkmark.viewmodel.ParkingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel : ParkingViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "map") {

        composable("map") {
            MapScreen(navController = navController, viewModel = viewModel)
        }
        composable("history") {
            HistoryScreen(navController = navController, viewModel = viewModel)
        }
        composable("settings") {
            SettingsScreen(navController = navController, viewModel = viewModel)
        }

    }

}