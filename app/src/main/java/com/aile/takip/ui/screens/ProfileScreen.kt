package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(vm: MainViewModel) {
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
                    Text("\uD83D\uDC65 Aile Uyeleri (${members.size})", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    members.forEach { m ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {}
                            Spacer(Modifier.width(8.dp))
                            Text(m.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
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
}
