package com.iceman.teveclub.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iceman.teveclub.AccountViewModel
import com.iceman.teveclub.R
import com.iceman.teveclub.TeveViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(vm: TeveViewModel) {
    val ctx = LocalContext.current
    val accountVm: AccountViewModel = viewModel()
    val accounts = accountVm.accounts.collectAsState()
    val isLoading = vm.isLoading.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var rememberAccount by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { accountVm.loadAccounts() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TeveColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .padding(20.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(TeveColors.PanelBackground)
                .border(3.dp, TeveColors.PanelBorder, RoundedCornerShape(15.dp))
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_teveclub_logo),
                contentDescription = "Teveclub",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "🐪 Teveclub",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TeveColors.HeadingBrown
            )
            Text(
                "Mobil kezelőfelület",
                fontSize = 14.sp,
                color = TeveColors.SubtitleBrown
            )

            Spacer(Modifier.height(24.dp))

            // Saved accounts
            if (accounts.value.isNotEmpty()) {
                Text(
                    "Mentett fiókok",
                    fontWeight = FontWeight.SemiBold,
                    color = TeveColors.SubtitleBrown,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                accounts.value.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(2.dp, TeveColors.ActionBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                username = acc.usernameEncrypted
                                password = acc.passwordEncrypted ?: ""
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TeveColors.BodyText)
                        Spacer(Modifier.width(10.dp))
                        Text(acc.usernameEncrypted, color = TeveColors.BodyText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { accountVm.deleteAccount(acc) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Törlés", tint = TeveColors.ErrorRed)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            // Username
            Text("👤 Felhasználónév", fontWeight = FontWeight.SemiBold, color = TeveColors.SubtitleBrown, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Írd be a felhasználóneved") },
                shape = RoundedCornerShape(15.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = TeveColors.StatusHeaderGold,
                    unfocusedBorderColor = TeveColors.PanelBorder,
                    backgroundColor = TeveColors.PanelBackground
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Password
            Text("🔒 Jelszó", fontWeight = FontWeight.SemiBold, color = TeveColors.SubtitleBrown, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Írd be a jelszavad") },
                shape = RoundedCornerShape(15.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Elrejtés" else "Megjelenítés"
                        )
                    }
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = TeveColors.StatusHeaderGold,
                    unfocusedBorderColor = TeveColors.PanelBorder,
                    backgroundColor = TeveColors.PanelBackground
                ),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // Remember checkbox
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberAccount,
                    onCheckedChange = { rememberAccount = it },
                    colors = CheckboxDefaults.colors(checkedColor = TeveColors.ButtonBlue)
                )
                Text("Fiók megjegyzése", color = TeveColors.SubtitleBrown, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Login button
            Button(
                onClick = {
                    message = null
                    vm.login(username, password) { ok, err ->
                        if (ok) {
                            if (rememberAccount) {
                                accountVm.saveAccount(username, password)
                            }
                        } else {
                            message = err ?: "Bejelentkezés sikertelen"
                        }
                    }
                },
                enabled = !isLoading.value && username.isNotEmpty() && password.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = TeveColors.ButtonBlue)
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("🚀 Bejelentkezés", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Error message
            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    color = TeveColors.ErrorRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8D7DA))
                        .padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            // Footer
            Text(
                "🐪 teveclub.hu",
                fontSize = 12.sp,
                color = TeveColors.SubtitleBrown.copy(alpha = 0.7f)
            )
            val uriHandler = LocalUriHandler.current
            Text(
                "© ICEMAN",
                fontSize = 12.sp,
                color = TeveColors.ButtonBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://github.com/iceman-twitch")
                }
            )
        }
    }
}
