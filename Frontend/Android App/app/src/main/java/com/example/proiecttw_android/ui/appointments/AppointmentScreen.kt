package com.example.proiecttw_android.ui.appointments

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
import com.example.proiecttw_android.data.api.AppointmentDto
import com.example.proiecttw_android.data.api.UpdateAppointmentStatusRequest
import com.example.proiecttw_android.ui.UserUi
import com.example.proiecttw_android.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AppointmentsScreen(
    navController: NavController,
    user: UserUi?
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf("") }
    var list by remember { mutableStateOf<List<AppointmentDto>>(emptyList()) }
    var busyId by remember { mutableStateOf<Long?>(null) }

    val topBlue = AppColors.TopBar
    val blue = AppColors.Primary
    val green = Color(0xFF0AA862)
    val red = Color(0xFFB42318)

    // guard
    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate("login") { popUpTo("home") }
        }
    }

    fun reload() {
        val u = user ?: return
        scope.launch {
            loading = true
            err = ""
            try {
                val role = u.role.uppercase()
                val resp = when (role) {
                    "PATIENT" -> ApiClient.appointmentApi.byPatient(u.id)
                    "DOCTOR" -> ApiClient.appointmentApi.byDoctor(u.id)
                    else -> null
                }

                if (resp == null) {
                    err = "Rolul ${u.role} nu are programări."
                    list = emptyList()
                } else if (!resp.isSuccessful) {
                    err = "Eroare backend: ${resp.code()}"
                    list = emptyList()
                } else {
                    list = resp.body().orEmpty()
                }
            } catch (_: Exception) {
                err = "Backend indisponibil / eroare rețea."
                list = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(user?.id, user?.role) { reload() }

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
                .background(topBlue)
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
                    .background(topBlue.copy(alpha = 0.70f))
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
                    "Programările mele",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Gestionează programările rapid și ușor.",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    textAlign = TextAlign.Center
                )
            }
        }

        // CARD wrapper
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

                    // Header actions
                    Text(
                        text = "Lista programărilor",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = blue,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { reload() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = blue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(if (loading) "Se reîncarcă..." else "Reîncarcă", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(14.dp))

                    if (loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                    }

                    if (err.isNotBlank()) {
                        ErrorBox(err)
                        Spacer(Modifier.height(10.dp))
                    }

                    if (!loading && err.isBlank() && list.isEmpty()) {
                        InfoNote("Nu există programări de afișat.")
                    }

                    // LIST
                    list.forEach { appt ->
                        Spacer(Modifier.height(12.dp))
                        AppointmentCard(
                            appt = appt,
                            role = user?.role?.uppercase().orEmpty(),
                            blue = blue,
                            green = green,
                            red = red,
                            busy = (busyId != null && busyId == appt.id),
                            onAccept = {
                                val id = appt.id ?: return@AppointmentCard
                                busyId = id
                                scope.launch {
                                    try {
                                        ApiClient.appointmentApi.updateStatus(
                                            id,
                                            UpdateAppointmentStatusRequest("ACCEPTED")
                                        )
                                        reload()
                                    } catch (_: Exception) {
                                        err = "Eroare rețea la ACCEPT."
                                    } finally {
                                        busyId = null
                                    }
                                }
                            },
                            onDeny = {
                                val id = appt.id ?: return@AppointmentCard
                                busyId = id
                                scope.launch {
                                    try {
                                        ApiClient.appointmentApi.updateStatus(
                                            id,
                                            UpdateAppointmentStatusRequest("DENIED")
                                        )
                                        reload()
                                    } catch (_: Exception) {
                                        err = "Eroare rețea la DENY."
                                    } finally {
                                        busyId = null
                                    }
                                }
                            },
                            onCancel = {
                                val id = appt.id ?: return@AppointmentCard
                                busyId = id
                                scope.launch {
                                    try {
                                        ApiClient.appointmentApi.cancel(id)
                                        reload()
                                    } catch (_: Exception) {
                                        err = "Eroare rețea la ANULARE."
                                    } finally {
                                        busyId = null
                                    }
                                }
                            },
                            onDelete = {
                                val id = appt.id ?: return@AppointmentCard
                                busyId = id
                                scope.launch {
                                    try {
                                        ApiClient.appointmentApi.delete(id)
                                        reload()
                                    } catch (_: Exception) {
                                        err = "Eroare rețea la ȘTERGERE."
                                    } finally {
                                        busyId = null
                                    }
                                }
                            }
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
            color = blue,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppointmentCard(
    appt: AppointmentDto,
    role: String,
    blue: Color,
    green: Color,
    red: Color,
    busy: Boolean,
    onAccept: () -> Unit,
    onDeny: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val border = Color(0xFF0F172A).copy(alpha = 0.08f)

    val doctorName = appt.doctor?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim() }.orEmpty()
    val patientName = appt.patient?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim() }.orEmpty()
    val spec = appt.doctor?.specialization?.name ?: appt.doctor?.specialization?.title ?: "-"

    val whenText = formatDateTime(appt.appointmentDatetime)

    val status = (appt.status ?: "PENDING").uppercase()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // header
            Text(
                text = when (role) {
                    "PATIENT" -> "Medic: ${doctorName.ifBlank { "Doctor" }}"
                    "DOCTOR" -> "Pacient: ${patientName.ifBlank { "Pacient" }}"
                    else -> "Programare"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF0F172A)
            )

            Spacer(Modifier.height(6.dp))

            // details
            InfoLine("Data & ora", whenText, blue)
            if (role == "PATIENT") InfoLine("Specializare", spec, blue)
            if (!appt.description.isNullOrBlank()) InfoLine("Descriere", appt.description!!, blue)

            Spacer(Modifier.height(10.dp))

            // status chip
            StatusChip(status = status, blue = blue)

            Spacer(Modifier.height(12.dp))

            // Actions
            when (role) {
                "DOCTOR" -> {
                    // DOCTOR: accept/deny dacă e PENDING
                    val canDecide = status == "PENDING"

                    Button(
                        onClick = onAccept,
                        enabled = canDecide && !busy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) { Text(if (busy) "Se procesează..." else "Acceptă", fontWeight = FontWeight.Bold) }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onDeny,
                        enabled = canDecide && !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, red.copy(alpha = 0.7f))
                    ) { Text("Respinge", fontWeight = FontWeight.Bold) }
                }

                "PATIENT" -> {
                    // PATIENT: cancel if PENDING/ACCEPTED; delete if DENIED/CANCELLED/FINISHED
                    val canCancel = status == "PENDING" || status == "ACCEPTED"
                    val canDelete = status == "DENIED" || status == "CANCELLED" || status == "FINISHED"

                    if (canCancel) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = red),
                            border = androidx.compose.foundation.BorderStroke(1.dp, red.copy(alpha = 0.7f))
                        ) { Text(if (busy) "Se procesează..." else "Anulează", fontWeight = FontWeight.Bold) }

                        Spacer(Modifier.height(10.dp))
                    }

                    if (canDelete) {
                        Button(
                            onClick = onDelete,
                            enabled = !busy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = red, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) { Text(if (busy) "Se procesează..." else "Șterge", fontWeight = FontWeight.Bold) }
                    }

                    if (!canCancel && !canDelete) {
                        InfoNote("Nu există acțiuni disponibile pentru acest status.")
                    }
                }

                else -> {
                    InfoNote("Rol necunoscut — nu există acțiuni.")
                }
            }
        }
    }
}

/* ===== UI helpers ===== */

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
private fun InfoLine(label: String, value: String, blue: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = blue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(value.ifBlank { "-" }, color = Color(0xFF334455), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun StatusChip(status: String, blue: Color) {
    val bg = when (status) {
        "ACCEPTED" -> Color(0xFF0AA862).copy(alpha = 0.12f)
        "DENIED" -> Color(0xFFB42318).copy(alpha = 0.12f)
        "CANCELLED" -> Color(0xFFB42318).copy(alpha = 0.10f)
        "FINISHED" -> blue.copy(alpha = 0.10f)
        else -> blue.copy(alpha = 0.08f) // PENDING
    }
    val fg = when (status) {
        "ACCEPTED" -> Color(0xFF0B5A2A)
        "DENIED" -> Color(0xFF7A1B1B)
        "CANCELLED" -> Color(0xFF7A1B1B)
        "FINISHED" -> blue
        else -> blue
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text("Status: $status", color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatDateTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    return try {
        // backend: LocalDateTime -> de obicei "2026-01-02T10:00:00"
        val dt = LocalDateTime.parse(raw)
        val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy • HH:mm")
        dt.format(fmt)
    } catch (_: Exception) {
        raw
    }
}
