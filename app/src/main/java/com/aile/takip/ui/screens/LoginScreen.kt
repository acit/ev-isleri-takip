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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(vm: MainViewModel) {
    val auth by vm.auth.collectAsState()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }
    
    // Setup state
    var showSetup by remember { mutableStateOf(false) }
    var setupPin by remember { mutableStateOf("") }
    var setupName by remember { mutableStateOf("") }
    var setupEmail by remember { mutableStateOf("") }
    var setupPhone by remember { mutableStateOf("") }
    var setupSecurityQuestion by remember { mutableStateOf("") }
    var setupSecurityAnswer by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    
    // Forgot password state
    var showForgot by remember { mutableStateOf(false) }
    var forgotStep by remember { mutableIntStateOf(1) } // 1: security question, 2: new pin
    var forgotAnswer by remember { mutableStateOf("") }
    var forgotNewPin by remember { mutableStateOf("") }
    var forgotConfirmPin by remember { mutableStateOf("") }
    var forgotError by remember { mutableStateOf(false) }

    val hasPin = auth?.pin?.isNotEmpty() ?: false
    val maxAttempts = 5

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
        Text("Aile y\u00f6netim sistemi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(40.dp))

        // ==================== PIN ENTRY ====================
        if (!showSetup && !showForgot) {
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

                // PIN input
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) { 
                            pin = it; error = false; showError = false 
                        } 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("4 haneli PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = error,
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (error) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (attemptCount >= maxAttempts) "Verya sayisi asildii! 5 dakika bekleyin." 
                        else "Yanlis PIN! (${maxAttempts - attemptCount} hak kaldi)",
                        color = MaterialTheme.colorScheme.error, 
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { 
                        vm.login(pin)
                        if (!vm.isAuthenticated.value) {
                            error = true
                            attemptCount++
                            pin = ""
                            if (attemptCount >= maxAttempts) showError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = pin.length == 4 && !showError
                ) { Text("Giris Yap", fontSize = 16.sp) }

                Spacer(Modifier.height(12.dp))
                
                // Forgot password
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, "Kilit", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Sifremi Unuttum", 
                        color = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.clickable { showForgot = true },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (showError) {
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, "Uyari", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Cok fazla hatali deneme!", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text("5 dakika bekleyin veya sifrenizi sifirlayin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
                
            } else {
                // ==================== FIRST TIME SETUP ====================
                Text("Hos Geldiniz!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Guvenlik icin bir PIN belirleyin", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(value = setupName, onValueChange = { setupName = it }, label = { Text("Adiniz") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = setupEmail, onValueChange = { setupEmail = it }, label = { Text("E-posta (istege bagli)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = setupPhone, onValueChange = { setupPhone = it }, label = { Text("Telefon (istege bagli)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                
                // Security question
                Text("Guvenlik Sorusu (Sifre sifirlama icin)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = setupSecurityQuestion, 
                        onValueChange = {}, 
                        readOnly = true,
                        label = { Text("Secin...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(
                            "En sevdiginiz yemek ne?",
                            "Ilk evcil hayvaninizin adi ne?",
                            "Dogum yeriniz neresi?",
                            "En iyi arkadasinizin adi ne?",
                            "ilk okulunuzun adi ne?"
                        ).forEach { question ->
                            DropdownMenuItem(
                                text = { Text(question) },
                                onClick = { setupSecurityQuestion = question; expanded = false }
                            )
                        }
                    }
                }
                
                if (setupSecurityQuestion.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = setupSecurityAnswer, 
                        onValueChange = { setupSecurityAnswer = it }, 
                        label = { Text("Cevap") }, 
                        singleLine = true, 
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = setupPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) setupPin = it }, label = { Text("4 haneli PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it }, label = { Text("PIN tekrar") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(12.dp), isError = confirmPin.isNotEmpty() && confirmPin != setupPin)
                if (confirmPin.isNotEmpty() && confirmPin != setupPin) Text("PIN'ler eslesmiyor", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { 
                        if (setupPin.length == 4 && setupPin == confirmPin) { 
                            vm.setupPin(setupPin, setupName, setupEmail, setupSecurityQuestion, setupSecurityAnswer)
                            vm.login(setupPin) 
                        } 
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = setupPin.length == 4 && setupPin == confirmPin
                ) { Text("Basla", fontSize = 16.sp) }

                Spacer(Modifier.height(12.dp))
                Text("Simdilik atla \u2192", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { vm.isAuthenticated.value = true })
            }
        }

        // ==================== FORGOT PASSWORD ====================
        if (showForgot) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Security, "Guvenlik", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    
                    if (forgotStep == 1) {
                        // Step 1: Security question
                        Text("Sifre Sifirlama", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Guvenlik sorunuzu cevaplayarak yeni PIN belirleyebilirsiniz.",
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        // Show security question from saved auth
                        val savedQuestion = auth?.securityQuestion ?: "En sevdiginiz yemek ne?"
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text(savedQuestion, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = forgotAnswer, 
                            onValueChange = { forgotAnswer = it; forgotError = false }, 
                            label = { Text("Cevabiniz") }, 
                            singleLine = true, 
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = forgotError
                        )
                        if (forgotError) {
                            Spacer(Modifier.height(4.dp))
                            Text("Cevap yanlis!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                if (vm.verifySecurityAnswer(forgotAnswer)) {
                                    forgotStep = 2
                                    forgotError = false
                                } else {
                                    forgotError = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = forgotAnswer.isNotBlank()
                        ) { Text("Dogrula", fontSize = 16.sp) }
                        
                    } else {
                        // Step 2: New PIN
                        Text("Yeni PIN Belirle", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Verileriniz korunacaktir. Yeni PIN'inizi girin.",
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = forgotNewPin, 
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) forgotNewPin = it }, 
                            label = { Text("Yeni 4 haneli PIN") }, 
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, 
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = forgotConfirmPin, 
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) forgotConfirmPin = it }, 
                            label = { Text("PIN tekrar") }, 
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, 
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = forgotConfirmPin.isNotEmpty() && forgotConfirmPin != forgotNewPin
                        )
                        if (forgotConfirmPin.isNotEmpty() && forgotConfirmPin != forgotNewPin) {
                            Text("PIN'ler eslesmiyor", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                if (forgotNewPin.length == 4 && forgotNewPin == forgotConfirmPin) {
                                    vm.resetPinWithNew(forgotNewPin)
                                    vm.login(forgotNewPin)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = forgotNewPin.length == 4 && forgotNewPin == forgotConfirmPin,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Kaydet ve Gir", fontSize = 16.sp) }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Geri", 
                        color = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.clickable { 
                            showForgot = false
                            forgotStep = 1
                            forgotAnswer = ""
                            forgotNewPin = ""
                            forgotConfirmPin = ""
                            forgotError = false
                        }
                    )
                }
            }
        }
    }
}
