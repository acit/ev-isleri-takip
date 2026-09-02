package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    
    // Edit form state
    var editName by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Market") }
    var editQuantity by remember { mutableStateOf("1") }
    var editBrand by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editUnitPrice by remember { mutableStateOf("") }
    var editStore by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    
    // Barcode lookup state
    val isLookingUp by vm.isLookingUpBarcode
    val lookupResult by vm.lastLookupResult
    var showLookupResult by remember { mutableStateOf(false) }

    // Handle scan result
    val scanResult by vm.lastScanResult
    LaunchedEffect(scanResult) {
        if (scanResult != null) {
            vm.lookupBarcode(scanResult ?: "")
            vm.lastScanResult.value = null
        }
    }
    
    // Handle lookup result
    LaunchedEffect(lookupResult) {
        lookupResult?.let { result ->
            if (result.found) {
                newItem = result.name
                category = result.category.ifEmpty { "Genel" }
                showLookupResult = true
            } else if (result.barcode.isNotEmpty()) {
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
                    Text("\u0130steniyor...", style = MaterialTheme.typography.bodySmall)
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
                        Text("\u00dcr\u00fcn bulundu!", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("Otomatik dolduruldu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showLookupResult = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Kapat", modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Add new item
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = newItem, onValueChange = { newItem = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("\u00dcr\u00fcn ekle...") }, singleLine = true, shape = RoundedCornerShape(10.dp))
            if (navController != null) {
                FilledIconButton(onClick = { navController.navigate("scanner/barcode") },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.QrCodeScanner, "Tara")
                }
            }
            FilledIconButton(onClick = {
                if (newItem.isNotBlank()) { vm.addShoppingItem(newItem, category = category); newItem = ""; showLookupResult = false }
            }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.Add, "Ekle")
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Market", "Gida", "Temizlik", "Diger").forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
            }
        }
        Spacer(Modifier.height(12.dp))

        // Unchecked items
        if (unchecked.isNotEmpty()) {
            Text("\uD83D\uDED2 Al\u0131nacaklar (${unchecked.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(unchecked, key = { it.id }) { item ->
                ShoppingItemCard(item = item, onChecked = { vm.toggleShoppingItem(item) }, onEdit = {
                    editingItem = item; editName = item.name; editCategory = item.category
                    editQuantity = item.quantity.toString(); editBrand = item.brand
                    editDescription = item.description; editUnitPrice = if (item.unitPrice > 0) item.unitPrice.toString() else ""
                    editStore = item.store; editNotes = item.notes; showDialog = true
                }, onDelete = { vm.deleteShopping(item) })
            }
            if (checked.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)); Text("\u2705 Al\u0131nanlar (${checked.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(6.dp)) }
            }
            items(checked, key = { it.id }) { item ->
                ShoppingItemCard(item = item, onChecked = { vm.toggleShoppingItem(item) }, onEdit = {
                    editingItem = item; editName = item.name; editCategory = item.category
                    editQuantity = item.quantity.toString(); editBrand = item.brand
                    editDescription = item.description; editUnitPrice = if (item.unitPrice > 0) item.unitPrice.toString() else ""
                    editStore = item.store; editNotes = item.notes; showDialog = true
                }, onDelete = { vm.deleteShopping(item) })
            }
        }
    }

    // Edit Dialog
    if (showDialog && editingItem != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false; editingItem = null },
            title = { Text("\u00dcr\u00fcn\u00fc D\u00fczenle") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("\u00dcr\u00fcn Ad\u0131") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editBrand, onValueChange = { editBrand = it }, label = { Text("Marka") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("A\u00e7\u0131klama") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editQuantity, onValueChange = { editQuantity = it }, label = { Text("Miktar") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = editUnitPrice, onValueChange = { editUnitPrice = it }, label = { Text("\u20BA/Birim") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = editStore, onValueChange = { editStore = it }, label = { Text("Ma\u011faza") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editNotes, onValueChange = { editNotes = it }, label = { Text("Notlar") }, modifier = Modifier.fillMaxWidth())
                    
                    // Show barcode if exists
                    editingItem?.let { item ->
                        if (item.barcode.isNotEmpty()) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                                    Icon(Icons.Default.QrCode, "Barkod", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Barkod: ${item.barcode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    
                    // Category chips
                    Text("Kategori:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Market", "Gida", "Temizlik", "Diger").forEach { c ->
                            FilterChip(selected = editCategory == c, onClick = { editCategory = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingItem?.let { item ->
                        val qty = editQuantity.toIntOrNull() ?: 1
                        val price = editUnitPrice.toDoubleOrNull() ?: 0.0
                        vm.updateShoppingItem(item.copy(
                            name = editName, category = editCategory, quantity = qty,
                            brand = editBrand, description = editDescription,
                            unitPrice = price, totalPrice = price * qty,
                            store = editStore, notes = editNotes
                        ))
                    }
                    showDialog = false; editingItem = null
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false; editingItem = null }) { Text("\u0130ptal") } }
        )
    }
}

@Composable
fun ShoppingItemCard(item: ShoppingItem, onChecked: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isChecked = item.checked
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isChecked, onCheckedChange = { onChecked() })
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Medium,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    
                    // Brand and category
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${item.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.brand.isNotEmpty()) {
                            Text("\u00B7 ${item.brand}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("\u00B7 x${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    // Price info
                    if (item.unitPrice > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\u20BA${item.totalPrice.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            if (item.store.isNotEmpty()) {
                                Text(" \u00B7 ${item.store}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "D\u00fczenle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
            
            // Description (if exists)
            if (item.description.isNotEmpty() && !isChecked) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            
            // Notes (if exists)
            if (item.notes.isNotEmpty() && !isChecked) {
                Text("\uD83D\uDCDD ${item.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
