package com.aile.takip.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aile.takip.sync.FirebaseSyncService
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.components.QrCodeView
import com.aile.takip.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncSettingsScreen(vm: MainViewModel, navController: NavController? = null) {
    val context = LocalContext.current
    val syncState by vm.syncState.collectAsState()
    val lastSync by vm.lastSyncTime.collectAsState()
    val autoSyncEnabled by vm.autoSyncEnabled.collectAsState()
    val pendingCount by vm.pendingSyncCount.collectAsState()
    val autoPushCount by vm.autoPushCount.collectAsState()
    val lastAutoPush by vm.lastAutoPushAt.collectAsState()
    val conflictsSolved by vm.conflictsSolved.collectAsState()
    val membership by vm.membership.collectAsState()
    val uid by vm.familyUid.collectAsState()
    val pendingRequests by vm.pendingJoinRequests.collectAsState()
    val authError by vm.authError.collectAsState()
    val members by vm.members.collectAsState()
    val currentMember = vm.currentMember

    val groupId by vm.familyGroupId
    val passcode by vm.familyPasscode
    val syncError by vm.syncError
    val signedInEmail by vm.signedInEmail

    var email by remember { mutableStateOf(signedInEmail) }
    var password by remember { mutableStateOf("") }
    var inputPasscode by remember { mutableStateOf(passcode) }
    var joinGroupId by remember { mutableStateOf(groupId) }
    var inputPasscodeJoin by remember { mutableStateOf("") }
    var inviteCopied by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    // QR tarama sonucu: davet kodu ise bilgileri doldur ve tek adımda istek gönder
    val scanResult by vm.lastScanResult
    LaunchedEffect(scanResult) {
        val raw = scanResult ?: return@LaunchedEffect
        val invite = vm.parseInvite(raw)
        if (invite != null) {
            joinGroupId = invite.groupId
            inputPasscodeJoin = invite.passcode
            // "Tek tarama": giriş yapılmışsa ve henüz üye değilsek isteği hemen gönder
            val state = vm.membership.value
            val canAutoJoin = uid != null &&
                state !is FirebaseSyncService.Membership.Member &&
                state !is FirebaseSyncService.Membership.Pending
            if (canAutoJoin) {
                vm.requestJoinFamily(invite.groupId, invite.passcode)
            }
        } else {
            joinGroupId = raw
        }
        vm.lastScanResult.value = null
    }

    fun shareIntent() = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Aile Takip daveti")
        putExtra(Intent.EXTRA_TEXT, vm.familyInviteText())
    }

    fun shareInvite() {
        context.startActivity(Intent.createChooser(shareIntent(), "Aile davetini paylaş"))
    }

    /** Daveti doğrudan WhatsApp ile açar; WhatsApp yüklü değilse genel paylaşım ekranına düşer. */
    fun shareToWhatsApp() {
        val text = Uri.encode(vm.familyInviteText())
        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=$text")).apply {
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(waIntent)
        } catch (_: ActivityNotFoundException) {
            shareInvite()
        }
    }

    fun copyInvite() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Aile Takip daveti", vm.familyInviteCode()))
        inviteCopied = true
    }

    PageScaffold("\uD83D\uDD04 Senkronizasyon", "\uD83D\uDD04") {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            // ==================== 1. AİLE HESABI ====================
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uid == null) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("\uD83D\uDD11 Aile Hesabı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Senkronizasyon gerçek üye kimliğiyle (Firebase UID) çalışır. Aile bireyleri kendi e-posta/şifresiyle giriş yapar; veriler yalnızca onaylı üyelere açıktır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    if (uid == null) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-posta") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Şifre (en az 6 karakter)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.signInFamilyAccount(email, password) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Giriş Yap") }
                            OutlinedButton(
                                onClick = { vm.signUpFamilyAccount(email, password) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Hesap Oluştur") }
                        }
                        authError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(signedInEmail.ifBlank { "Aile hesabı" }, fontWeight = FontWeight.Medium)
                                Text(
                                    "UID: ${uid?.take(8)}…",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { vm.signOutFamilyAccount() }) { Text("Çıkış") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (uid == null) {
                HowItWorksCard()
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            // ==================== 2. ÜYELİK DURUMU ====================
            when (membership) {
                is FirebaseSyncService.Membership.Member -> Unit // aşağıda detaylar

                is FirebaseSyncService.Membership.Pending -> {
                    StatusCard(
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        icon = Icons.Default.HourglassTop,
                        title = "Onay bekleniyor",
                        body = "Katılım isteğiniz gönderildi. Mevcut bir aile bireyi onayladığında senkronizasyon otomatik başlar. Bu sırada uygulama yerel olarak çalışır."
                    )
                    Spacer(Modifier.height(12.dp))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Grup ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(groupId, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { vm.requestJoinFamily(groupId, passcode) }, shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Durumu Yenile")
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HowItWorksCard()
                    Spacer(Modifier.height(24.dp))
                    return@Column
                }

                is FirebaseSyncService.Membership.Rejected -> {
                    StatusCard(
                        container = MaterialTheme.colorScheme.errorContainer,
                        icon = Icons.Default.Block,
                        title = "Katılım reddedildi",
                        body = "Bu gruba katılma isteğiniz reddedildi. Aile bireyinizle görüşüp yeniden istek gönderebilirsiniz."
                    )
                    Spacer(Modifier.height(16.dp))
                }

                is FirebaseSyncService.Membership.Error -> {
                    StatusCard(
                        container = MaterialTheme.colorScheme.errorContainer,
                        icon = Icons.Default.ErrorOutline,
                        title = "Hata",
                        body = (membership as FirebaseSyncService.Membership.Error).message
                    )
                    Spacer(Modifier.height(16.dp))
                }

                else -> Unit
            }

            // ==================== 3. GRUP KUR / KATIL ====================
            if (membership !is FirebaseSyncService.Membership.Member &&
                membership !is FirebaseSyncService.Membership.Pending
            ) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("\uD83C\uDD95 Yeni Aile Grubu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Grubu siz kurun. Tahmin edilemez bir grup kimliği üretilir ve mevcut verileriniz aileye taşınır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputPasscode,
                            onValueChange = { inputPasscode = it },
                            label = { Text("Aile Şifresi (en az 4 karakter)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.createFamilyGroup(inputPasscode.trim()) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Aile Grubunu Oluştur")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("\uD83D\uDC65 Var Olan Gruba Katıl", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Davet edildiğiniz grubun ID'sini ve aile şifresini girin. İsteğiniz bir aile bireyi onaylayınca senkron başlar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = joinGroupId,
                                onValueChange = { joinGroupId = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Grup ID") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            if (navController != null) {
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
                        OutlinedTextField(
                            value = inputPasscodeJoin,
                            onValueChange = { inputPasscodeJoin = it },
                            label = { Text("Aile Şifresi") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { vm.requestJoinFamily(joinGroupId.trim(), inputPasscodeJoin.trim()) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Login, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Katılma İsteği Gönder")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ==================== 4. BAĞLI ÜYE ====================
            if (membership is FirebaseSyncService.Membership.Member) {
                // Durum kartı
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
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    when (syncState) {
                                        is FirebaseSyncService.SyncState.Connected -> "Aile üyesisiniz · Bağlı"
                                        is FirebaseSyncService.SyncState.Syncing -> "Senkronize ediliyor: ${(syncState as FirebaseSyncService.SyncState.Syncing).table}"
                                        is FirebaseSyncService.SyncState.Error -> "Hata: ${(syncState as FirebaseSyncService.SyncState.Error).message}"
                                        is FirebaseSyncService.SyncState.Connecting -> "Bağlanıyor..."
                                        else -> "Bağlantı yok"
                                    },
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                                )
                                if (lastSync > 0) {
                                    Text(
                                        "Son senkron: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSync))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Otomatik senkron
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("\u26A1 Otomatik Senkron", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    "Aile bireylerinin yaptığı her değişiklik anında diğer cihazlara yansır.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = autoSyncEnabled, onCheckedChange = { vm.setAutoSync(it) })
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MiniStat("Gönderilen", "$autoPushCount")
                            MiniStat("Bekleyen", "$pendingCount")
                            MiniStat(
                                "Son gönderim",
                                if (lastAutoPush > 0) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastAutoPush)) else "—"
                            )
                        }
                        if (conflictsSolved > 0) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Balance,
                                    null,
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "$conflictsSolved kayıtta çakışma çözüldü · son değiştiren kazandı",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.syncToFirebase() }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Tümünü Gönder")
                    }
                    OutlinedButton(onClick = { vm.disconnectFirebase() }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.CloudOff, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kes")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Onay bekleyen istekler
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\uD83D\uDD14 Katılım İstekleri", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            if (pendingRequests.isNotEmpty()) {
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.error) {
                                    Text(
                                        "${pendingRequests.size}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (pendingRequests.isEmpty()) {
                            Text(
                                "Bekleyen katılım isteği yok.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            pendingRequests.forEach { req ->
                                val matches = vm.passcodeProofMatches(req.passcodeProof)
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(req.name.ifBlank { "Aile bireyi" }, fontWeight = FontWeight.Bold)
                                        Text(
                                            "UID: ${req.uid.take(10)}…",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (matches) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                null,
                                                Modifier.size(16.dp),
                                                tint = if (matches) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                if (matches) "Aile şifresi doğru" else "Aile şifresi eşleşmiyor",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (matches) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { vm.approveJoinRequest(req) },
                                                shape = RoundedCornerShape(8.dp),
                                                enabled = matches
                                            ) { Text("Onayla") }
                                            OutlinedButton(
                                                onClick = { vm.rejectJoinRequest(req.uid) },
                                                shape = RoundedCornerShape(8.dp)
                                            ) { Text("Reddet") }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Davet / paylaşım
                if (groupId.isNotBlank() && passcode.isNotBlank()) {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("\uD83D\uDCE4 Aile Bireylerini Davet Et", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Davet kodunu paylaşın. Aile bireyi kendi hesabıyla giriş yapıp bu kodla katılma isteği gönderir; siz onayladıktan sonra senkron başlar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(10.dp))

                            // QR kod: aile bireyi tek taramayla katılabilir
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                QrCodeView(content = vm.familyInviteCode(), size = 116.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tek tarama ile katılım", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Aile bireyi kendi hesabıyla giriş yapıp bu kodu okutur; grup bilgileri otomatik dolar ve katılma isteği size düşer.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    FilledTonalButton(onClick = { showQr = true }, shape = RoundedCornerShape(10.dp)) {
                                        Icon(Icons.Default.QrCode2, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("QR Kodu Büyüt")
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text(
                                        vm.familyInviteCode(),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { shareToWhatsApp() }, modifier = Modifier.weight(1.2f), shape = RoundedCornerShape(10.dp)) {
                                    Text("\uD83D\uDFE2")
                                    Spacer(Modifier.width(6.dp))
                                    Text("WhatsApp")
                                }
                                OutlinedButton(onClick = { shareInvite() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Paylaş")
                                }
                                OutlinedButton(onClick = { copyInvite() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                    Icon(if (inviteCopied) Icons.Default.Check else Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (inviteCopied) "Kopyalandı" else "Kopyala")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Davet QR kodu (tarama için büyük görünüm)
                if (showQr) {
                    AlertDialog(
                        onDismissRequest = { showQr = false },
                        icon = { Icon(Icons.Default.QrCode2, null) },
                        title = { Text("Aile Davet QR Kodu") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                QrCodeView(content = vm.familyInviteCode(), size = 250.dp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    vm.familyInviteCode(),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Aile bireyi: Senkronizasyon → \"Var Olan Gruba Katıl\" → QR Tara",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = { shareInvite() }) {
                                Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Paylaş")
                            }
                        },
                        dismissButton = { TextButton(onClick = { showQr = false }) { Text("Kapat") } }
                    )
                }

                // Bu cihazı kullanan
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("\uD83D\uDCF1 Bu Cihazı Kullanan", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            currentMember?.let { "${it.name} olarak giriş yapıldı. Yaptığınız değişiklikler bu isimle görünür." }
                                ?: "Henüz seçilmedi. Profil ekranından bu cihazı kullanan aile üyesini seçin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (members.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                members.take(4).forEach { m ->
                                    FilterChip(
                                        selected = vm.myMemberId.value == m.id,
                                        onClick = { vm.setDeviceUser(m) },
                                        label = { Text(m.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (syncError != null) {
                Spacer(Modifier.height(12.dp))
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            syncError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { vm.clearSyncError() }) { Text("Tamam") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HowItWorksCard()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusCard(
    container: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = container)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("\uD83D\uDCA1 Nasıl Çalışır?", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val steps = listOf(
                "1. Her aile bireyi kendi e-posta/şifresiyle aile hesabı açar (gerçek üye kimliği)",
                "2. Kurucu grup oluşturur; tahmin edilemez bir grup kimliği üretilir",
                "3. Diğerleri davet koduyla katılma isteği gönderir",
                "4. Mevcut bir üye isteği onaylar (şifre kontrolüyle birlikte)",
                "5. Onaydan sonra her değişiklik otomatik olarak diğerlerine yayılır",
                "6. Her cihaz veriyi kendi telefonunda saklar; internet olmadan da çalışır"
            )
            steps.forEach { step ->
                Text(step, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "\uD83D\uDD10 Veritabanı kuralları yalnızca onaylı üyelerin UID'lerine izin verir; grubu bilmeyen veya onaylanmayan hiç kimse verilere erişemez.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
