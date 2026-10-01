package com.alfleyla.zeituna.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfleyla.zeituna.auth.AuthViewModel
import com.alfleyla.zeituna.utils.platformGetCurrentUrl

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.h4)
        
        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Text(error!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onNavigateToRegister, enabled = !isLoading) {
                Text("Register")
            }
            TextButton(onClick = onNavigateToForgotPassword, enabled = !isLoading) {
                Text("Forgot Password?")
            }
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.registrationSuccess.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Register", style = MaterialTheme.typography.h4)

        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Text(error!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (success == true) {
            Text(
                "Registration successful! Please check your email to confirm your account.",
                color = Color(0xFF4CAF50),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Login")
            }
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { 
                        // Enhanced Base URL detection
                        val currentUrl = platformGetCurrentUrl().split("?")[0].split("#")[0]
                        val baseUrl = if (currentUrl.endsWith(".html")) {
                            currentUrl.substringBeforeLast("/")
                        } else {
                            currentUrl.removeSuffix("/")
                        }
                        val redirectUrl = "https://zeitun-a.com/email_confirmation.html"
                        viewModel.register(email, password, name, redirectUrl) 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Register")
                }
            }

            TextButton(onClick = onNavigateToLogin, enabled = !isLoading) {
                Text("Already have an account? Login")
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val resetSent by viewModel.resetPasswordSent.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Reset Password", style = MaterialTheme.typography.h4)

        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Text(error!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (resetSent == true) {
            Text(
                "Check your email for a password reset link.",
                color = Color(0xFF4CAF50),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Login")
            }
        } else {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { 
                        // Enhanced Base URL detection
                        val currentUrl = platformGetCurrentUrl().split("?")[0].split("#")[0]
                        val baseUrl = if (currentUrl.endsWith(".html")) {
                            currentUrl.substringBeforeLast("/")
                        } else {
                            currentUrl.removeSuffix("/")
                        }
                        val redirectUrl = "https://zeitun-a.com/reset-password.html"
                        viewModel.sendResetPasswordEmail(email, redirectUrl) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank()
                ) {
                    Text("Send Reset Link")
                }
            }

            TextButton(onClick = onNavigateToLogin, enabled = !isLoading) {
                Text("Back to Login")
            }
        }
    }
}
