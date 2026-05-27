package com.example.proiecttw_android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.proiecttw_android.R
import com.example.proiecttw_android.data.api.*
import com.example.proiecttw_android.data.datastore.SessionStore
import com.example.proiecttw_android.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun validateDobStrict(dob: String): Result<Unit> {
    val s = dob.trim()
    if (s.isEmpty()) {
        return Result.failure(IllegalStateException("Completează data nașterii (YYYY-MM-DD)."))
    }

    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(s)) {
        return Result.failure(IllegalStateException("Data nașterii invalidă. Format corect: YYYY-MM-DD (ex: 2001-09-30)."))
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val parsed: Date = try {
        sdf.parse(s) ?: return Result.failure(IllegalStateException("Data nașterii invalidă."))
    } catch (_: ParseException) {
        return Result.failure(IllegalStateException("Data nașterii invalidă (ex: 2025-02-30 nu există)."))
    }

    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = parsed }
    val year = cal.get(Calendar.YEAR)

    val nowCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val currentYear = nowCal.get(Calendar.YEAR)

    if (year < 1900 || year > currentYear) {
        return Result.failure(IllegalStateException("An invalid. Folosește un an între 1900 și $currentYear."))
    }

    val todayStartUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    if (parsed.after(todayStartUtc) && s != sdf.format(todayStartUtc)) {
        return Result.failure(IllegalStateException("Data nașterii nu poate fi în viitor."))
    }

    return Result.success(Unit)
}

