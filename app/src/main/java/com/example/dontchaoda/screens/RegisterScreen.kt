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
fun RegisterScreen(
    onRegister: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onGoToLogin: () -> Unit,
    onGoToHome: () -> Unit
) {
    var showTerms by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Register", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
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
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = confirm,
                onValueChange = {
                    confirm = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            PrimaryButton(
                text = if (isLoading) "Creating Account..." else "Create Account",
                onClick = {
                    when {
                        email.trim().isBlank() -> error = "Email cannot be empty"
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                            error = "Please enter a valid email address"
                        password.trim().isBlank() -> error = "Password cannot be empty"
                        password.length < 6 -> error = "Password must be at least 6 characters"
                        password != confirm -> error = "Passwords do not match"
                        else -> {
                            showTerms = true
                        }
                    }
                },
                enabled = !isLoading
            )

            TextButton(
                onClick = onGoToLogin,
                enabled = !isLoading
            ) {
                Text("Already have an account? Login")
            }

            if (showTerms) {
                TermsPopup(
                    onAccept = {
                        isLoading = true
                        onRegister(email, password) { ok, message ->
                            isLoading = false
                            if (ok) {
                                showTerms = false
                            } else {
                                error = message ?: "Registration failed"
                            }
                        }
                    },
                    onDismiss = {
                        if (!isLoading) {
                            showTerms = false
                        }
                    }
                )
            }
        }
    }
}
