package com.hkm.pozix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hkm.pozix.data.repository.PozixAiAccount
import com.hkm.pozix.data.repository.PozixAiAccountRepository
import kotlinx.coroutines.launch

@Composable
internal fun PozixAiAccountCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { PozixAiAccountRepository(context) }
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf<PozixAiAccount?>(null) }
    var loading by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    LaunchedEffect(repository) { account = runCatching { repository.current() }.getOrNull() }

    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Pozix AI account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("3 quiz generations per day · up to 15 questions each", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (account != null) {
                Text("Signed in as ${account!!.displayName} (@${account!!.username})", style = MaterialTheme.typography.bodyMedium)
                if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !loading, onClick = { loading = true; scope.launch { repository.logout(); account = null; loading = false } }) {
                        Icon(Icons.Default.Logout, null); Text("Sign out", Modifier.padding(start = 6.dp))
                    }
                    TextButton(enabled = !loading, onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error); Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Text("Create an account or sign in to use the built-in AI providers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (showForm) {
                    Spacer(Modifier.height(8.dp))
                    if (creating) OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(password, { password = it }, label = { Text("Password (10+ characters)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !loading, onClick = {
                            loading = true; notice = ""
                            scope.launch {
                                try { account = if (creating) repository.register(displayName, username, password) else repository.login(username, password); showForm = false; password = "" }
                                catch (e: Exception) { notice = e.message ?: "Unable to sign in." }
                                loading = false
                            }
                        }) { if (loading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp) else Text(if (creating) "Create account" else "Sign in") }
                        TextButton(onClick = { creating = !creating; notice = "" }) { Text(if (creating) "I have an account" else "Create account") }
                    }
                } else {
                    Button(onClick = { showForm = true; creating = false }) { Text("Sign in") }
                    TextButton(onClick = { showForm = true; creating = true }) { Text("Create account") }
                }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete Pozix AI account?") },
        text = { Text("Your AI profile, server sessions, and quota records will be permanently deleted. Local quizzes and chat history stay on this device.") },
        confirmButton = { TextButton(onClick = {
            confirmDelete = false; loading = true
            scope.launch { try { repository.deleteAccount(); account = null; notice = "" } catch (e: Exception) { notice = e.message ?: "Unable to delete account." }; loading = false }
        }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )
}
