package com.example.proiecttw_android.ui.consultation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.proiecttw_android.R
import com.example.proiecttw_android.ui.UserUi
import com.example.proiecttw_android.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationScreen(
    navController: NavController,
    user: UserUi?,
    searchArg: String?,
    vm: ConsultationViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.init(loadGlobalImmediately = true, initialSearch = searchArg)
    }

    val filtered = remember(ui.doctors, ui.search) {
        val q = ui.search.trim().lowercase()
        if (q.isBlank()) ui.doctors
        else ui.doctors.filter { d ->
            val fullName = "${d.firstName.orEmpty()} ${d.lastName.orEmpty()}".lowercase()
            val spec = d.specialization?.name.orEmpty().lowercase()
            fullName.contains(q) || spec.contains(q)
        }
    }

    val displayName = remember(user) {
        user?.let { "${it.firstName} ${it.lastName}".trim() }.orEmpty()
    }

    val bgPage = AppColors.Bg
    val topBlue = AppColors.TopBar
    val blue = AppColors.Primary
    val green = Color(0xFF0AA862)
    val cardBorder = Color(0xFF0F172A).copy(alpha = 0.08f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgPage)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBlue)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("0740 123 456", color = Color.White)
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = { navController.navigate(if (user != null) "account" else "login") },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                modifier = Modifier.widthIn(max = 220.dp)
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (displayName.isNotBlank()) displayName else "Contul meu",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // HERO
            item {
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
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Consultații Medicale",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Alege specializarea și găsește medicul potrivit.",
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 420.dp)
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-18).dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            blue.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Caută rapid",
                                    color = blue,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    "Poți selecta o specializare sau poți căuta global după nume / specializare.",
                                    color = Color(0xFF0F172A).copy(alpha = 0.70f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    readOnly = true,
                                    value = if (ui.selectedSpec.isBlank()) "(Căutare globală)" else ui.selectedSpec,
                                    onValueChange = {},
                                    label = { Text("Specializare") },
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("(Căutare globală)") },
                                        onClick = {
                                            expanded = false
                                            vm.onSelectSpecialization("")
                                        }
                                    )
                                    ui.specializations.forEach { s ->
                                        val name = s.name.orEmpty()
                                        if (name.isNotBlank()) {
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    expanded = false
                                                    vm.onSelectSpecialization(name)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = ui.search,
                                onValueChange = {
                                    vm.onSearchChange(it)
                                    vm.ensureGlobalLoadedIfNeeded()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Caută medic / specializare") },
                                placeholder = { Text("Ex: Popescu / Cardiologie") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (ui.loadingGlobal) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            ui.error?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // doctor list
            item {
                Spacer(Modifier.height(4.dp))
            }

            items(filtered, key = { it.id ?: 0L }) { doctor ->
                DoctorCardReactLike(
                    doctorName = "${doctor.firstName.orEmpty()} ${doctor.lastName.orEmpty()}".trim(),
                    specName = doctor.specialization?.name.orEmpty().ifBlank { "Specializare" },
                    blue = blue,
                    green = green,
                    borderColor = cardBorder,
                    onDetails = {
                        val id = doctor.id ?: return@DoctorCardReactLike
                        navController.navigate("doctor-details/$id")
                    },
                    onBook = { doctor.id?.let { navController.navigate("booking/$it") } },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                )
            }

            if (ui.selectedSpec.isNotBlank() && filtered.isEmpty()) {
                item {
                    Text(
                        "Nu există doctori pentru această specializare.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.65f)
                    )
                }
            }

            if (ui.selectedSpec.isBlank() && ui.search.isNotBlank() && filtered.isEmpty()) {
                item {
                    Text(
                        "Nu am găsit rezultate pentru „${ui.search}”.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorCardReactLike(
    doctorName: String,
    specName: String,
    blue: Color,
    green: Color,
    borderColor: Color,
    onDetails: () -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        doctorName.ifBlank { "Doctor" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF0F172A)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(blue.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        specName,
                        color = blue,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDetails,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = blue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, blue.copy(alpha = 0.25f))
                ) {
                    Text("Detalii")
                }

                Spacer(Modifier.width(10.dp))

                Button(
                    onClick = onBook,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = green,
                        contentColor = Color.White
                    )
                ) {
                    Text("Programează-te", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
