package com.aile.takip.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aile.takip.data.model.InventoryItem
import com.aile.takip.data.model.PriceRecord
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel
import com.aile.takip.utils.AttachmentHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(vm: MainViewModel) {
    val items by vm.inventory.collectAsState()
    val context = LocalContext.current
    
    // Add form state
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf("Genel") }
    var unit by remember { mutableStateOf("adet") }
    var minStock by remember { mutableStateOf("0") }
    var location by remember { mutableStateOf("") }
    
    // Edit state
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPriceHistoryDialog by remember { mutableStateOf(false) }
    var showAddPriceDialog by remember { mutableStateOf(false) }
    
    // Image picker
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    // Price form
    var priceAmount by remember { mutableStateOf("") }
    var priceStore by remember { mutableStateOf("") }
    var priceNotes by remember { mutableStateOf("") }

    PageScaffold("Envanter", "📦") {
        // Add new item card
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Ürün Ekle", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                
                // Name and image row
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Ürün adı") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.CameraAlt, "Resim Ekle", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                // Show selected image preview
                selectedImageUri?.let { uri ->
                    Spacer(Modifier.height(8.dp))
                    Card(shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Seçilen resim", style = MaterialTheme.typography.labelSmall)
                            // Image would be loaded here with Coil
                        }
                    }
                }
                
                Spacer(Modifier.height(6.dp))
                
                // Quantity, unit, min stock row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Miktar") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Birim") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = minStock, onValueChange = { minStock = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("Min Stok") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                }
                
                Spacer(Modifier.height(6.dp))
                
                // Location
                OutlinedTextField(value = location, onValueChange = { location = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Konum (buzdolabı, kiler vb.)") }, singleLine = true, shape = RoundedCornerShape(10.dp))
                
                Spacer(Modifier.height(6.dp))
                
                // Category chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Genel", "Gıda", "Temizlik", "Kişisel", "Sağlık").forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                Button(onClick = { 
                    if (name.isNotBlank()) { 
                        val imageBase64 = selectedImageUri?.let { uri -> 
                            AttachmentHelper.uriToBase64(context, uri) 
                        } ?: ""
                        vm.addInventory(
                            name, category, quantity.toIntOrNull() ?: 1, unit, 
                            minStock.toIntOrNull() ?: 0, location, imageBase64
                        )
                        name = ""; quantity = "1"; unit = "adet"; minStock = "0"; location = ""
                        selectedImageUri = null
                    } 
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) { 
                    Text("+ Ekle") 
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Stats
        Text("Stok: ${items.size} ürün • Düşük stok: ${items.count { it.minStock > 0 && it.quantity <= it.minStock }}", 
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(Modifier.height(8.dp))
        
        // Inventory list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(items, key = { it.id }) { item ->
                val color = when { 
                    item.quantity <= item.minStock -> Color(0xFFE74C3C)
                    item.quantity <= item.minStock * 2 -> Color(0xFFF39C12) 
                    else -> Color(0xFF2ECC71) 
                }
                
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Stock indicator
                        Surface(modifier = Modifier.size(4.dp, 32.dp), color = color, shape = RoundedCornerShape(2.dp)) {}
                        Spacer(Modifier.width(12.dp))
                        
                        // Item info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, fontWeight = FontWeight.Medium)
                                if (item.imageBase64.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Image, "Resimli", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text("${item.category} · ${item.location.ifEmpty { "Yer belirtilmedi" }}", 
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // Price and store info
                            if (item.lastPrice > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💰 \u20BA${item.lastPrice.toInt()}", 
                                        style = MaterialTheme.typography.bodySmall, 
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium)
                                    if (item.lastStore.isNotEmpty()) {
                                        Text(" · ${item.lastStore}", 
                                            style = MaterialTheme.typography.bodySmall, 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (item.lastPurchaseDate.isNotEmpty()) {
                                        Text(" · ${item.lastPurchaseDate}", 
                                            style = MaterialTheme.typography.bodySmall, 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        
                        // Quantity
                        Text("${item.quantity} ${item.unit}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        
                        // Action buttons
                        IconButton(onClick = { 
                            editingItem = item
                            name = item.name
                            quantity = item.quantity.toString()
                            category = item.category
                            unit = item.unit
                            minStock = item.minStock.toString()
                            location = item.location
                            showEditDialog = true 
                        }) {
                            Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        // Price history button (if has price history)
                        if (item.priceHistory.isNotEmpty()) {
                            IconButton(onClick = { 
                                editingItem = item
                                showPriceHistoryDialog = true 
                            }) {
                                Icon(Icons.Default.ShowChart, "Fiyat Grafiği", tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        
                        // Add price button
                        IconButton(onClick = { 
                            editingItem = item
                            showAddPriceDialog = true 
                        }) {
                            Icon(Icons.Default.AttachMoney, "Fiyat Ekle", tint = MaterialTheme.colorScheme.secondary)
                        }
                        
                        IconButton(onClick = { vm.deleteInventory(item) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    // Edit Item Dialog
    if (showEditDialog && editingItem != null) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false
                editingItem = null
                name = ""; quantity = "1"; category = "Genel"; unit = "adet"; minStock = "0"; location = ""
            },
            title = { Text("Ürünü Düzenle") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ürün Adı") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Miktar") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Birim") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Min Stok") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Konum") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    
                    // Category selector
                    Text("Kategori:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Genel", "Gıda", "Temizlik", "Kişisel", "Sağlık").forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    
                    // Image picker
                    Spacer(Modifier.height(8.dp))
                    Text("Ürün Resmi:", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.CameraAlt, "Resim Seç", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Resim değiştirmek için tıklayın", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingItem?.let { item ->
                        val imageBase64 = selectedImageUri?.let { uri -> 
                            AttachmentHelper.uriToBase64(context, uri) 
                        } ?: item.imageBase64
                        vm.updateInventory(item.copy(
                            name = name,
                            quantity = quantity.toIntOrNull() ?: 1,
                            category = category,
                            unit = unit,
                            minStock = minStock.toIntOrNull() ?: 0,
                            location = location,
                            imageBase64 = imageBase64
                        ))
                    }
                    showEditDialog = false
                    editingItem = null
                    name = ""; quantity = "1"; category = "Genel"; unit = "adet"; minStock = "0"; location = ""
                    selectedImageUri = null
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditDialog = false
                    editingItem = null
                    name = ""; quantity = "1"; category = "Genel"; unit = "adet"; minStock = "0"; location = ""
                    selectedImageUri = null
                }) {
                    Text("İptal")
                }
            }
        )
    }
    
    // Add Price Dialog
    if (showAddPriceDialog && editingItem != null) {
        AlertDialog(
            onDismissRequest = { 
                showAddPriceDialog = false
                editingItem = null
                priceAmount = ""; priceStore = ""; priceNotes = ""
            },
            title = { Text("Fiyat Ekle - ${editingItem?.name}") },
            text = {
                Column {
                    OutlinedTextField(value = priceAmount, onValueChange = { priceAmount = it }, 
                        label = { Text("Fiyat (\u20BA)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = priceStore, onValueChange = { priceStore = it }, 
                        label = { Text("Market / Mağaza") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = priceNotes, onValueChange = { priceNotes = it }, 
                        label = { Text("Notlar (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val price = priceAmount.toDoubleOrNull()
                    if (price != null && price > 0) {
                        editingItem?.let { item ->
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val priceRecord = PriceRecord(
                                price = price,
                                store = priceStore,
                                date = today,
                                quantity = item.quantity,
                                notes = priceNotes
                            )
                            
                            // Update price history
                            val currentHistory = parsePriceHistory(item.priceHistory)
                            val updatedHistory = currentHistory + priceRecord
                            val avgPrice = updatedHistory.map { it.price }.average()
                            
                            vm.updateInventory(item.copy(
                                lastPrice = price,
                                lastStore = priceStore,
                                lastPurchaseDate = today,
                                averagePrice = avgPrice,
                                priceHistory = Gson().toJson(updatedHistory)
                            ))
                        }
                    }
                    showAddPriceDialog = false
                    editingItem = null
                    priceAmount = ""; priceStore = ""; priceNotes = ""
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddPriceDialog = false
                    editingItem = null
                    priceAmount = ""; priceStore = ""; priceNotes = ""
                }) {
                    Text("İptal")
                }
            }
        )
    }
    
    // Price History Dialog
    if (showPriceHistoryDialog && editingItem != null) {
        val priceHistory = parsePriceHistory(editingItem?.priceHistory ?: "")
        
        AlertDialog(
            onDismissRequest = { 
                showPriceHistoryDialog = false
                editingItem = null
            },
            title = { Text("Fiyat Geçmişi - ${editingItem?.name}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (priceHistory.isEmpty()) {
                        Text("Henüz fiyat kaydı yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        // Price chart placeholder (would need a chart library for real implementation)
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📈 Fiyat Grafiği", fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(8.dp))
                                
                                // Simple text-based chart
                                val maxPrice = priceHistory.maxOfOrNull { it.price } ?: 1.0
                                val minPrice = priceHistory.minOfOrNull { it.price } ?: 0.0
                                val latestPrice = priceHistory.lastOrNull()?.price ?: 0.0
                                val previousPrice = priceHistory.dropLast(1).lastOrNull()?.price ?: latestPrice
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Son Fiyat", style = MaterialTheme.typography.labelSmall)
                                        Text("\u20BA${latestPrice.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column {
                                        Text("Ortalama", style = MaterialTheme.typography.labelSmall)
                                        Text("\u20BA${priceHistory.map { it.price }.average().toInt()}", fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("En Düşük", style = MaterialTheme.typography.labelSmall)
                                        Text("\u20BA${minPrice.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                                    }
                                    Column {
                                        Text("En Yüksek", style = MaterialTheme.typography.labelSmall)
                                        Text("\u20BA${maxPrice.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFE74C3C))
                                    }
                                }
                                
                                // Price change indicator
                                if (previousPrice != latestPrice) {
                                    val changePercent = ((latestPrice - previousPrice) / previousPrice * 100)
                                    val isIncrease = latestPrice > previousPrice
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isIncrease) Color(0xFFE74C3C) else Color(0xFF2ECC71),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "${if (isIncrease) "↑" else "↓"} %${"%.1f".format(kotlin.math.abs(changePercent))}",
                                            color = if (isIncrease) Color(0xFFE74C3C) else Color(0xFF2ECC71),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Price history list
                        Text("Fiyat Kayıtları:", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        
                        priceHistory.sortedByDescending { it.date }.forEach { record ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(6.dp)) {
                                Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(record.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(record.store.ifEmpty { "Bilinmiyor" }, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("\u20BA${record.price.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        if (record.notes.isNotEmpty()) {
                                            Text(record.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Market comparison
                        if (priceHistory.size > 1) {
                            Spacer(Modifier.height(12.dp))
                            Text("🏪 Market Karşılaştırması", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            
                            val storePrices = priceHistory.groupBy { it.store.ifEmpty { "Bilinmiyor" } }
                                .mapValues { (_, records) -> records.map { it.price }.average() }
                                .toList()
                                .sortedBy { it.second }
                            
                            storePrices.forEach { (store, avgPrice) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), 
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(store, style = MaterialTheme.typography.bodySmall)
                                    Text("\u20BA${avgPrice.toInt()} (ort.)", 
                                        fontWeight = FontWeight.Medium,
                                        color = if (avgPrice == storePrices.first().second) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showPriceHistoryDialog = false
                    editingItem = null
                }) {
                    Text("Kapat")
                }
            }
        )
    }
}

private fun parsePriceHistory(json: String): List<PriceRecord> {
    if (json.isEmpty()) return emptyList()
    return try {
        val type = object : TypeToken<List<PriceRecord>>() {}.type
        Gson().fromJson(json, type)
    } catch (e: Exception) {
        emptyList()
    }
}