@Composable
fun EditAccountScreen(
    navController: NavController,
    user: UserUi?,
    sessionStore: SessionStore
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }

    var username by remember { mutableStateOf("-") }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }

    val roleUpper = user?.role?.uppercase().orEmpty()
    val roleLabel = if (roleUpper.isBlank()) "-" else roleUpper
    val showPatientExtras = roleUpper == "PATIENT"

    // Guard: if not logged in -> login
    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate("login") { popUpTo("home") }
        }
    }

    // Load profile
    LaunchedEffect(user?.id, user?.role) {
        val u = user ?: return@LaunchedEffect
        loading = true
        err = ""

        try {
            when (u.role.uppercase()) {
                "PATIENT" -> {
                    val res = ApiClient.userApi.getPatient(u.id)
                    if (res.code() == 404) err = "Profilul nu a fost găsit (404)."
                    else if (!res.isSuccessful) err = "Eroare backend: ${res.code()}"
                    else {
                        val p = res.body()
                        if (p == null) err = "Răspuns invalid (body lipsă)."
                        else {
                            username = p.username ?: "-"
                            firstName = p.firstName.orEmpty()
                            lastName = p.lastName.orEmpty()
                            email = p.email.orEmpty()
                            phone = p.phone.orEmpty()
                            dateOfBirth = p.dateOfBirth.orEmpty()
                        }
                    }
                }

                "DOCTOR" -> {
                    val res = ApiClient.userApi.getDoctorAccount(u.id)
                    if (res.code() == 404) err = "Profilul nu a fost găsit (404)."
                    else if (!res.isSuccessful) err = "Eroare backend: ${res.code()}"
                    else {
                        val d = res.body()
                        if (d == null) err = "Răspuns invalid (body lipsă)."
                        else {
                            username = d.username ?: "-"
                            firstName = d.firstName.orEmpty()
                            lastName = d.lastName.orEmpty()
                            email = d.email.orEmpty()
                            phone = ""       // doctor edit: nu avem în payload, îl lăsăm gol
                            dateOfBirth = "" // doctor edit: idem
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

    val displayName = (("${firstName.trim()} ${lastName.trim()}").trim())
        .ifBlank { if (username != "-") username else "Cont" }

    fun handleCancel() {
        navController.navigate("account") {
            popUpTo("account") { inclusive = true }
        }
    }

    fun validate(): Boolean {
        if (firstName.trim().isEmpty() || lastName.trim().isEmpty()) {
            err = "Prenumele și numele sunt obligatorii."
            return false
        }

        // DOB validation
        if (showPatientExtras) {
            val dob = dateOfBirth.trim()
            if (dob.isNotBlank()) {
                val r = validateDobStrict(dob)
                if (r.isFailure) {
                    err = r.exceptionOrNull()?.message ?: "Data nașterii invalidă."
                    return false
                }
            }
        }

        return true
    }

    suspend fun doSave() {
        val u = user ?: return
        err = ""

        if (!validate()) return

        saving = true
        try {
            when (u.role.uppercase()) {
                "PATIENT" -> {
                    val body = PatientUpdateRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = email.trim().ifBlank { null },
                        phone = phone.trim().ifBlank { null },
                        dateOfBirth = dateOfBirth.trim().ifBlank { null }
                    )
                    val res = ApiClient.userApi.updatePatient(u.id, body)
                    if (!res.isSuccessful) {
                        err = "Eroare la salvare (${res.code()})"
                        return
                    }
                }

                "DOCTOR" -> {
                    val body = DoctorAdminUpdateRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = email.trim().ifBlank { null }
                    )
                    val res = ApiClient.userApi.updateDoctorAccount(u.id, body)
                    if (!res.isSuccessful) {
                        err = "Eroare la salvare (${res.code()})"
                        return
                    }
                }

                else -> {
                    err = "Rol necunoscut: ${u.role}"
                    return
                }
            }

            // save account in localStorage
            sessionStore.updateName(firstName.trim(), lastName.trim())

            // back to account
            navController.navigate("account") {
                popUpTo("account") { inclusive = true }
            }
        } catch (_: Exception) {
            err = "Backend indisponibil / eroare rețea la salvare."
        } finally {
            saving = false
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
                onClick = { handleCancel() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                enabled = !saving
            ) {
                Text("← Înapoi la cont", color = Color.White)
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
                .height(260.dp)
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
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Editează cont",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Actualizează datele contului tău.",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    textAlign = TextAlign.Center
                )
            }
        }

        // CARD
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

                    // HEADER ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (loading) "Se încarcă..." else displayName,
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
                                text = roleLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { handleCancel() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(42.dp),
                                enabled = !saving
                            ) {
                                Text("Anulează", fontWeight = FontWeight.Bold, color = AppColors.Primary)
                            }

                            Button(
                                onClick = { scope.launch { doSave() } },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(42.dp),
                                enabled = !saving && !loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(if (saving) "Se salvează..." else "Salvează", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (err.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        ErrorBox(err)
                    }

                    if (loading && err.isBlank()) {
                        Spacer(Modifier.height(16.dp))
                        InfoNote("Se încarcă datele...")
                    }

                    if (!loading && err.isBlank()) {
                        Spacer(Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            FieldReadOnly(label = "Username", value = username)

                            FieldInput(
                                label = "Prenume",
                                value = firstName,
                                onValueChange = { firstName = it },
                                placeholder = "Prenume"
                            )

                            FieldInput(
                                label = "Nume",
                                value = lastName,
                                onValueChange = { lastName = it },
                                placeholder = "Nume"
                            )

                            FieldInput(
                                label = "Email",
                                value = email,
                                onValueChange = { email = it },
                                placeholder = "email@exemplu.com",
                                keyboardType = KeyboardType.Email
                            )

                            if (showPatientExtras) {
                                FieldInput(
                                    label = "Telefon",
                                    value = phone,
                                    onValueChange = { phone = it },
                                    placeholder = "07xx xxx xxx",
                                    keyboardType = KeyboardType.Phone
                                )

                                FieldInput(
                                    label = "Data nașterii (YYYY-MM-DD)",
                                    value = dateOfBirth,
                                    onValueChange = { dateOfBirth = it },
                                    placeholder = "ex: 2001-09-30",
                                    keyboardType = KeyboardType.Number
                                )
                            }
                        }
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
private fun FieldReadOnly(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F9FC))
            .padding(12.dp)
    ) {
        Text(label, color = AppColors.Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun FieldInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F9FC))
            .padding(12.dp)
    ) {
        Text(label, color = AppColors.Primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true
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
