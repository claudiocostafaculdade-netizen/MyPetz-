package com.example.ui

import android.content.Context
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: PetViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var screenState by remember { mutableStateOf("LOGIN") } // LOGIN, SIGNUP, FORGOT_PASSWORD
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Text(
                text = "Agenda Pet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = when(screenState) {
                            "SIGNUP" -> "Criar Conta"
                            "FORGOT_PASSWORD" -> "Recuperar Senha"
                            else -> "Bem-vindo(a)!"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (errorMessage != null) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center)
                        }
                    }

                    if (successMessage != null) {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                            Text(successMessage!!, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center)
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    } else {
                        // FORM FIELDS
                        if (screenState == "SIGNUP") {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nome Completo") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (screenState != "FORGOT_PASSWORD") {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Senha") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (screenState == "SIGNUP") {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirmar Senha") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // PRIMARY ACTION BUTTON
                        Button(
                            onClick = {
                                scope.launch {
                                    errorMessage = null
                                    successMessage = null
                                    if (email.isBlank()) { errorMessage = "Preencha o e-mail"; return@launch }
                                    
                                    isLoading = true
                                    when(screenState) {
                                        "LOGIN" -> {
                                            viewModel.loginWithEmail(email, password) { error ->
                                                errorMessage = error
                                                isLoading = false
                                            }
                                        }
                                        "SIGNUP" -> {
                                            if (password != confirmPassword) {
                                                errorMessage = "As senhas não coincidem"
                                                isLoading = false
                                            } else {
                                                viewModel.registerWithEmail(name, email, password) { error ->
                                                    errorMessage = error
                                                    isLoading = false
                                                }
                                            }
                                        }
                                        "FORGOT_PASSWORD" -> {
                                            viewModel.resetPassword(email) { error ->
                                                if (error == null) {
                                                    successMessage = "E-mail de recuperação enviado!"
                                                    screenState = "LOGIN"
                                                } else {
                                                    errorMessage = error
                                                }
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(when(screenState) {
                                "SIGNUP" -> "Cadastrar"
                                "FORGOT_PASSWORD" -> "Enviar E-mail"
                                else -> "Entrar"
                            }, fontWeight = FontWeight.Bold)
                        }

                        if (screenState == "LOGIN") {
                            TextButton(onClick = { screenState = "FORGOT_PASSWORD" }) {
                                Text("Esqueceu a senha?", style = MaterialTheme.typography.bodySmall)
                            }

                            // Google Login Button
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            performGoogleSignIn(context, viewModel) { error ->
                                                errorMessage = error
                                                isLoading = false
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Erro no Google Sign-In: ${e.localizedMessage}"
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Login, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Entrar com Google")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Não tem uma conta?", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { screenState = "SIGNUP" }) {
                                    Text("Cadastre-se", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            TextButton(onClick = { screenState = "LOGIN" }) {
                                Text("Voltar para o Login", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun performGoogleSignIn(context: Context, viewModel: PetViewModel, onError: (String) -> Unit) {
    if (!com.example.data.AuthManager(context).isFirebaseAvailable()) {
        onError("Configuração do Firebase ausente.")
        return
    }

    val credentialManager = CredentialManager.create(context)
    val serverClientId = "19599239851-fole8dbmgljpshns9plth31a50k39ehg.apps.googleusercontent.com"
    
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        
        // Audit Fix: Use the official way to parse the Google ID Token from CustomCredential or direct class
        if (credential is GoogleIdTokenCredential) {
            viewModel.onGoogleSignInSuccess(credential.idToken)
        } else if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.onGoogleSignInSuccess(googleIdTokenCredential.idToken)
        } else {
            Log.e("LoginScreen", "Unsupported credential type: ${credential.type}")
            onError("Credencial não suportada. Tipo: ${credential.type}")
        }
    } catch (e: Exception) {
        Log.e("LoginScreen", "Credential Manager Exception", e)
        onError("Falha ao entrar com Google: ${e.localizedMessage}")
    }
}
