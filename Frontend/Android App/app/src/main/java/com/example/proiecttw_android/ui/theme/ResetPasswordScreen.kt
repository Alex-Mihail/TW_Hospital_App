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
import com.example.proiecttw_android.data.api.ApiClient
import com.example.proiecttw_android.data.models.ResetPasswordRequest
import com.example.proiecttw_android.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResetPasswordScreen(navController: NavController) {
    var identifier by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var newPw2 by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    fun validateInputs(): Boolean {
        err = ""
        ok = ""

        val id = identifier.trim()
        val pw = newPw

        if (id.isBlank()) {
            err = "Completează username/email."
            return false
        }
        if (pw.trim().isEmpty()) {
            err = "Completează parola nouă."
            return false
        }
        if (pw.trim().length < 6) {
            err = "Parola trebuie să aibă minim 6 caractere."
            return false
        }
        if (newPw != newPw2) {
            err = "Parolele nu coincid."
            return false
        }
        return true
    }

    suspend fun resetOnPatientOrDoctor(id: String, pw: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val req = ResetPasswordRequest(identifier = id, newPassword = pw)

                // 1) pacient
                var res = ApiClient.authApi.resetPasswordPatient(req)

                // 2) dacă nu există pacient -> doctor
                if (res.code() == 404) {
                    res = ApiClient.authApi.resetPasswordDoctor(req)
                }

                if (!res.isSuccessful) {
                    val txt = res.errorBody()?.string().orEmpty().trim()
                    return@withContext Result.failure(
                        IllegalStateException(txt.ifBlank { "Eroare la resetare (${res.code()})." })
                    )
                }

                val msg = res.body()?.string().orEmpty().trim()
                Result.success(msg.ifBlank { "Parola a fost actualizată. Te poți autentifica acum." })
            } catch (_: Exception) {
                Result.failure(IllegalStateException("Backend indisponibil / eroare rețea."))
            }
        }
    }

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
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("← Înapoi la login", color = Color.White)
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
                    "Resetare parolă",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Introdu username/email și setează o parolă nouă.",
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
                        "Schimbă parola",
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
                        singleLine = true,
                        placeholder = { Text("ex: ion.popescu / ion@email.com") }
                    )

                    Text("Parolă nouă", color = AppColors.Primary, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = newPw,
                        onValueChange = { newPw = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        placeholder = { Text("Minim 6 caractere") },
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

                    Text("Confirmă parola", color = AppColors.Primary, fontWeight = FontWeight.Medium)
                    OutlinedTextField(
                        value = newPw2,
                        onValueChange = { newPw2 = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    if (err.isNotBlank()) ErrorBox(err)
                    if (ok.isNotBlank()) OkBox(ok)

                    Button(
                        onClick = {
                            if (!validateInputs()) return@Button

                            busy = true
                            scope.launch {
                                val res = resetOnPatientOrDoctor(identifier.trim(), newPw)
                                busy = false

                                if (res.isSuccess) {
                                    ok = res.getOrNull().orEmpty()
                                    delay(800)
                                    navController.navigate("login") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    err = res.exceptionOrNull()?.message ?: "Eroare necunoscută."
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (busy) "Se salvează..." else "Actualizează parola", fontWeight = FontWeight.SemiBold)
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

@Composable
private fun OkBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE9FFF0))
            .padding(12.dp)
    ) {
        Text(message, color = Color(0xFF0B5A2A))
    }
}
