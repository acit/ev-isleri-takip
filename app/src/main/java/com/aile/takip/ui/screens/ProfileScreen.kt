package com.aile.takip.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aile.takip.ui.components.MemberAvatar
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(vm: MainViewModel, navController: NavController? = null) {
    val context = LocalContext.current
    val auth by vm.auth.collectAsState()
    val syncState by vm.syncState.collectAsState()
    val syncEnabled by vm.syncEnabled
    val currentMember = vm.currentMember
    var showChangePin by remember { mutableStateOf(false) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }
    val members by vm.members.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val inventory by vm.inventory.collectAsState()
    val invoices by vm.invoices.collectAsState()
    val shopping by vm.shoppingItems.collectAsState()
    
    // Theme preferences
    val useDynamicColor by vm.useDynamicColor
    val isDarkMode by vm.isDarkMode
    val useSystemTheme by vm.useSystemTheme
    val currentLanguage by vm.currentLanguage

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("\u2699\uFE0F Profil", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        // Profile card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) { Text("A", color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Aile Takip", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("v${com.aile.takip.BuildConfig.VERSION_NAME} \u00B7 Tum moduller aktif", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Stats
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("\uD83D\uDCCA Istatistikler", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${tasks.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                        Text("Gorev", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${inventory.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 18.sp)
                        Text("Envanter", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${shopping.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 18.sp)
                        Text("Market", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${invoices.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                        Text("Fatura", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ============ HESAP VE PAYLAŞIM ============
        Text("Hesap ve Paylaşım", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📱 Bu cihazı kullanan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemberAvatar(
                        name = currentMember?.name?.ifBlank { null } ?: auth?.name?.ifBlank { null } ?: "?",
                        colorHex = currentMember?.color ?: "#3498DB",
                        size = 44.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            currentMember?.name?.ifBlank { null } ?: auth?.name?.ifBlank { null } ?: "Aile üyesi",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            currentMember?.let { it.role.replaceFirstChar { c -> c.titlecase() } }
                                ?: "Bu cihaz için bir aile üyesi seçin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showMemberPicker = true }) { Text("Değiştir") }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { showProfileEdit = true },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Profili düzenle", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showChangePin = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("PIN Değiştir")
            }
            OutlinedButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Aile Takip daveti")
                    putExtra(Intent.EXTRA_TEXT, vm.familyInviteText())
                }
                context.startActivity(Intent.createChooser(intent, "Aile davetini paylaş"))
            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Davet Et")
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Aile Üyelerini Yönet", fontWeight = FontWeight.Medium)
                    Text(
                        "${members.size} üye · ekle, düzenle, puan ver",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { navController?.navigate("family-members") }) { Text("Aç") }
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (syncEnabled) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    null,
                    tint = if (syncEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Senkronizasyon", fontWeight = FontWeight.Medium)
                    Text(
                        if (syncEnabled) "Bağlı · veriler aile grubunda paylaşılıyor"
                        else "Bağlı değil · tüm veriler yalnızca bu cihazda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { navController?.navigate("sync") }) { Text("Aç") }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Appearance Settings
        Text("Gorunum Ayarlari", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        
        // Dynamic Color toggle (Material You)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("\uD83C\uDFA8 Material You Renkleri", fontWeight = FontWeight.Medium)
                    Text("Duvar kagidinizdan renk alir (Android 12+)", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = useDynamicColor, onCheckedChange = { vm.setDynamicColor(it) })
            }
        }
        Spacer(Modifier.height(8.dp))
        
        // Dark mode toggle
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("\uD83C\uDF19 Karanlik Mod", fontWeight = FontWeight.Medium)
                    Text("Sistem ayarini mi kullanacaksiniz?", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = useSystemTheme, onCheckedChange = { 
                    if (it) vm.useSystemTheme()
                })
            }
        }
        
        if (!useSystemTheme) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isDarkMode) "\uD83C\uDF19 Karanlik" else "\u2600\uFE0F Aydinlik", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Switch(checked = isDarkMode, onCheckedChange = { vm.setDarkMode(it) })
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Language Selection
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83C\uDF10 Dil", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                }
                Text("Uygulama dilini secin", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentLanguage == "tr",
                        onClick = { vm.setLanguage("tr") },
                        label = { Text("Turkce") }
                    )
                    FilterChip(
                        selected = currentLanguage == "en",
                        onClick = { vm.setLanguage("en") },
                        label = { Text("English") }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        // Family members
        if (members.isNotEmpty()) {
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDC65 Aile Uyeleri (${members.size})", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { navController?.navigate("family-members") }) { Text("Yönet") }
                    }
                    Spacer(Modifier.height(8.dp))
                    members.forEach { m ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            MemberAvatar(m.name, m.color, 28.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.name, fontWeight = FontWeight.Medium)
                                if (vm.myMemberId.value == m.id) {
                                    Text("bu cihaz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text("${m.points} puan", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Logout button
        Button(
            onClick = { vm.logout() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Cikis Yap")
        }
    }

    // ===== PIN DEĞİŞTİR =====
    if (showChangePin) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showChangePin = false },
            icon = { Icon(Icons.Default.Lock, null) },
            title = { Text("PIN Değiştir") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6) newPin = it.filter { c -> c.isDigit() } },
                        label = { Text("Yeni PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6) confirmPin = it.filter { c -> c.isDigit() } },
                        label = { Text("Yeni PIN (tekrar)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    when {
                        newPin.length < 4 -> error = "PIN en az 4 haneli olmalı"
                        newPin != confirmPin -> error = "PIN'ler aynı değil"
                        else -> { vm.resetPinWithNew(newPin); showChangePin = false }
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showChangePin = false }) { Text("İptal") } }
        )
    }

    // ===== PROFİL DÜZENLE =====
    if (showProfileEdit) {
        var editName by remember { mutableStateOf(auth?.name ?: "") }
        var editEmail by remember { mutableStateOf(auth?.email ?: "") }
        AlertDialog(
            onDismissRequest = { showProfileEdit = false },
            icon = { Icon(Icons.Default.Person, null) },
            title = { Text("Profili Düzenle") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Ad Soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("E-posta (isteğe bağlı)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.updateProfile(editName.trim(), editEmail.trim())
                    showProfileEdit = false
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showProfileEdit = false }) { Text("İptal") } }
        )
    }

    // ===== CİHAZ KULLANICISI SEÇ =====
    if (showMemberPicker) {
        AlertDialog(
            onDismissRequest = { showMemberPicker = false },
            icon = { Icon(Icons.Default.Groups, null) },
            title = { Text("Bu cihazı kullanan kişi") },
            text = {
                Column {
                    Text(
                        "Seçilen üyenin adı, yaptığınız değişikliklerde görünür.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    members.forEach { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(m.name, m.color, 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(m.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            if (vm.myMemberId.value == m.id) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                TextButton(onClick = { vm.setDeviceUser(m); showMemberPicker = false }) { Text("Seç") }
                            }
                        }
                    }
                    if (members.isEmpty()) {
                        Text(
                            "Önce aile üyesi ekleyin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = { Button(onClick = { showMemberPicker = false }) { Text("Kapat") } }
        )
    }
}
