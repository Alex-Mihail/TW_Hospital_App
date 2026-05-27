package com.example.proiecttw_android.ui.doctor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.proiecttw_android.R
import com.example.proiecttw_android.data.api.ApiClient
import com.example.proiecttw_android.ui.theme.AppColors

@Composable
fun DoctorDetailsScreen(
    navController: NavController,
    doctorId: Long
) {
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }

    var profileMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(doctorId) {
        loading = true
        err = ""
        profileMap = emptyMap()
        displayName = ""

        try {
            val res = ApiClient.userApi.getDoctorAccount(doctorId)

            if (res.code() == 404) err = "Doctorul nu a fost găsit în backend (404)."
            else if (!res.isSuccessful) err = "Eroare backend: ${res.code()}"
            else {
                val d = res.body()
                if (d == null) err = "Răspuns invalid (body lipsă)."
                else {
                    displayName = (("${d.firstName.orEmpty()} ${d.lastName.orEmpty()}").trim())
                        .ifBlank { d.username.orEmpty().ifBlank { "Doctor" } }

                    val spec = d.specialization?.name
                        ?: d.specialization?.title
                        ?: d.specialization?.id?.toString()
                        ?: "-"

                    profileMap = linkedMapOf(
                        "ID" to (d.id?.toString() ?: "-"),
                        "Username" to (d.username ?: "-"),
                        "Prenume" to (d.firstName ?: "-"),
                        "Nume" to (d.lastName ?: "-"),
                        "Email" to (d.email ?: "-"),
                        "Specializare" to spec
                    )
                }
            }
        } catch (_: Exception) {
            err = "Backend indisponibil / eroare rețea."
        } finally {
            loading = false
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
                onClick = { navController.popBackStack() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text("← Înapoi", color = Color.White)
            }

            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(20.dp))
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
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Detalii medic",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vezi informațiile medicului și programează o consultație.",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    textAlign = TextAlign.Center
                )
            }
        }

        // CARD WRAPPER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-44).dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 820.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Header
                    Text(
                        text = if (loading) "Se încarcă..." else displayName.ifBlank { "Doctor" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    if (err.isNotBlank()) {
                        ErrorBox(err)
                    }

                    if (!loading && err.isBlank() && profileMap.isEmpty()) {
                        InfoNote("Nu există date pentru acest medic.")
                    }

                    if (!loading && err.isBlank() && profileMap.isNotEmpty()) {
                        InfoGrid(profileMap)
                    }

                    if (loading) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(Modifier.height(18.dp))

                    // Butoane jos (react-like): verde + albastru
                    Button(
                        onClick = {  navController.navigate("booking/$doctorId") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0AA862),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Programează-te", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { navController.navigate("consultation") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Înapoi la consultații", fontWeight = FontWeight.Bold, color = AppColors.Primary)
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

/* ==== UI helpers ==== */

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
private fun InfoNote(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F9FC))
            .padding(12.dp)
    ) {
        Text(text, color = Color(0xFF445566))
    }
}

@Composable
private fun InfoGrid(items: Map<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { (label, value) ->
            InfoBox(
                label = label,
                value = value,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFBFDFF))
            .padding(12.dp)
    ) {
        Text(
            label,
            color = AppColors.Primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(6.dp))
        Text(value.ifBlank { "-" }, color = Color(0xFF334455))
    }
}
