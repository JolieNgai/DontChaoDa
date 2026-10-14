package com.example.dontchaoda.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQScreen(navController: NavController) {
    val faqs = listOf(
        FAQData(
            "How do I start a scan?",
            "Go to the Home screen and tap the large 'Start Scan' button. Point your camera at electrical outlets or appliances to detect potential fire hazards."
        ),
        FAQData(
            "What does the scan actually detect?",
            "The scan detects common household hazards such as fire risks, frayed wires, flammable materials, and obstructions."
        ),
        FAQData(
            "What do the risk levels (Low, Medium, High) mean?",
            """
                • Low (Green): No significant hazards detected
                • Medium (Yellow): Some potential risks found
                • High (Red): Serious hazards detected that require attention
            """.trimIndent()
        ),
        FAQData(
            "How is the risk score calculated?",
            """
                The score is based on the types and severity of hazards detected during the scan, with higher-risk hazards contributing more to the total score.
                
                • High Risk (Red): 70 - 100
                • Medium Risk (Yellow): 40 - 69
                • Low Risk (Green): Below 40
            """.trimIndent()
        ),
        FAQData(
            "How do I fix a detected hazard?",
            "In the Room History page, click on an active hazard. Follow the safety recommendations provided to resolve the issue."
        ),
        FAQData(
            "Can I add or rename rooms?",
            """
                Yes, you can manage rooms from the room selection screen:
                
                • To add: Click the '+ Add New Room' button at the bottom right.
                • To rename: Long press on any room name.
            """.trimIndent()
        ),
        FAQData(
            "Do I need to scan every room?",
            "It is recommended to scan all rooms regularly to ensure overall safety."
        ),
        FAQData(
            "Why is the camera not working?",
            "Please ensure camera permissions are enabled in your device settings."
        )
    )

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                        )
                    )
            ) {
                TopAppBar(
                    title = { Text("FAQ", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(faqs) { faq ->
                FAQItem(faq)
            }
        }
    }
}

data class FAQData(val question: String, val answer: String)

@Composable
fun FAQItem(faq: FAQData) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .animateContentSize()
            .clickable { expanded = !expanded },
        color = Color(0xFFF8F9FA),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.Gray
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
