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
import com.aile.takip.data.model.InventoryItem
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

@Composable
fun InventoryScreen(vm: MainViewModel) {
    val items by vm.inventory.collectAsState()
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf("Genel") }
    var unit by remember { mutableStateOf("adet") }

    PageScaffold("Envanter", "📦") {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Ürün Ekle", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ürün adı") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Miktar") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Birim") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Genel", "Gıda", "Temizlik", "Kişisel").forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (name.isNotBlank()) { vm.addInventory(name, category, quantity.toIntOrNull() ?: 1, unit); name = ""; quantity = "1" } },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) { Text("+ Ekle") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Stok: ${items.size} ürün", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(items, key = { it.id }) { item ->
                val color = when { item.quantity <= item.minStock -> Color(0xFFE74C3C); item.quantity <= item.minStock * 2 -> Color(0xFFF39C12); else -> Color(0xFF2ECC71) }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(4.dp, 32.dp), color = color, shape = RoundedCornerShape(2.dp)) {}
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Medium)
                            Text("${item.category} · ${item.location.ifEmpty { "Yer belirtilmedi" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${item.quantity} ${item.unit}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { vm.deleteInventory(item) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
