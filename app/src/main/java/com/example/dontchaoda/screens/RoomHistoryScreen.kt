package com.example.dontchaoda.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dontchaoda.navigation.Screen
import com.example.dontchaoda.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomHistoryScreen(
    navController: NavController,
    appViewModel: AppViewModel
) {
    val roomName = appViewModel.selectedRoom
    val scans = appViewModel.roomScans[roomName] ?: emptyList()
    val latestScan = scans.firstOrNull()
    val fixedHazards = appViewModel.roomFixedHazards[roomName] ?: emptyList()
    
    var hazardToFix by remember { mutableStateOf<String?>(null) }
    
    val riskLevel = when {
        latestScan == null -> "N/A"
        latestScan.riskscore >= 70 -> "High"
        latestScan.riskscore >= 40 -> "Medium"
        else -> "Low"
    }
    
    val riskScore = latestScan?.riskscore ?: 0
    
    val riskColor = when (riskLevel) {
        "High" -> Color(0xFFFFEBEE)
        "Medium" -> Color(0xFFFFF8E1)
        "Low" -> Color(0xFFE8F5E9)
        else -> Color.LightGray
    }
    
    val riskTextColor = when (riskLevel) {
        "High" -> Color(0xFFC62828)
        "Medium" -> Color(0xFFF57F17)
        "Low" -> Color(0xFF2E7D32)
        else -> Color.Black
    }

    val orangeColor = Color(0xFFFF5722)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roomName) },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Modified to always go back to Home screen
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Overview Card with dynamic fill color and latest risk score
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = riskColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = riskTextColor)
                                Surface(
                                    color = Color.LightGray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Score: $riskScore",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = riskTextColor
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                OverviewItem("Last Scanned", latestScan?.date ?: "Never", riskTextColor)
                                OverviewItem("Risk Level", riskLevel, riskTextColor)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Color coded stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatBox("Total Scans", scans.size.toString(), Color(0xFFE3F2FD), Color(0xFF1976D2), Modifier.weight(1f))
                                StatBox("Fixed", fixedHazards.size.toString(), Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                                StatBox("Active", (latestScan?.hazards?.size ?: 0).toString(), Color(0xFFFFEBEE), Color(0xFFC62828), Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Text("Current Active Hazards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (latestScan != null && latestScan.hazards.isNotEmpty()) {
                    items(latestScan.hazards) { hazard ->
                        HazardDetailCard(
                            name = hazard,
                            status = "Active",
                            bgColor = Color(0xFFFFEBEE),
                            textColor = Color(0xFFC62828),
                            onClick = { hazardToFix = hazard }
                        )
                    }
                } else {
                    item { Text("No active hazards.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                }

                item {
                    Text("Fixed Hazards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (fixedHazards.isNotEmpty()) {
                    items(fixedHazards) { hazard ->
                        HazardDetailCard(
                            name = hazard,
                            status = "Fixed",
                            bgColor = Color(0xFFE8F5E9),
                            textColor = Color(0xFF2E7D32)
                        )
                    }
                } else {
                    item { Text("No fixed hazards.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                }

                item {
                    Text("Scan History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (scans.isNotEmpty()) {
                    items(scans) { scan ->
                        HistoryItemCard(ScanHistoryItem(scan.date, scan.riskscore))
                    }
                } else {
                    item { Text("No scan history available.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom buttons
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.ARScan.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = orangeColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("Scan Again")
                }
                OutlinedButton(
                    onClick = {navController.navigate(Screen.Recommendation.route)},
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, orangeColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = orangeColor
                    )
                ) {
                    Text("View Recommendations")
                }
            }
        }

        // Complete Hazard Dialog
        if (hazardToFix != null) {
            AlertDialog(
                onDismissRequest = { hazardToFix = null },
                title = { Text("Mark Hazard as Fixed") },
                text = { Text("Has \"${hazardToFix}\" been resolved?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            appViewModel.fixHazard(roomName, hazardToFix!!)
                            hazardToFix = null
                        }
                    ) {
                        Text("Complete", color = orangeColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { hazardToFix = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun OverviewItem(label: String, value: String, textColor: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
fun StatBox(label: String, value: String, bgColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}

@Composable
fun HazardDetailCard(
    name: String,
    status: String,
    bgColor: Color,
    textColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class ScanHistoryItem(val date: String, val score: Int)

@Composable
fun HistoryItemCard(item: ScanHistoryItem) {
    val (riskLevel, riskColor) = when {
        item.score >= 70 -> "High Risk" to Color(0xFFC62828)
        item.score >= 40 -> "Medium Risk" to Color(0xFFF57F17)
        else -> "Low Risk" to Color(0xFF2E7D32)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(riskLevel, style = MaterialTheme.typography.bodySmall, color = riskColor, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Score", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(
                    text = item.score.toString(),
                    color = riskColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
