package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import java.text.SimpleDateFormat
import java.util.*

private val priorityColors = mapOf("düşük" to Color(0xFF2ECC71), "orta" to Color(0xFFF39C12), "yüksek" to Color(0xFFE74C3C))
private val categoryIcons = mapOf("Genel" to "📋", "Alışveriş" to "🛒", "Temizlik" to "🧹", "Sağlık" to "💊", "Eğitim" to "📚", "İş" to "💼")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: MainViewModel) {
    val tasks by vm.tasks.collectAsState()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Genel") }
    var priority by remember { mutableStateOf("orta") }
    var assignee by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    PageScaffold("Görevler", "📋") {
        // Add new task section
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Yeni görev...") }, singleLine = true, shape = RoundedCornerShape(10.dp))
            FilledIconButton(onClick = { 
                if (title.isNotBlank()) { 
                    vm.addTask(title, description = description, category = category, priority = priority, assignee = assignee, dueDate = dueDate)
                    title = ""
                    description = ""
                    assignee = ""
                    dueDate = ""
                } 
            },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.Add, "Ekle")
            }
        }
        
        Spacer(Modifier.height(6.dp))
        
        // Priority chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("düşük", "orta", "yüksek").forEach { p ->
                FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p, style = MaterialTheme.typography.labelSmall) })
            }
        }
        
        Spacer(Modifier.height(6.dp))
        
        // Category chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categoryIcons.forEach { (cat, icon) ->
                FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text("$icon $cat", style = MaterialTheme.typography.labelSmall) })
            }
        }
        
        Spacer(Modifier.height(6.dp))
        
        // Assignee and due date row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = assignee, onValueChange = { assignee = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Sorumlu...") }, singleLine = true, shape = RoundedCornerShape(10.dp))
            
            OutlinedTextField(
                value = if (dueDate.isNotEmpty()) dueDate else "", 
                onValueChange = {}, 
                modifier = Modifier.weight(1f),
                placeholder = { Text("Bitiş tarihi...") }, 
                singleLine = true, 
                shape = RoundedCornerShape(10.dp),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, "Tarih Seç")
                    }
                }
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Task list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tasks, key = { it.id }) { task ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(4.dp, 32.dp), color = priorityColors[task.priority] ?: Color.Gray, shape = RoundedCornerShape(2.dp)) {}
                        Spacer(Modifier.width(12.dp))
                        Checkbox(checked = task.status == "tamamlanan", onCheckedChange = { vm.toggleTask(task) })
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(categoryIcons[task.category] ?: "📋", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(4.dp))
                                Text(task.title, fontWeight = FontWeight.Medium,
                                    color = if (task.status == "tamamlanan") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (task.assignee.isNotEmpty()) {
                                    Text("👤 ${task.assignee}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (task.dueDate.isNotEmpty()) {
                                    Text("📅 ${task.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (task.description.isNotEmpty()) {
                                Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        IconButton(onClick = { 
                            editingTask = task
                            title = task.title
                            description = task.description
                            category = task.category
                            priority = task.priority
                            assignee = task.assignee
                            dueDate = task.dueDate
                            showDialog = true 
                        }) {
                            Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { vm.deleteTask(task) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        dueDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("Seç")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Edit Task Dialog
    if (showDialog && editingTask != null) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                editingTask = null
                title = ""
                description = ""
                category = "Genel"
                priority = "orta"
                assignee = ""
                dueDate = ""
            },
            title = { Text("Görevi Düzenle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = assignee, onValueChange = { assignee = it }, label = { Text("Sorumlu") }, modifier = Modifier.fillMaxWidth())
                    
                    // Category selector
                    Text("Kategori:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        categoryIcons.forEach { (cat, icon) ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(icon, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    
                    // Priority selector
                    Text("Öncelik:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("düşük", "orta", "yüksek").forEach { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    
                    // Due date picker
                    Text("Bitiş Tarihi:", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = dueDate, 
                        onValueChange = {}, 
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tarih seçmek için tıklayın") }, 
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, "Tarih Seç")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingTask?.let { task ->
                        vm.updateTask(task.copy(
                            title = title,
                            description = description,
                            category = category,
                            priority = priority,
                            assignee = assignee,
                            dueDate = dueDate
                        ))
                    }
                    showDialog = false
                    editingTask = null
                    title = ""
                    description = ""
                    category = "Genel"
                    priority = "orta"
                    assignee = ""
                    dueDate = ""
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    editingTask = null
                    title = ""
                    description = ""
                    category = "Genel"
                    priority = "orta"
                    assignee = ""
                    dueDate = ""
                }) {
                    Text("İptal")
                }
            }
        )
    }
}
