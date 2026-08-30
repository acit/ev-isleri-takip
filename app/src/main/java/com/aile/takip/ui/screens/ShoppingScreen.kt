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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

@Composable
fun ShoppingScreen(vm: MainViewModel) {
    val items by vm.shoppingItems.collectAsState()
    var newItem by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Market") }

    val unchecked = items.filter { !it.checked }
    val checked = items.filter { it.checked }

    PageScaffold("\uD83D\uDED2", "\uD83D\uDED2") {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newItem, onValueChange = { newItem = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Ürün ekle...") }, singleLine = true, shape = RoundedCornerShape(10.dp))
            FilledIconButton(onClick = {
                if (newItem.isNotBlank()) { vm.addShoppingItem(newItem, category = category); newItem = "" }
            }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.Add, "Ekle")
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Market", "Gıda", "Temizlik", "Diğer").forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
            }
        }
        Spacer(Modifier.height(12.dp))

        if (unchecked.isNotEmpty()) {
            Text("\uD83D\uDED2 Alınacaklar (${unchecked.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(unchecked, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = false, onCheckedChange = { vm.toggleShoppingItem(item) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Medium)
                            Text("${item.category} \u00B7 x${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.deleteShopping(item) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (checked.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("\u2705 Alınanlar (${checked.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                }
            }
            items(checked, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = true, onCheckedChange = { vm.toggleShoppingItem(item) })
                        Text(item.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { vm.deleteShopping(item) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
