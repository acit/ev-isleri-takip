package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aile.takip.sync.FirebaseSyncService
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncSettingsScreen(vm: MainViewModel, navController: NavController? = null) {
    val syncState by vm.syncState.collectAsState()
    val lastSync by vm.lastSyncTime.collectAsState()
    val syncedTables by vm.syncedTables.collectAsState()
    val syncEnabled by vm.syncEnabled
    val groupId by vm.familyGroupId
    var inputGroupId by remember { mutableStateOf(groupId.ifEmpty { "aile_" + (1000..9999).random() }) }

    // Handle QR scan result for group code
    val scanResult by vm.lastScanResult
    LaunchedEffect(scanResult) {
        if (scanResult != null) {
            inputGroupId = scanResult ?: ""
            vm.lastScanResult.value = null
        }
    }

    PageScaffold("\uD83D\uDD04 Senkronizasyon", "\uD83D\uDD04") {
        // Connection status
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(
            containerColor = when (syncState) {
                is FirebaseSyncService.SyncState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is FirebaseSyncService.SyncState.Syncing -> MaterialTheme.colorScheme.tertiaryContainer
                is FirebaseSyncService.SyncState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (syncState) {
                            is FirebaseSyncService.SyncState.Connected -> Icons.Default.CheckCircle
                            is FirebaseSyncService.SyncState.Syncing -> Icons.Default.Refresh
                            is FirebaseSyncService.SyncState.Error -> Icons.Default.Error
                            else -> Icons.Default.CloudOff
                        },
                        contentDescription = null,
                        tint = when (syncState) {
                            is FirebaseSyncService.SyncState.Connected -> MaterialTheme.colorScheme.primary
                            is FirebaseSyncService.SyncState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            when (syncState) {
                                is FirebaseSyncService.SyncState.Connected -> "Bağlı"
                                is FirebaseSyncService.SyncState.Syncing -> "Senkronize ediliyor: ${(syncState as FirebaseSyncService.SyncState.Syncing).table}"
                                is FirebaseSyncService.SyncState.Error -> "Hata: ${(syncState as FirebaseSyncService.SyncState.Error).message}"
                                is FirebaseSyncService.SyncState.Connecting -> "Bağlanıyor..."
                                else -> "Bağlantı yok"
                            },
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        if (lastSync > 0) {
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSync))
                            Text("Son senkron: $timeStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (syncedTables.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Senkronize edilen tablolar:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        syncedTables.take(6).forEach { table ->
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                                Text(table, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Group ID
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("\uD83D\uDC65 Aile Grubu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Aynı grup ID'sini kullanan tüm aile üyeleri otomatik senkronize olur.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputGroupId,
                        onValueChange = { inputGroupId = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Grup ID") },
                        placeholder = { Text("örn: aile_1234") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        readOnly = syncEnabled
                    )
                    if (navController != null && !syncEnabled) {
                        FilledIconButton(
                            onClick = { navController.navigate("scanner/qr") },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(Icons.Default.QrCodeScanner, "QR Tara")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("\u2139\uFE0F Tüm aile üyeleriniz bu ID'yi aynı anda girmeli. QR kodu ile hızlıca tarayabilirsiniz.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        if (!syncEnabled) {
            Button(
                onClick = { vm.connectToFirebase(inputGroupId) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Cloud, null)
                Spacer(Modifier.width(8.dp))
                Text("Senkronizasyonu Başlat", fontSize = 16.sp)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.syncToFirebase() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Hemen Push Et")
                }
                OutlinedButton(
                    onClick = { vm.disconnectFirebase() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudOff, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Kes")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // How it works
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("\uD83D\uDCA1 Nasıl Çalışır?", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val steps = listOf(
                    "1. Tüm aile üyeleri aynı Grup ID'yi girer",
                    "2. Bir üye 'Senkronize Et' butonuna basar",
                    "3. Tüm veriler Firebase'e yüklenir",
                    "4. Diğer üyeler otomatik olarak aynı verileri alır",
                    "5. Bundan sonra her değişiklik anında yansır"
                )
                steps.forEach { step ->
                    Text(step, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("\u26A0\uFE0F NOT: google-services.json dosyası Firebase konsolundan indirilmeli", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
