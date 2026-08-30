package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

private val symptoms = listOf("Kramp", "Baş ağrısı", "Şişkinlik", "Yorgunluk", "Meme hassasiyeti", "Akne", "Uyku sorunu", "İştah artışı")
private val moods = listOf("\uD83D\uDE0A Mutlu", "\uD83D\uDE10 Nötr", "\uD83D\uDE14 Üzgün", "\uD83D\uDE21 Stresli", "\uD83E\uDD14 Sinirli", "\uD83D\uDE0D Enerjik")
private val flows = listOf("Hafif", "Orta", "Ağır")

@Composable
fun MenstrualCycleScreen(vm: MainViewModel) {
    val cycles by vm.menstrualCycles.collectAsState()
    val members by vm.members.collectAsState()
    val femaleMembers = members.filter { !it.name.lowercase().contains("baba") }
    var showAdd by remember { mutableStateOf(false) }
    var selectedMemberId by remember { mutableStateOf("") }
    var cycleLength by remember { mutableStateOf("28") }
    var periodLength by remember { mutableStateOf("5") }
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var selectedMood by remember { mutableStateOf("") }
    var selectedFlow by remember { mutableStateOf("Orta") }
    var isPersonal by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    val filteredCycles = if (selectedMemberId.isNotEmpty()) cycles.filter { it.memberId == selectedMemberId } else cycles
    val lastCycle = filteredCycles.maxByOrNull { it.startDate }

    // Calculate next period
    val nextPeriod = lastCycle?.let {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.time = sdf.parse(it.startDate) ?: Date()
            cal.add(Calendar.DAY_OF_YEAR, it.cycleLength)
            sdf.format(cal.time)
        } catch (_: Exception) { "?" }
    }

    // Days since last period
    val daysSinceLast = lastCycle?.let {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = sdf.parse(it.startDate) ?: Date()
            val diff = (System.currentTimeMillis() - start.time) / (1000 * 60 * 60 * 24)
            diff.toInt()
        } catch (_: Exception) { 0 }
    }

    PageScaffold("\uD83D\uDC95 Döngü Takibi", "\uD83D\uDC95") {
        // Member selector
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selected = selectedMemberId.isEmpty(), onClick = { selectedMemberId = "" }, label = { Text("Tümü", style = MaterialTheme.typography.labelSmall) })
            femaleMembers.take(4).forEach { m ->
                FilterChip(selected = selectedMemberId == m.id, onClick = { selectedMemberId = m.id }, label = { Text(m.name.take(6), style = MaterialTheme.typography.labelSmall) })
            }
        }

        Spacer(Modifier.height(12.dp))

        // Status card
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (lastCycle != null) {
                    val isOnPeriod = daysSinceLast != null && daysSinceLast <= (lastCycle.periodLength)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(if (isOnPeriod) "\uD83D\uDD34 Adet Döneminde" else "\uD83D\uDD35 Döngüde", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize, color = if (isOnPeriod) Color(0xFFE74C3C) else MaterialTheme.colorScheme.primary)
                            Text("Son adet: ${lastCycle.startDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = if (isOnPeriod) Color(0xFFE74C3C).copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer) {
                            Text("${daysSinceLast ?: 0}. gün", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = if (isOnPeriod) Color(0xFFE74C3C) else MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Progress ring visualization
                    val cycleProgress = (daysSinceLast?.toFloat()?.div(lastCycle.cycleLength) ?: 0f).coerceIn(0f, 1f)
                    Column {
                        Text("Döngü İlerlemesi", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { cycleProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (isOnPeriod) Color(0xFFE74C3C) else Color(0xFFE91E63),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Adet başlangıcı", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Sonraki: $nextPeriod", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Cycle info
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${lastCycle.cycleLength}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Döngü Süresi", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${lastCycle.periodLength}", fontWeight = FontWeight.Bold, color = Color(0xFFE74C3C))
                                Text("Adet Süresi", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Last symptoms
                    if (lastCycle.symptoms.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Son Semptomlar:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            lastCycle.symptoms.split(",").take(4).forEach { s ->
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                    Text(s.trim(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }

                    // Privacy toggle
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (lastCycle.isPersonal) Icons.Default.Delete else Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(if (lastCycle.isPersonal) "\uD83D\uDD12 Kişisel (sadece ben)" else "\uD83D\uDC65 Aileyle paylaşılıyor", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text("Henüz döngü kaydı yok", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) { Text("+ Yeni Döngü Kaydı") }

        // History
        Spacer(Modifier.height(12.dp))
        Text("Geçmiş Döngüler", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(filteredCycles, key = { it.id }) { cycle ->
                val memberName = members.find { it.id == cycle.memberId }?.name ?: "Bilinmiyor"
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(memberName, fontWeight = FontWeight.SemiBold)
                                if (cycle.isPersonal) {
                                    Spacer(Modifier.width(4.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("\uD83D\uDD12", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Text("${cycle.startDate} \u00B7 ${cycle.cycleLength}g döngü \u00B7 ${cycle.periodLength}g adet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (cycle.symptoms.isNotEmpty()) Text("\uD83D\uDE14 ${cycle.symptoms}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            if (cycle.mood.isNotEmpty()) Text(cycle.mood, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { vm.deleteCycle(cycle) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Add Dialog
        if (showAdd) {
            AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Döngü Kaydı") },
                text = {
                    Column {
                        if (femaleMembers.isNotEmpty()) {
                            Text("Üye:", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                femaleMembers.take(4).forEach { m ->
                                    FilterChip(selected = selectedMemberId == m.id, onClick = { selectedMemberId = m.id }, label = { Text(m.name.take(6), style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        OutlinedTextField(value = cycleLength, onValueChange = { cycleLength = it }, label = { Text("Döngü süresi (gün)") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = periodLength, onValueChange = { periodLength = it }, label = { Text("Adet süresi (gün)") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        Text("Semptomlar:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            symptoms.take(4).forEach { s ->
                                FilterChip(selected = s in selectedSymptoms, onClick = { selectedSymptoms = if (s in selectedSymptoms) selectedSymptoms - s else selectedSymptoms + s }, label = { Text(s, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Ruh Hali:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            moods.take(3).forEach { m ->
                                FilterChip(selected = selectedMood == m, onClick = { selectedMood = m }, label = { Text(m, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kişisel:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                            Switch(checked = isPersonal, onCheckedChange = { isPersonal = it })
                            Text(if (isPersonal) "\uD83D\uDD12" else "\uD83D\uDC65")
                        }
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    vm.addCycle(selectedMemberId, today, cycleLength.toIntOrNull() ?: 28, periodLength.toIntOrNull() ?: 5, selectedSymptoms.joinToString(","), selectedMood, selectedFlow, isPersonal, notes)
                    showAdd = false; selectedSymptoms = emptySet(); selectedMood = ""
                }) { Text("Kaydet") } },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } }
            )
        }
    }
}
