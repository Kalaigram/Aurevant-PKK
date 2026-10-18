package com.aurevant.pkk.ui.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(state: AuthState, onAction: (AuthAction) -> Unit, onRegister: () -> Unit) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Aurevant PKK", style = MaterialTheme.typography.headlineMedium)
        Text("Masuk dengan email atau nomor HP.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(login, { login = it }, Modifier.fillMaxWidth(), label = { Text("Email / nomor HP") }, singleLine = true)
        OutlinedTextField(
            password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Kata sandi") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true,
        )
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { onAction(AuthAction.Login(login, password)) },
            Modifier.fillMaxWidth(), enabled = !state.busy && login.isNotBlank() && password.isNotBlank(),
        ) { Text(if (state.busy) "Memproses…" else "Masuk") }
        TextButton(onClick = onRegister, Modifier.fillMaxWidth()) { Text("Belum punya akun? Daftar") }
    }
}

@Composable
fun RegisterScreen(state: AuthState, onAction: (AuthAction) -> Unit, onLogin: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Daftar akun baru", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nama lengkap") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Nomor HP (opsional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Kata sandi (min 8)") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
        OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Ulangi kata sandi") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { onAction(AuthAction.Register(name, email, phone, password, confirm)) },
            Modifier.fillMaxWidth(), enabled = !state.busy && name.isNotBlank() && email.isNotBlank() && password.length >= 8 && password == confirm,
        ) { Text(if (state.busy) "Memproses…" else "Daftar") }
        TextButton(onClick = onLogin, Modifier.fillMaxWidth()) { Text("Sudah punya akun? Masuk") }
    }
}
