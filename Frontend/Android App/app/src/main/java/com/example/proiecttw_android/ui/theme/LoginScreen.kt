package com.example.proiecttw_android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.proiecttw_android.R
import com.example.proiecttw_android.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    onLogin: suspend (identifier: String, password: String) -> Result<Unit>
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
            .verticalScroll(rememberScrollState())
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.TopBar)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text("← Înapoi acasă", color = Color.White)
            }

            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("0740 123 456", color = Color.White)
        }

        // HERO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.homepage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.TopBar.copy(alpha = 0.70f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 35.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Autentificare",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Accesează programările și contul tău",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-28).dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Bine ai revenit",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Text("Username / Email", color = AppColors.Primary, fontWeight = FontWeight.Medium)

                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Text("Parolă", color = AppColors.Primary, fontWeight = FontWeight.Medium)

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showPw) "🙈" else "👁️",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showPw = !showPw }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "Am uitat parola",
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.navigate("reset-password") }
                        )
                    }

                    if (error.isNotBlank()) ErrorBox(error)

                    Button(
                        onClick = {
                            error = ""
                            val id = identifier.trim()
                            val pw = password.trim()

                            if (id.isBlank() || pw.isBlank()) {
                                error = "Completează username/email și parola."
                                return@Button
                            }

                            loading = true
                            scope.launch {
                                val res = try { onLogin(id, pw) } catch (e: Exception) { Result.failure(e) }
                                loading = false

                                if (res.isSuccess) {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                } else {
                                    error = res.exceptionOrNull()?.message ?: "Backend indisponibil."
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (loading) "Se autentifică..." else "Autentificare", fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Nu ai cont? ", color = Color(0xFF556677))
                        Text(
                            "Creează cont",
                            color = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { navController.navigate("signup") }
                        )
                    }
                }
            }
        }

        Text(
            "© 2025 Spitalul Central TW",
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            color = AppColors.Primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFE8E8))
            .padding(12.dp)
    ) {
        Text(message, color = Color(0xFF7A1B1B))
    }
}
