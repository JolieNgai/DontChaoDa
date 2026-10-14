package com.example.dontchaoda.screens

import android.util.Patterns
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.dontchaoda.ui.theme.PrimaryButton

@Composable
fun LoginScreen(
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onForgotPassword: (String, (Boolean, String?) -> Unit) -> Unit,
    onGoToRegister: () -> Unit,
    onGoToHome: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var popupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onGoToHome,
            enabled = !isLoading,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Home"
            )
        }

        if (popupMessage != null) {
            AlertDialog(
                onDismissRequest = { popupMessage = null },
                confirmButton = {
                    TextButton(onClick = { popupMessage = null }) {
                        Text("OK")
                    }
                },
                title = { Text("Password Reset") },
                text = { Text(popupMessage!!) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Login", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
                    message = null
                    popupMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                enabled = !isLoading
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                    message = null
                    popupMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            if (message != null) {
                Text(message!!, color = MaterialTheme.colorScheme.primary)
            }

            PrimaryButton(
                text = if (isLoading) "Logging in..." else "Login",
                onClick = {
                    when {
                        email.trim().isBlank() -> error = "Email cannot be empty"
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                            error = "Please enter a valid email address"
                        password.trim().isBlank() -> error = "Password cannot be empty"
                        else -> {
                            isLoading = true
                            onLogin(email, password) { ok, loginMessage ->
                                isLoading = false
                                if (!ok) {
                                    error = loginMessage ?: "Invalid email or password"
                                } else {
                                    message = null
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading
            )

            TextButton(
                onClick = {
                    when {
                        email.trim().isBlank() -> error = "Enter your email first"
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                            error = "Please enter a valid email address"
                        else -> {
                            isLoading = true
                            error = null
                            onForgotPassword(email) { ok, resetMessage ->
                                isLoading = false
                                if (ok) {
                                    message = resetMessage ?: "Password reset email sent"
                                } else {
                                    val displayMessage = resetMessage ?: "Could not send reset email"
                                    if (displayMessage.contains("not registered", ignoreCase = true)) {
                                        popupMessage = displayMessage
                                        error = null
                                    } else {
                                        error = displayMessage
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            TextButton(
                onClick = onGoToRegister,
                enabled = !isLoading
            ) {
                Text("No account? Register here!")
            }
        }
    }
}
