package com.example.dontchaoda.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.dontchaoda.screens.*
import com.example.dontchaoda.viewmodel.AppViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel
) {
    LaunchedEffect(Unit) {
        appViewModel.generateReminders()
    }

    NavHost(
        navController = navController,
        startDestination = if (appViewModel.isLoggedIn) Screen.Home.route else Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            if (appViewModel.isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            } else {
                WelcomeScreen(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onRegisterClick = { navController.navigate(Screen.Register.route) }
                )
            }
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLogin = { email, password, result ->
                    appViewModel.login(email, password) { ok, message ->
                        if (ok) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                        result(ok, message)
                    }
                },
                onForgotPassword = { email, result ->
                    appViewModel.resetPassword(email) { ok, message ->
                        result(ok, message)
                    }
                },
                onGoToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onGoToHome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegister = { email, password, result ->
                    appViewModel.register(email, password) { ok, message ->
                        if (ok) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                        result(ok, message)
                    }
                },
                onGoToLogin = { navController.navigate(Screen.Login.route) },
                onGoToHome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                appViewModel = appViewModel,
                onStartScan = { navController.navigate(Screen.RoomSelection.route) },
                onLogout = {
                    appViewModel.logout()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                viewModel = appViewModel,
                onLogout = {
                    appViewModel.logout()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RoomSelection.route) {
            RoomSelectionScreen(
                navController = navController,
                viewModel = appViewModel,
                onRoomSelected = { roomName ->
                    appViewModel.selectedRoom = roomName
                    navController.navigate(Screen.ARScan.route)
                }
            )
        }

        composable(Screen.ARScan.route) {
            ARScanScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }

        composable(Screen.ScanSummary.route) {
            ScanSummaryScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }

        composable(Screen.RoomHistory.route) {
            RoomHistoryScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }

        composable(Screen.Reminders.route) {
            RemindersScreen(
                appViewModel = appViewModel,
                navController = navController
            )
        }
        composable(Screen.Recommendation.route) {
            RecommendationScreen(
                navController = navController,
                appViewModel = appViewModel
            )
        }

        composable(Screen.FAQ.route) {
            FAQScreen(
                navController = navController
            )
        }
    }
}
