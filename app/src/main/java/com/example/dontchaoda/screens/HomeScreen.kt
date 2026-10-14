package com.example.dontchaoda.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dontchaoda.navigation.Screen
import com.example.dontchaoda.viewmodel.AppViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    onStartScan: () -> Unit,
    appViewModel: AppViewModel,
    onLogout: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = { BottomNavBar(navController) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // --- Header Row ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Welcome,",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E)
                        )
                        Text(
                            text = "Stay safe, stay protected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                    
                    // Notification bell
                    Box {
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.Reminders.route)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Reminders"
                            )
                        }
                        if (appViewModel.reminderCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(16.dp)
                                    .background(Color.Red, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = appViewModel.reminderCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Logout Button
                    Surface(
                        onClick = onLogout,
                        color = Color(0xFFF1F3F4),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logout", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- Start Scan Button ---
                Button(
                    onClick = onStartScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF5722), Color(0xFFD50000))
                                ),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Start Scan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // --- Your Rooms Section ---
                Text(
                    text = "Your Rooms",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val rooms = appViewModel.rooms
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(rooms) { roomName ->
                        val scans = appViewModel.roomScans[roomName]
                        val latestScan = scans?.firstOrNull()
                        
                        val riskLevel = when {
                            latestScan == null -> "No Scan"
                            latestScan.riskscore >= 70 -> "High"
                            latestScan.riskscore >= 40 -> "Medium"
                            else -> "Low"
                        }
                        
                        val riskBg = when (riskLevel) {
                            "High" -> Color(0xFFFFEBEE)
                            "Medium" -> Color(0xFFFFF8E1)
                            "Low" -> Color(0xFFE8F5E9)
                            else -> Color(0xFFF1F3F4)
                        }
                        
                        val riskText = when (riskLevel) {
                            "High" -> Color(0xFFC62828)
                            "Medium" -> Color(0xFFF57F17)
                            "Low" -> Color(0xFF2E7D32)
                            else -> Color.Gray
                        }

                        val room = RoomInfo(
                            name = roomName,
                            lastScan = latestScan?.date ?: "Never",
                            riskLevel = riskLevel,
                            riskBg = riskBg,
                            riskText = riskText
                        )
                        
                        RoomCard(room) {
                            appViewModel.selectedRoom = roomName
                            navController.navigate(Screen.RoomHistory.route)
                        }
                    }
                }
            }
        }

        // --- Tutorial Overlay Logic ---
        if (appViewModel.showTutorial) {
            HomeTutorialOverlay(onDismiss = { appViewModel.showTutorial = false })
        }
    }
}

@Composable
fun HomeTutorialOverlay(onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(enabled = false) { }
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                1 -> HomeTutorialStep(
                    icon = Icons.Default.Home,
                    title = "Dashboard",
                    desc = "This is your home screen. You can see your rooms and their safety status here."
                )
                2 -> HomeTutorialStep(
                    icon = Icons.Default.CameraAlt,
                    title = "Start Scanning",
                    desc = "Tap 'Start Scan' to use your camera. Point it at potential hazards like loose cables or overloaded plugs."
                )
                3 -> HomeTutorialStep(
                    icon = Icons.Default.Warning,
                    title = "Stay Safe",
                    desc = "If a fire hazard is detected, a red warning box will appear. Take action immediately!"
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { if (step < 3) step++ else onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (step < 3) "Next" else "Get Started", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HomeTutorialStep(icon: ImageVector, title: String, desc: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(desc, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, lineHeight = 24.sp)
    }
}

data class RoomInfo(
    val name: String,
    val lastScan: String,
    val riskLevel: String,
    val riskBg: Color,
    val riskText: Color
)

@Composable
fun RoomCard(room: RoomInfo, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8F9FA)
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(room.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    // Risk Badge
                    Surface(
                        color = room.riskBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = room.riskLevel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = room.riskText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text("Last scan: ${room.lastScan}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}
