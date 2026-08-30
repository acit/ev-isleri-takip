package com.aile.takip.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aile.takip.ui.viewmodel.MainViewModel

@Composable
fun LoginScreen(vm: MainViewModel) {
    val auth by vm.auth.collectAsState()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showSetup by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    var setupPin by remember { mutableStateOf("") }
    var setupName by remember { mutableStateOf("") }
    var setupEmail by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    val hasPin = auth?.pin?.isNotEmpty() ?: false

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Box(contentAlignment = Alignment.Center) { Text("\uD83C\uDFE0", fontSize = 36.sp) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Aile Takip", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Aile yönetim sistemi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(40.dp))

        if (!showSetup && !showForgot) {
            // PIN Entry
            if (hasPin) {
                Text("PIN girin", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))

                // PIN dots
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { i ->
                        Surface(
                            modifier = Modifier.size(16.dp),
                            shape = CircleShape,
                            color = if (i < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {}
                    }
                }
                Spacer(Modifier.height(16.dp))

                // PIN pad
                val pinPad = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
                pinPad.forEach { key ->
                    if (key.isNotEmpty()) {
                        // We need a grid, use FlowRow-like approach
                    }
                }

                // Simple PIN input
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { pin = it; error = false } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("4 haneli PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = error,
                    shape = RoundedCornerShape(12.dp)
                )
                if (error) {
                    Text("Yanlış PIN!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { vm.login(pin); if (!vm.isAuthenticated.value) error = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Giriş Yap", fontSize = 16.sp) }

                Spacer(Modifier.height(12.dp))
                Text("Şifremi Unuttum", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showForgot = true })
            } else {
                // No PIN set — first time setup
                Text("Hoş Geldiniz!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Güvenlik için bir PIN belirleyin", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(value = setupName, onValueChange = { setupName = it }, label = { Text("Adınız") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = setupEmail, onValueChange = { setupEmail = it }, label = { Text("E-posta (isteğe bağlı)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = setupPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) setupPin = it }, label = { Text("4 haneli PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it }, label = { Text("PIN tekrar") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), isError = confirmPin.isNotEmpty() && confirmPin != setupPin)
                if (confirmPin.isNotEmpty() && confirmPin != setupPin) Text("PIN'ler eşleşmiyor", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { if (setupPin.length == 4 && setupPin == confirmPin) { vm.setupPin(setupPin, setupName, setupEmail); vm.login(setupPin) } },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = setupPin.length == 4 && setupPin == confirmPin
                ) { Text("Başla", fontSize = 16.sp) }

                Spacer(Modifier.height(12.dp))
                Text("Şimdilik atla →", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { vm.isAuthenticated.value = true })
            }
        }

        // Forgot password flow
        if (showForgot) {
            Text("Şifre Sıfırlama", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Mevcut PIN'inizi bilmiyorsanız, verileriniz silinerek yeni PIN belirleyebilirsiniz.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(value = setupName, onValueChange = { setupName = it }, label = { Text("Adınız") }, singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = setupPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) setupPin = it }, label = { Text("Yeni 4 haneli PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it }, label = { Text("PIN tekrar") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (setupPin.length == 4 && setupPin == confirmPin) { vm.resetPin(); vm.setupPin(setupPin, setupName, ""); vm.login(setupPin) } },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
                enabled = setupPin.length == 4 && setupPin == confirmPin,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Sıfırla ve Gir", fontSize = 16.sp) }
            Spacer(Modifier.height(12.dp))
            Text("Geri", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showForgot = false; setupPin = ""; confirmPin = "" })
        }
    }
}
