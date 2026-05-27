package com.example.proiecttw_android.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.proiecttw_android.ui.UserUi
import kotlinx.coroutines.launch

data class ChatMsg(val fromUser: Boolean, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatFab(
    modifier: Modifier = Modifier,
    baseUrl: String,
    user: UserUi?
) {
    var open by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Fixed floating button
    FloatingActionButton(
        onClick = {
            open = true
            scope.launch { sheetState.show() }
        },
        modifier = modifier,
        containerColor = AppColors.Primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(Icons.Default.Chat, contentDescription = "Chat AI")
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = {
                open = false
                scope.launch { sheetState.hide() }
            },
            sheetState = sheetState,
            containerColor = AppColors.Bg
        ) {
            AiChatSheetContent(
                baseUrl = baseUrl,
                user = user,
                onClose = {
                    open = false
                    scope.launch { sheetState.hide() }
                }
            )
        }
    }
}

@Composable
private fun AiChatSheetContent(
    baseUrl: String,
    user: UserUi?,
    onClose: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMsg(false, "Salut! Sunt asistentul TW Hospital. Te ajut cu programări, doctori și specializări.")
        )
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 420.dp, max = 620.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Asistent AI",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Primary
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Închide")
            }
        }

        Spacer(Modifier.height(10.dp))

        // Messages
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.large,
                            color = if (m.fromUser) AppColors.Primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = m.text,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = if (m.fromUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (loading) {
                    item {
                        Text(
                            "AI scrie...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Input + send
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Scrie un mesaj...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (!loading) {
                            scope.launch {
                                sendMsg(
                                    baseUrl = baseUrl,
                                    user = user,
                                    userText = input,
                                    messages = messages,
                                    setLoading = { loading = it },
                                    clearInput = { input = "" }
                                )
                            }
                        }
                    }
                )
            )

            Spacer(Modifier.width(10.dp))

            IconButton(
                onClick = {
                    if (!loading) {
                        scope.launch {
                            sendMsg(
                                baseUrl = baseUrl,
                                user = user,
                                userText = input,
                                messages = messages,
                                setLoading = { loading = it },
                                clearInput = { input = "" }
                            )
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Trimite")
            }
        }
    }
}

private suspend fun sendMsg(
    baseUrl: String,
    user: UserUi?,
    userText: String,
    messages: MutableList<ChatMsg>,
    setLoading: (Boolean) -> Unit,
    clearInput: () -> Unit
) {
    val msg = userText.trim()
    if (msg.isEmpty()) return

    messages.add(ChatMsg(true, msg))
    clearInput()
    setLoading(true)

    // UI context (varianta 2) - exact ca în React
    val uiContext = mapOf(
        "page" to "HomeScreen",
        "actions" to listOf("Caută", "Programează-te", "Contul meu", "Chat AI"),
        "steps" to listOf(
            "Apasă pe 'Programează-te' (te duce la Consultation)",
            "Caută specializarea sau medicul",
            "Selectează medicul",
            "Alege data și ora (08:00–16:00, minute 00)",
            "Confirmă programarea"
        )
    )

    // role/userId - adaptează dacă ai alte câmpuri
    val role = (user?.role ?: "PATIENT").uppercase()
    val userId = user?.id

    try {
        val answer = ChatApiClient.ask(
            baseUrl = baseUrl,
            message = msg,
            role = role,
            userId = userId,
            uiContext = uiContext
        )
        messages.add(ChatMsg(false, answer))
    } catch (e: Exception) {
        messages.add(ChatMsg(false, "Eroare: nu pot contacta serverul. (${e.message ?: "unknown"})"))
    } finally {
        setLoading(false)
    }
}