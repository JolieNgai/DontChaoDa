package com.example.dontchaoda.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dontchaoda.navigation.Screen

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            val isHome = screen == Screen.Home
            val label = if (isHome) "Home" else "Profile"
            val icon = if (isHome) Icons.Default.Home else Icons.Default.Person
            
            // The "Home" tab is selected if we are on the Home screen OR the Room Selection screen
            val isSelected = if (isHome) {
                currentRoute == Screen.Home.route || currentRoute == Screen.RoomSelection.route
            } else {
                currentRoute == screen.route
            }

            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label) },
                selected = isSelected,
                onClick = {
                    // Special case: If we're in Room Selection and click the Home button, 
                    // force navigation back to the root Home screen.
                    if (isHome && currentRoute == Screen.RoomSelection.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
