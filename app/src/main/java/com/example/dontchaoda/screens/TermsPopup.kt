package com.example.dontchaoda.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

@Composable
fun TermsPopup(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    var agreed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Terms & Conditions")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 550.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = """
                        Terms & Conditions
                    
                        • This app provides fire hazard detection based on image scanning and analysis.
                        • The results are generated using automated detection and may not always be accurate or complete.
                        • This app is intended for informational purposes only and does not replace professional safety inspections.
                        • Users are responsible for verifying hazards and taking appropriate safety precautions.
                        • We are not liable for any damage, injury, or loss resulting from reliance on this app.
                        • Always follow official fire safety guidelines and emergency procedures.
                        
                        By continuing, you agree to these terms.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Checkbox row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { agreed = !agreed }
                ) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = { agreed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.Black,
                            uncheckedColor = Color.Black,
                            checkmarkColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("I agree")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = agreed
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}