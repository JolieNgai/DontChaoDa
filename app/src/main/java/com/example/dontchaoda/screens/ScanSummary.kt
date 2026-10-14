package com.example.dontchaoda.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dontchaoda.navigation.Screen
import com.example.dontchaoda.viewmodel.AppViewModel

@Composable
fun ScanSummaryScreen(navController: NavController, appViewModel: AppViewModel) {
    val scanResults = appViewModel.currentScanResults
    val overallRisk = appViewModel.overallRiskLevel
    val riskScore = appViewModel.overallRiskScore
    val roomName = appViewModel.selectedRoom

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            Text(
                text = "Scan Complete",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
            Text(
                text = "Your $roomName has been analyzed",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Overall Risk Level", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = overallRisk.uppercase(),
                        color = when (overallRisk) {
                            "high" -> Color.Red
                            "medium" -> Color(0xFFFFA500)
                            else -> Color.Green
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Risk Score: $riskScore / 100", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Detected Hazards", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val uniqueHazards = scanResults
                    .groupBy { it.label.substringBefore(" (").trim() }
                    .map { it.value.first() }

                items(uniqueHazards) { hazard ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(hazard.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Risk: ${hazard.risk.name}",
                                color = when (hazard.risk) {
                                    Risk.HIGH -> Color.Red
                                    Risk.MEDIUM -> Color(0xFFFFA500)
                                    Risk.SAFE -> Color.Green
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Hazard Button
            Button(
                onClick = {
                    appViewModel.saveCurrentScan()
                    navController.navigate(Screen.RoomHistory.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
            ) {
                Text("Save Hazard")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {navController.navigate(Screen.Recommendation.route)},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Recommendations")
            }
        }
    }
}