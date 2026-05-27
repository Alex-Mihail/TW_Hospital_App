package com.example.proiecttw_android.ui

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
import com.example.proiecttw_android.data.api.*
import com.example.proiecttw_android.data.datastore.SessionStore
import com.example.proiecttw_android.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    navController: NavController,
    user: UserUi?,
    sessionStore: SessionStore
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var deleteBusy by remember { mutableStateOf(false) }

    var profileMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var displayName by remember { mutableStateOf("") }
    var roleLabel by remember { mutableStateOf("") }

    // guard: if not logged in -> login
    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate("login") { popUpTo("home") }
        }
    }

    // fetch profile
    LaunchedEffect(user?.id, user?.role) {
        val u = user ?: return@LaunchedEffect
        loading = true
        err = ""
        profileMap = emptyMap()
        displayName = ""
        roleLabel = u.role.uppercase()

        try {
            when (u.role.uppercase()) {
                "PATIENT" -> {
                    val res = ApiClient.userApi.getPatient(u.id)
                    if (res.code() == 404) err = "Profilul nu a fost găsit în backend (404)."
                    else if (!res.isSuccessful) err = "Eroare backend: ${res.code()}"
                    else {
                        val p = res.body()
                        if (p == null) err = "Răspuns invalid (body lipsă)."
                        else {
                            displayName = (("${p.firstName.orEmpty()} ${p.lastName.orEmpty()}").trim())
                                .ifBlank { p.username.orEmpty().ifBlank { "Cont" } }
                            roleLabel = (p.role ?: "PATIENT").uppercase()

                            profileMap = linkedMapOf(
                                "ID" to (p.id?.toString() ?: "-"),
                                "Username" to (p.username ?: "-"),
                                "Prenume" to (p.firstName ?: "-"),
                                "Nume" to (p.lastName ?: "-"),
                                "Email" to (p.email ?: "-"),
                                "Telefon" to (p.phone ?: "-"),
                                "Data nașterii" to (p.dateOfBirth ?: "-")
                            )
                        }
                    }
                }

                "DOCTOR" -> {
                    val res = ApiClient.userApi.getDoctorAccount(u.id)
                    if (res.code() == 404) err = "Profilul nu a fost găsit în backend (404)."
                    else if (!res.isSuccessful) err = "Eroare backend: ${res.code()}"
                    else {
                        val d = res.body()

                        if (d == null) err = "Răspuns invalid (body lipsă)."
                        else {
                            displayName = (("${d.firstName.orEmpty()} ${d.lastName.orEmpty()}").trim())
                                .ifBlank { d.username.orEmpty().ifBlank { "Cont" } }

                            roleLabel = (d.role ?: "DOCTOR").uppercase()
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
                }
                else -> err = "Rol necunoscut: ${u.role}"
            }
        } catch (_: Exception) {
            err = "Backend indisponibil / eroare rețea."
        } finally {
            loading = false
        }
    }

    val roleUpper = user?.role?.uppercase().orEmpty()
    val showAppointments = roleUpper == "PATIENT" || roleUpper == "DOCTOR"
    val showEdit = roleUpper == "PATIENT" || roleUpper == "DOCTOR"
    val showDelete = roleUpper == "PATIENT"

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
                Text("← Acasă", color = Color.White)
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
                    "Detalii cont",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Gestionează-ți contul simplu și rapid.",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (loading) "Se încarcă..." else displayName.ifBlank { "Cont" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = AppColors.Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "Rol: ",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF556677))
                            )
                            Text(
                                text = roleLabel.ifBlank { "-" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (showAppointments) {
                                OutlinedAction(
                                    text = "Programări",
                                    onClick = { navController.navigate("appointments") }
                                )
                            }
                        }
                    }

                    if (err.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        ErrorBox(err)
                    }

                    if (!loading && err.isBlank() && profileMap.isEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        InfoNote("Nu există date de profil încărcate.")
                    }

                    if (!loading && err.isBlank() && profileMap.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        InfoGrid(profileMap)
                    }

                    Spacer(Modifier.height(18.dp))

                    BottomActionsRow(
                        showEdit = showEdit,
                        showDelete = showDelete,
                        deleteBusy = deleteBusy,
                        onEdit = { navController.navigate("account/edit") },
                        onDelete = {
                            val u = user ?: return@BottomActionsRow
                            if (!showDelete) return@BottomActionsRow

                            deleteBusy = true
                            err = ""

                            scope.launch {
                                try {
                                    val delRes = when (u.role.uppercase()) {
                                        "PATIENT" -> ApiClient.userApi.deletePatient(u.id)
                                        else -> null
                                    }

                                    if (delRes == null) {
                                        err = "Ștergerea nu este permisă pentru rolul ${u.role}."
                                    } else if (delRes.code() == 404) {
                                        err = "Contul nu a fost găsit (404)."
                                    } else if (!delRes.isSuccessful) {
                                        err = "Eroare la ștergere: ${delRes.code()}"
                                    } else {
                                        sessionStore.logout()
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                } catch (_: Exception) {
                                    err = "Backend indisponibil / eroare rețea la ștergere."
                                } finally {
                                    deleteBusy = false
                                }
                            }
                        },
                        onLogout = {
                            scope.launch {
                                sessionStore.logout()
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        }
                    )
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
private fun BottomActionsRow(
    showEdit: Boolean,
    showDelete: Boolean,
    deleteBusy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showEdit) {
            OutlinedButton(
                onClick = onEdit,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Editează cont", fontWeight = FontWeight.Bold, color = AppColors.Primary)
            }
        }

        if (showDelete) {
            Button(
                onClick = onDelete,
                enabled = !deleteBusy,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB42318),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (deleteBusy) "Se șterge..." else "Șterge cont", fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Log out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OutlinedAction(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(42.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = AppColors.Primary)
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
        Text(label, color = AppColors.Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Text(value.ifBlank { "-" }, color = Color(0xFF334455))
    }
}
