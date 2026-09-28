package com.aile.takip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aile.takip.data.model.FamilyMember
import com.aile.takip.ui.components.MemberAvatar
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.components.parseHexColor
import com.aile.takip.ui.viewmodel.MainViewModel

/** Üye avatarı için hazır renk paleti. */
private val memberColors = listOf(
    "#E91E63", "#9C27B0", "#3F51B5", "#2196F3", "#00BCD4",
    "#4CAF50", "#8BC34A", "#FF9800", "#795548", "#607D8B"
)

/** Hazır rol şablonları. */
private val rolePresets = listOf("Anne", "Baba", "Kız", "Oğul", "Büyükanne", "Büyükbaba", "Kardeş", "Diğer")

private fun displayRole(role: String): String =
    role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@Composable
fun FamilyMembersScreen(vm: MainViewModel, navController: NavController? = null) {
    val members by vm.members.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val shopping by vm.shoppingItems.collectAsState()
    val myMemberId = vm.myMemberId.value

    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<FamilyMember?>(null) }
    var pointsTarget by remember { mutableStateOf<FamilyMember?>(null) }
    var deleteTarget by remember { mutableStateOf<FamilyMember?>(null) }

    val filtered = if (query.isBlank()) members else members.filter {
        it.name.contains(query, ignoreCase = true) || it.role.contains(query, ignoreCase = true)
    }

    PageScaffold("\uD83D\uDC65 Aile Üyeleri (${members.size})", "\uD83D\uDC65") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { showAdd = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(6.dp))
                Text("Üye Ekle")
            }
            OutlinedButton(
                onClick = { navController?.navigate("sync") },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudSync, null)
                Spacer(Modifier.width(6.dp))
                Text("Senkron")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (members.size > 4) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Üye ara…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        if (members.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("\uD83D\uDC65", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz aile üyesi yok", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Aile bireylerinizi ekleyerek görevleri, alışverişi ve bütçeyi paylaşabilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filtered.isEmpty()) {
            Text(
                "\"$query\" için sonuç bulunamadı",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { member ->
                val taskCount = tasks.count { it.assignee.equals(member.name, ignoreCase = true) }
                val shoppingCount = shopping.count { it.addedBy.equals(member.name, ignoreCase = true) }
                val isMe = member.id == myMemberId

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MemberAvatar(member.name, member.color, 48.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(member.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (isMe) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "Bu cihaz",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                displayRole(member.role),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "$taskCount görev · $shoppingCount alışveriş",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${member.points}",
                                fontWeight = FontWeight.Bold,
                                color = parseHexColor(member.color),
                                fontSize = 16.sp
                            )
                            Text(
                                "puan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { pointsTarget = member }) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Puan")
                        }
                        TextButton(onClick = { if (!isMe) vm.setDeviceUser(member) else vm.setDeviceUser(null) }) {
                            Icon(
                                if (isMe) Icons.Default.CheckCircle else Icons.Default.PhoneAndroid,
                                null,
                                Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isMe) "Bu cihaz" else "Bu cihazda kullan")
                        }
                        IconButton(onClick = { editTarget = member }) {
                            Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { deleteTarget = member }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("\uD83D\uDCA1 İpucu", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bir üyeyi \"Bu cihazda kullan\" olarak seçtiğinizde, o cihazda yapılan değişiklikler bu isimle görünür ve aile bireyleri kimin ne yaptığını görebilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    // ============ EKLE / DÜZENLE ============
    if (showAdd || editTarget != null) {
        val existing = editTarget
        MemberEditorDialog(
            existing = existing,
            onDismiss = { showAdd = false; editTarget = null },
            onSave = { name, role, color ->
                if (existing == null) {
                    vm.addMember(name, role, color)
                } else {
                    vm.updateMember(existing.copy(name = name, role = role, color = color))
                }
                showAdd = false
                editTarget = null
            }
        )
    }

    // ============ PUAN ============
    pointsTarget?.let { member ->
        AddPointsDialog(
            member = member,
            onDismiss = { pointsTarget = null },
            onConfirm = { amount ->
                vm.addPoints(member, amount)
                pointsTarget = null
            }
        )
    }

    // ============ SİLME ONAYI ============
    deleteTarget?.let { member ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Üyeyi sil") },
            text = { Text("\"${member.name}\" aile listesinden silinsin mi? Bu işlem diğer cihazlara da yansır.") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteMember(member); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("İptal") } }
        )
    }
}

@Composable
private fun MemberEditorDialog(
    existing: FamilyMember?,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var role by remember { mutableStateOf(existing?.role?.replaceFirstChar { it.titlecase() } ?: "Anne") }
    var customRole by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(existing?.color ?: memberColors.first()) }

    val effectiveRole = if (role == "Diğer") customRole.ifBlank { "Üye" } else role
    val canSave = name.isNotBlank() && effectiveRole.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (existing == null) Icons.Default.PersonAdd else Icons.Default.Edit, null) },
        title = { Text(if (existing == null) "Aile Üyesi Ekle" else "Üyeyi Düzenle") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                // Canlı önizleme
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemberAvatar(name.ifBlank { "?" }, color, 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(name.ifBlank { "Yeni üye" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            effectiveRole,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsim") },
                    placeholder = { Text("örn: Ayşe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(14.dp))
                Text("Rol", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(rolePresets) { preset ->
                        FilterChip(
                            selected = role == preset,
                            onClick = { role = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                if (role == "Diğer") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customRole,
                        onValueChange = { customRole = it },
                        label = { Text("Rol adı") },
                        placeholder = { Text("örn: Hala, Teyze, Kuzen") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text("Avatar rengi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(memberColors) { hex ->
                        val selected = color == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim(), effectiveRole.trim(), color) }, enabled = canSave) {
                Text(if (existing == null) "Ekle" else "Kaydet")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun AddPointsDialog(member: FamilyMember, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val quick = listOf(5, 10, 25, 50)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Star, null, tint = parseHexColor(member.color)) },
        title = { Text("${member.name} — Puan Ekle") },
        text = {
            Column {
                Text(
                    "Mevcut: ${member.points} puan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Puan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quick.forEach { q ->
                        OutlinedButton(
                            onClick = { amount = ((amount.toIntOrNull() ?: 0) + q).toString() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text("+$q") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount.toIntOrNull() ?: 0) },
                enabled = (amount.toIntOrNull() ?: 0) > 0
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
