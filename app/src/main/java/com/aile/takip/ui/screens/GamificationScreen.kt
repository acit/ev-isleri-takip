package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.sp
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

private val rankColors = listOf(Color(0xFFFFD700), Color(0xFFC0C0C0), Color(0xFFCD7F32), Color(0xFF3498DB))

@Composable
fun GamificationScreen(vm: MainViewModel) {
    val members by vm.members.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Üye") }
    var showPoints by remember { mutableStateOf<String?>(null) }
    var points by remember { mutableStateOf("") }

    val sorted = members.sortedByDescending { it.points }

    PageScaffold("\uD83C\uDFAE", "\uD83C\uDFAE") {
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
            Text("+ Üye Ekle")
        }
        Spacer(Modifier.height(12.dp))

        if (sorted.isEmpty()) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83C\uDFAE", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz üye eklenmemiş", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(sorted) { i, member ->
                val color = rankColors.getOrElse(i) { Color.Gray }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(18.dp), color = color) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${i + 1}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(member.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${member.points} puan", fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
                        IconButton(onClick = { showPoints = member.id }) {
                            Icon(Icons.Default.Add, "+ Puan", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { vm.deleteMember(member) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                title = { Text("Aile Üyesi Ekle") },
                text = {
                    Column {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("İsim") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Rol") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    if (name.isNotBlank()) { vm.addMember(name, role); name = ""; showAdd = false }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } }
            )
        }

        showPoints?.let { memberId ->
            AlertDialog(
                onDismissRequest = { showPoints = null },
                title = { Text("Puan Ekle") },
                text = { OutlinedTextField(value = points, onValueChange = { points = it }, label = { Text("Puan miktarı") }, singleLine = true) },
                confirmButton = { TextButton(onClick = {
                    val p = points.toIntOrNull() ?: 0
                    val m = members.find { it.id == memberId }
                    if (m != null && p > 0) { vm.addPoints(m, p); points = ""; showPoints = null }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showPoints = null }) { Text("İptal") } }
            )
        }
    }
}
