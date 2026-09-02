package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aile.takip.data.model.ShoppingItem
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

@Composable
fun ShoppingScreen(vm: MainViewModel, navController: NavController? = null) {
    val items by vm.shoppingItems.collectAsState()
    var newItem by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Market") }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Market") }
    var editQuantity by remember { mutableStateOf("1") }
    
    // Barcode lookup state
    val isLookingUp by vm.isLookingUpBarcode
    val lookupResult by vm.lastLookupResult
    var showLookupResult by remember { mutableStateOf(false) }

    // Handle scan result from barcode scanner
    val scanResult by vm.lastScanResult
    LaunchedEffect(scanResult) {
        if (scanResult != null) {
            val barcode = scanResult ?: ""
            // Start barcode lookup
            vm.lookupBarcode(barcode)
            vm.lastScanResult.value = null
        }
    }
    
    // Handle lookup result
    LaunchedEffect(lookupResult) {
        lookupResult?.let { result ->
            if (result.found) {
                // Product found - auto-fill
                newItem = result.name
                category = result.category.ifEmpty { "Genel" }
                showLookupResult = true
            } else if (result.barcode.isNotEmpty()) {
                // Product not found - ask user
                newItem = result.barcode
                showLookupResult = true
            }
            vm.clearLookupResult()
        }
    }

    val unchecked = items.filter { !it.checked }
    val checked = items.filter { it.checked }

    PageScaffold("\uD83D\uDED2", "\uD83D\uDED2") {
        // Barcode lookup indicator
        if (isLookingUp) {
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Barkod aranıyor...", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        
        // Lookup result indicator
        if (showLookupResult && lookupResult == null) {
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, "Bulundu", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ürün bulundu!", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("Otomatik dolduruldu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showLookupResult = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Kapat", modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Add new item section
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newItem, onValueChange = { newItem = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Ürün ekle...") }, singleLine = true, shape = RoundedCornerShape(10.dp))
            // QR/Barkod tarama butonu
            if (navController != null) {
                FilledIconButton(
                    onClick = { navController.navigate("scanner/barcode") },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.QrCodeScanner, "Tara")
                }
            }
            FilledIconButton(onClick = {
                if (newItem.isNotBlank()) { 
                    vm.addShoppingItem(newItem, category = category)
                    newItem = ""
                    showLookupResult = false
                }
            }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.Add, "Ekle")
            }
        }
        
        Spacer(Modifier.height(6.dp))
        
        // Category chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Market", "Gıda", "Temizlik", "Diğer").forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
            }
        }
        
        Spacer(Modifier.height(12.dp))

        // Unchecked items
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
                        IconButton(onClick = { 
                            editingItem = item
                            editName = item.name
                            editCategory = item.category
                            editQuantity = item.quantity.toString()
                            showDialog = true 
                        }) {
                            Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { vm.deleteShopping(item) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            // Checked items section
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${item.category} \u00B7 x${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { 
                            editingItem = item
                            editName = item.name
                            editCategory = item.category
                            editQuantity = item.quantity.toString()
                            showDialog = true 
                        }) {
                            Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { vm.deleteShopping(item) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    // Edit Shopping Item Dialog
    if (showDialog && editingItem != null) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                editingItem = null
                editName = ""
                editCategory = "Market"
                editQuantity = "1"
            },
            title = { Text("Ürünü Düzenle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Ürün Adı") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editQuantity, onValueChange = { editQuantity = it }, label = { Text("Miktar") }, modifier = Modifier.fillMaxWidth())
                    
                    // Category selector
                    Text("Kategori:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Market", "Gıda", "Temizlik", "Diğer").forEach { c ->
                            FilterChip(selected = editCategory == c, onClick = { editCategory = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingItem?.let { item ->
                        val qty = editQuantity.toIntOrNull() ?: 1
                        vm.updateShoppingItem(item.copy(
                            name = editName,
                            category = editCategory,
                            quantity = qty
                        ))
                    }
                    showDialog = false
                    editingItem = null
                    editName = ""
                    editCategory = "Market"
                    editQuantity = "1"
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    editingItem = null
                    editName = ""
                    editCategory = "Market"
                    editQuantity = "1"
                }) {
                    Text("İptal")
                }
            }
        )
    }
}
