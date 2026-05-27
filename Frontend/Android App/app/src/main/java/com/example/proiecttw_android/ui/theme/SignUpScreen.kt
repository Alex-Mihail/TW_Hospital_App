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
import com.example.proiecttw_android.data.models.PatientRegisterRequest
import com.example.proiecttw_android.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState

private fun isValidEmail(email: String): Boolean {
    val regex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    return regex.matches(email)
}

private fun validateDobStrict(dob: String): Result<Unit> {
    val s = dob.trim()
    if (s.isEmpty()) {
        return Result.failure(IllegalStateException("Completează data nașterii (YYYY-MM-DD)."))
    }

    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(s)) {
        return Result.failure(
            IllegalStateException("Data nașterii invalidă. Format corect: YYYY-MM-DD (ex: 2001-09-30).")
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var dateOfBirth by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    var dobPickerOpen by remember { mutableStateOf(false) }

    val todayStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun millisToIsoDate(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(millis))
    }

    fun validateAll(): Boolean {
        val fn = firstName.trim()
        val ln = lastName.trim()
        val un = username.trim()
        val em = email.trim()
        val pw = password
        val ph = phone.trim()
        val dob = dateOfBirth.trim()

        if (fn.isEmpty() || ln.isEmpty() || em.isEmpty() || un.isEmpty() || pw.trim().isEmpty()) {
            error = "Completează prenume, nume, email, username și parola."
            return false
        }

        if (!isValidEmail(em)) {
            error = "Email invalid. Format corect: exemplu@email.com"
            return false
        }

        if (pw.length < 6) {
            error = "Parola trebuie să aibă minim 6 caractere."
            return false
        }

        if (password != password2) {
            error = "Parolele nu coincid."
            return false
        }

        if (ph.isEmpty()) {
            error = "Completează numărul de telefon."
            return false
        }

        val dobRes = validateDobStrict(dob)
        if (dobRes.isFailure) {
            error = dobRes.exceptionOrNull()?.message ?: "Data nașterii invalidă."
            return false
        }

        return true
    }

    suspend fun doRegister() {
        error = ""
        if (!validateAll()) return

        loading = true
        try {
            val res = withContext(Dispatchers.IO) {
                ApiClient.authApi.registerPatient(
                    PatientRegisterRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        username = username.trim(),
                        email = email.trim().ifBlank { null },
                        password = password,
                        phone = phone.trim(),
                        dateOfBirth = dateOfBirth.trim()
                    )
                )
            }

            if (res.code() == 409) {
                error = "Username sau email deja folosit."
                return
            }

            if (!res.isSuccessful) {
                error = "Eroare server la înregistrare (${res.code()})."
                return
            }

            navController.navigate("login") {
                popUpTo("home") { inclusive = false }
            }
        } catch (_: Exception) {
            error = "Backend indisponibil."
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
                onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
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
                    "Creare cont",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Înregistrează-te pentru programări online",
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
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Cont nou",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    FieldLabel("Prenume")
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    FieldLabel("Nume")
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    FieldLabel("Username")
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    FieldLabel("Email")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    FieldLabel("Telefon")
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        placeholder = { Text("07xx xxx xxx") }
                    )

                    FieldLabel("Data nașterii")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                error = ""
                                dobPickerOpen = true
                            }
                    ) {
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = {},
                            enabled = false,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            placeholder = { Text("Alege din calendar") },
                            supportingText = {
                                Text(
                                    "Nu poți selecta o dată din viitor.",
                                    color = Color(0xFF556677)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF0F172A),
                                disabledBorderColor = AppColors.Primary.copy(alpha = 0.25f),
                                disabledLabelColor = AppColors.Primary,
                                disabledPlaceholderColor = Color(0xFF556677),
                                disabledSupportingTextColor = Color(0xFF556677),
                                disabledContainerColor = Color.White
                            )
                        )
                    }

                    FieldLabel("Parolă")
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

                    FieldLabel("Confirmă parola")
                    OutlinedTextField(
                        value = password2,
                        onValueChange = { password2 = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    if (error.isNotBlank()) {
                        ErrorBox(error)
                    }

                    Button(
                        onClick = { scope.launch { doRegister() } },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (loading) "Se creează contul..." else "Creează cont",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Ai deja cont? ", color = Color(0xFF556677))
                        Text(
                            "Autentifică-te",
                            color = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { navController.navigate("login") }
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

    @OptIn(ExperimentalMaterial3Api::class)
    if (dobPickerOpen) {
        val state = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { dobPickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis == null) {
                            error = "Alege o dată."
                            return@TextButton
                        }

                        if (millis > todayStartMillis) {
                            error = "Data nașterii nu poate fi în viitor."
                            return@TextButton
                        }

                        dateOfBirth = millisToIsoDate(millis) // YYYY-MM-DD
                        dobPickerOpen = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { dobPickerOpen = false }) { Text("Renunță") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = AppColors.Primary, fontWeight = FontWeight.Medium)
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
