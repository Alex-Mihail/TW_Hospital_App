package com.example.proiecttw_android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.proiecttw_android.R
import com.example.proiecttw_android.ui.theme.AiChatFab
import com.example.proiecttw_android.ui.theme.AppColors
import java.net.URLEncoder

@Composable
fun HomeScreen(
    navController: NavController,
    user: UserUi?
) {
    var search by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Bg)
                .verticalScroll(rememberScrollState())
        ) {
            TopBar(
                userDisplayName = user?.displayName,
                onAccountClick = {
                    if (user == null) navController.navigate("login")
                    else navController.navigate("account")
                }
            )

            HeroSection(
                search = search,
                onSearchChange = { search = it },
                onSearchSubmit = {
                    val q = search.trim()
                    if (q.isEmpty()) {
                        navController.navigate("consultation")
                    } else {
                        val encoded = URLEncoder.encode(q, "UTF-8")
                        navController.navigate("consultation/$encoded")
                    }
                }
            )

            IntroSection()

            ServicesGridSection(
                onAppointmentClick = { navController.navigate("consultation") }
            )

            Footer()
        }

        AiChatFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp),
            baseUrl = com.example.proiecttw_android.data.api.ApiConfig.BASE_HTTP,
            user = user
        )
    }
}

@Composable
private fun TopBar(
    userDisplayName: String?,
    onAccountClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.TopBar)
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
            onClick = onAccountClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                text = userDisplayName ?: "Contul meu",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HeroSection(
    search: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
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
                .background(Color.Black.copy(alpha = 0.40f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Spitalul Central TW",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "Îngrijire medicală la standarde internaționale.",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Light
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Caută un medic, o specializare sau un serviciu...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.55f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.75f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.75f),
                        focusedLeadingIconColor = Color.White,
                        unfocusedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                Button(
                    onClick = onSearchSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Caută", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun IntroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Sănătatea ta merită mai mult decât o soluție „rapidă”.",
            style = MaterialTheme.typography.titleLarge.copy(
                color = AppColors.Primary,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "La Spitalul Central TW, combinăm expertiza medicală cu tehnologii moderne și o abordare orientată către pacient.\n\n" +
                    "Ne concentrăm pe prevenție, diagnostic corect și continuitate în îngrijire, iar programările sunt simple.",
            style = MaterialTheme.typography.bodyLarge.copy(color = AppColors.Text),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 900.dp)
        )
    }
}

@Composable
private fun ServicesGridSection(
    onAppointmentClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Serviciile noastre medicale",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = AppColors.Primary,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        ServiceCard(
            title = "Consultații medicale",
            subtitle = "Acces rapid la medici specialiști.",
            imageRes = R.drawable.medical_service,
            modifier = Modifier.fillMaxWidth()
        )

        ServiceCard(
            title = "Tratamente medicale",
            subtitle = "Soluții moderne și eficiente.",
            imageRes = R.drawable.treatment,
            modifier = Modifier.fillMaxWidth()
        )

        ServiceCard(
            title = "Programări online",
            subtitle = "Rezervă consultații rapid, fără așteptare.",
            imageRes = R.drawable.appointment,
            modifier = Modifier.fillMaxWidth()
        )

        ServiceCard(
            title = "Echipă medicală",
            subtitle = "Medici experimentați și dedicați.",
            imageRes = R.drawable.medical_team,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Ai nevoie de o consultație? Programează-te gratuit la unul dintre medicii noștri specialiști și primești rapid recomandări clare, adaptate nevoilor tale.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = AppColors.Primary,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onAppointmentClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary,
                contentColor = Color.White
            )
        ) {
            Text("Programează-te", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ServiceCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(12.dp))

            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = AppColors.Primary,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.Text),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Footer() {
    Text(
        "© 2025 Spitalul Central TW",
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = AppColors.Primary,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall
    )
}
