package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

private val ownerColors = listOf(Color(0xFF3498DB), Color(0xFF2ECC71), Color(0xFFF39C12), Color(0xFFE74C3C), Color(0xFF9B59B6))

@Composable
fun MentalLoadScreen(vm: MainViewModel) {
    val tasks by vm.tasks.collectAsState()
    val members by vm.members.collectAsState()

    val memberNames = if (members.isNotEmpty()) members.map { it.name } else listOf("Anne", "Baba", "Çocuk 1")

    val mentalTasks = tasks.map { task ->
        val owner = task.assignee.ifEmpty { memberNames[task.hashCode().toInt().and(0x7FFFFFFF) % memberNames.size] }
        Triple(task.id, task.title, owner)
    }

    PageScaffold("\uD83E\uDDE0", "\uD83E\uDDE0") {
        // Summary cards
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Yük Dağılımı", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                memberNames.take(3).forEachIndexed { idx, name ->
                    val count = mentalTasks.count { it.third == name }
                    val pct = if (mentalTasks.isNotEmpty()) count.toFloat() / mentalTasks.size else 0f
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.width(70.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.weight(1f).height(8.dp),
                            color = ownerColors[idx % ownerColors.size],
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(" ${(pct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Görevler ve Sorumlular", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (mentalTasks.isEmpty()) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83E\uDDE0", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz görev yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Görev ekleyin, zihinsel yük dağılımı otomatik oluşur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(mentalTasks, key = { it.first }) { (id, task, owner) ->
                val ownerIdx = memberNames.indexOf(owner).coerceAtLeast(0)
                val color = ownerColors[ownerIdx % ownerColors.size]
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Text(task, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                            Text(owner, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
