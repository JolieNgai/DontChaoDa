package com.example.dontchaoda.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object Login : Screen("login")
    object Register : Screen("register")
    object RoomSelection : Screen("room_selection")
    object ARScan : Screen("ar_scan")
    object Profile : Screen("profile")
    object ScanSummary : Screen("scan_summary")
    object RoomHistory : Screen("room_history")
    object Reminders : Screen("reminders")
    object FAQ : Screen("faq")
    object Recommendation : Screen("recommendation")
}
