package com.example.dsatracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.dsatracker.presentation.auth.LoginScreen
import com.example.dsatracker.presentation.auth.RegisterScreen
import com.example.dsatracker.presentation.dashboard.DashboardScreen

@Composable
fun NavGraph(startDestination: String) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(navController)
        }
        composable("register") {
            RegisterScreen(navController)
        }
        composable("dashboard") {
            DashboardScreen(navController)
        }
    }
}