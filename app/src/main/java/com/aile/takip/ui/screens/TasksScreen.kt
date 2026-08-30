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
import com.aile.takip.data.model.Task
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

private val priorityColors = mapOf("düşük" to Color(0xFF2ECC71), "orta" to Color(0xFFF39C12), "yüksek" to Color(0xFFE74C3C))

@Composable
fun TasksScreen(vm: MainViewModel) {
    val tasks by vm.tasks.collectAsState()
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("orta") }
    var assignee by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    PageScaffold("Görevler", "📋") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Yeni görev...") }, singleLine = true, shape = RoundedCornerShape(10.dp))
            FilledIconButton(onClick = { if (title.isNotBlank()) { vm.addTask(title, priority = priority, assignee = assignee); title = ""; assignee = "" } },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.Add, "Ekle")
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("düşük", "orta", "yüksek").forEach { p ->
                FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p, style = MaterialTheme.typography.labelSmall) })
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = assignee, onValueChange = { assignee = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Sorumlu (isteğe bağlı)...") }, singleLine = true, shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tasks, key = { it.id }) { task ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(4.dp, 32.dp), color = priorityColors[task.priority] ?: Color.Gray, shape = RoundedCornerShape(2.dp)) {}
                        Spacer(Modifier.width(12.dp))
                        Checkbox(checked = task.status == "tamamlanan", onCheckedChange = { vm.toggleTask(task) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.Medium,
                                color = if (task.status == "tamamlanan") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            if (task.assignee.isNotEmpty()) Text("👤 ${task.assignee}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.deleteTask(task) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
