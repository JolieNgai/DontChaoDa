package com.example.dontchaoda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.dontchaoda.navigation.AppNavHost
import com.example.dontchaoda.screens.AppDatabase
import com.example.dontchaoda.ui.theme.DontChaoDaTheme
import com.example.dontchaoda.viewmodel.AppViewModel
import com.example.dontchaoda.viewmodel.AppViewModelFactory


class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels {
        val db = AppDatabase.getDatabase(applicationContext)
        AppViewModelFactory(db.scanDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DontChaoDaTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    appViewModel = appViewModel
                )
            }
        }
    }
}
