package com.example.dontchaoda.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dontchaoda.ui.theme.*

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Scaffold(containerColor = AppBackground) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Logo (gradient square + tick icon)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(10.dp, RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.verticalGradient(listOf(BrandOrange, BrandPinkRed)),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Logo Tick",
                        tint = CardSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "DontChaoDa",
                    fontSize = 28.sp,
                    color = TitleColor
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Scan your room. Reduce fire risks.",
                    fontSize = 14.sp,
                    color = BodyGrey
                )

                Spacer(Modifier.height(28.dp))

                // Feature icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureItem(title = "Scan", icon = Icons.Filled.CameraAlt)
                    FeatureItem(title = "Detect", icon = Icons.Filled.Visibility)
                    FeatureItem(title = "Alert", icon = Icons.Filled.Notifications)
                }

                Spacer(Modifier.height(34.dp))

                // Login button
                PrimaryButton(
                    text = "Login",
                    onClick = onLoginClick
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Register button
                SecondaryButton(
                    text = "Register",
                    onClick = onRegisterClick
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(SoftOrangeBg, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BrandOrange,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            color = LabelGrey
        )
    }
}