package com.example.dontchaoda.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dontchaoda.ui.theme.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .background(
                brush = Brush.horizontalGradient(listOf(BrandOrange, BrandPinkRed)),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = CardSurface,
            fontSize = 16.sp
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CardSurface,
            contentColor = BrandOrange
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(listOf(BrandOrange, BrandPinkRed))
        )
    ) {
        Text(text, fontSize = 15.sp)
    }
}
