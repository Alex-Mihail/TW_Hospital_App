package com.example.proiecttw_android.ui.booking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.proiecttw_android.data.api.CreateAppointmentRequest
import com.example.proiecttw_android.ui.UserUi
import com.example.proiecttw_android.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter

private val HOURS = (8..16).toList()
private const val BUFFER_MIN = 15L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController: NavController,
    user: UserUi?,
    doctorId: Long
) {
    val scope = rememberCoroutineScope()

    // guard: doar PATIENT
    LaunchedEffect(user) {
        val ok = user != null && user.role.uppercase() == "PATIENT"
        if (!ok) {
            navController.navigate("login") { popUpTo("home") }
        }
    }

    val topBlue = AppColors.TopBar
    val blue = AppColors.Primary
    val green = Color(0xFF0AA862)

    var loadingDoctor by remember { mutableStateOf(true) }
    var doctorName by remember { mutableStateOf("Doctor") }
    var specName by remember { mutableStateOf("Specializare") }
    var doctorErr by remember { mutableStateOf("") }

    // date selection
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var datePickerOpen by remember { mutableStateOf(false) }

    // availability
    var loadingSlots by remember { mutableStateOf(false) }
    var busyHours by remember { mutableStateOf(setOf<Int>()) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }

    // description
    var description by remember { mutableStateOf("") }

    // submit
    var submitBusy by remember { mutableStateOf(false) }
    var msgErr by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf("") }

    // ---- helpers ----
    fun isWeekend(d: LocalDate) = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
    fun isPast(d: LocalDate) = d.isBefore(LocalDate.now())
    fun isToday(d: LocalDate) = d == LocalDate.now()

    fun nowHourWithBuffer(): Int {
        val limit = LocalDateTime.now().plusMinutes(BUFFER_MIN)
        return limit.hour
    }

    fun visibleHoursForDate(d: LocalDate): List<Int> {
        if (!isToday(d)) return HOURS
        val nh = nowHourWithBuffer()
        return HOURS.filter { it > nh }
    }

    fun buildDateTimeIso(d: LocalDate, hour: Int): String {
        // backend waits "YYYY-MM-DDT HH:00"
        val dt = LocalDateTime.of(d, LocalTime.of(hour, 0))
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
    }

    suspend fun fetchDoctor() {
        loadingDoctor = true
        doctorErr = ""
        try {
            val res = ApiClient.userApi.getDoctorAccount(doctorId)
            if (!res.isSuccessful) {
                doctorErr = "Eroare doctor: ${res.code()}"
            } else {
                val d = res.body()
                if (d == null) {
                    doctorErr = "Răspuns invalid."
                } else {
                    doctorName = (("${d.firstName.orEmpty()} ${d.lastName.orEmpty()}").trim())
                        .ifBlank { d.username.orEmpty().ifBlank { "Doctor" } }

                    specName = d.specialization?.name
                        ?: d.specialization?.title
                                ?: d.specialization?.id?.toString()
                                ?: "Specializare"
                }
            }
        } catch (_: Exception) {
            doctorErr = "Backend indisponibil."
        } finally {
            loadingDoctor = false
        }
    }

    suspend fun fetchAvailability(d: LocalDate) {
        loadingSlots = true
        msgErr = ""
        try {
            val res = ApiClient.appointmentApi.availability(
                doctorId = doctorId,
                date = d.format(DateTimeFormatter.ISO_DATE) // "YYYY-MM-DD"
            )

            if (!res.isSuccessful) {
                msgErr = "Nu pot încărca disponibilitatea (${res.code()})."
                busyHours = emptySet()
            } else {
                val appts = res.body().orEmpty()
                val set = mutableSetOf<Int>()
                appts.forEach { a ->
                    val raw = a.appointmentDatetime ?: return@forEach
                    val hour = raw.substring(11, 13).toIntOrNull()
                    if (hour != null) set.add(hour)
                }
                busyHours = set
            }
        } catch (_: Exception) {
            msgErr = "Backend indisponibil / eroare rețea."
            busyHours = emptySet()
        } finally {
            loadingSlots = false
        }
    }

    fun validateDateOrExplain(d: LocalDate): Boolean {
        if (isPast(d)) {
            msgErr = "Nu poți selecta o dată din trecut."
            return false
        }
        if (isWeekend(d)) {
            msgErr = "Weekend-ul este indisponibil. Alege o zi lucrătoare."
            return false
        }
        return true
    }

    suspend fun submit() {
        msgErr = ""
        msgOk = ""

        val u = user
        if (u == null || u.role.uppercase() != "PATIENT") {
            msgErr = "Nu ești autentificat ca pacient."
            return
        }
        if (!validateDateOrExplain(selectedDate)) return

        val hour = selectedHour
        if (hour == null) {
            msgErr = "Alege o oră."
            return
        }

        if (isToday(selectedDate)) {
            val nh = nowHourWithBuffer()
            if (hour <= nh) {
                msgErr = "Pentru ziua de azi poți alege doar orele care urmează."
                return
            }
        }

        if (busyHours.contains(hour)) {
            msgErr = "Ora selectată este ocupată."
            return
        }

        val iso = buildDateTimeIso(selectedDate, hour)

        submitBusy = true
        try {
            val res = ApiClient.appointmentApi.create(
                CreateAppointmentRequest(
                    patientId = u.id,
                    doctorId = doctorId,
                    appointmentDatetime = iso,
                    description = description.trim().ifBlank { null }
                )
            )

            if (res.code() == 409) {
                msgErr = "Slot ocupat. Alege altă oră."
                fetchAvailability(selectedDate)
                return
            }

            if (!res.isSuccessful) {
                msgErr = "Eroare la creare (${res.code()})"
                return
            }

            msgOk = "Solicitarea a fost trimisă (PENDING)."
            selectedHour = null
            description = ""
            fetchAvailability(selectedDate)
        } catch (_: Exception) {
            msgErr = "Backend indisponibil / eroare rețea."
        } finally {
            submitBusy = false
        }
    }

    // init
    LaunchedEffect(doctorId) {
        fetchDoctor()
        fetchAvailability(selectedDate)
    }

    // UI
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
            ) { Text("← Înapoi", color = Color.White) }

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
                    "Programare",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Alege data și ora. Sloturile ocupate sunt blocate.",
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

                    Text(
                        text = if (loadingDoctor) "Se încarcă..." else "Dr. $doctorName",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = blue,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = specName,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF556677))
                    )

                    if (doctorErr.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        ErrorBox(doctorErr)
                    }

                    Spacer(Modifier.height(14.dp))

                    // DATA
                    Text("Data", color = blue, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { datePickerOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                            fontWeight = FontWeight.Bold,
                            color = blue
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    InfoNote("Weekend-ul este blocat. Orele ocupate (PENDING/ACCEPTED) sunt blocate.")

                    Spacer(Modifier.height(14.dp))

                    // ORE
                    Text("Alege ora", color = blue, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (loadingSlots) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                    }

                    val visible = visibleHoursForDate(selectedDate)

                    if (isToday(selectedDate) && visible.isEmpty()) {
                        InfoNote("Nu mai sunt ore disponibile astăzi. Alege o altă zi.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp) // ca să nu "mănânce" tot ecranul
                        ) {
                            items(visible) { h ->
                                val busy = busyHours.contains(h)
                                val selected = selectedHour == h

                                val bg = when {
                                    busy -> Color(0xFFB42318).copy(alpha = 0.12f)
                                    selected -> blue.copy(alpha = 0.10f)
                                    else -> green.copy(alpha = 0.12f)
                                }

                                val fg = when {
                                    busy -> Color(0xFF7A1B1B)
                                    selected -> blue
                                    else -> Color(0xFF0B5A2A)
                                }

                                val border = when {
                                    selected -> blue.copy(alpha = 0.55f)
                                    busy -> Color(0xFFB42318).copy(alpha = 0.35f)
                                    else -> green.copy(alpha = 0.35f)
                                }

                                OutlinedButton(
                                    onClick = { selectedHour = h },
                                    enabled = !busy && !loadingSlots,
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, border),
                                    modifier = Modifier.height(46.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = bg,
                                        contentColor = fg
                                    )
                                ) {
                                    Text(String.format("%02d:00", h), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // DESCRIERE
                    Text("Descriere (opțional)", color = blue, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ex: consult de rutină / durere de cap de 3 zile...") },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3
                    )

                    Spacer(Modifier.height(14.dp))

                    if (msgErr.isNotBlank()) ErrorBox(msgErr)
                    if (msgOk.isNotBlank()) SuccessBox(msgOk)

                    Spacer(Modifier.height(16.dp))

                    // FOOTER ACTIONS
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        enabled = !submitBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Anulează", fontWeight = FontWeight.Bold, color = blue)
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { scope.launch { submit() } },
                        enabled = !submitBusy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = green,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (submitBusy) "Se trimite..." else "Trimite solicitarea", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text(
            "© 2025 Spitalul Central TW",
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            color = blue,
            textAlign = TextAlign.Center
        )
    }

    // ---- DatePicker Dialog (calendar) ----
    if (datePickerOpen) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) {
                            val d = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            if (validateDateOrExplain(d)) {
                                selectedDate = d
                                selectedHour = null
                                scope.launch { fetchAvailability(d) }
                                datePickerOpen = false
                            }
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOpen = false }) { Text("Renunță") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/* ===== UI helper boxes ===== */

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
private fun SuccessBox(message: String) {
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
