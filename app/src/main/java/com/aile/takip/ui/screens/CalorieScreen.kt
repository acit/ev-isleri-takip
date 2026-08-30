package com.aile.takip.ui.screens

import androidx.compose.foundation.clickable
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

private val mealTypes = listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık")
private val commonFoods = listOf(
    Triple("Yumurta (2 adet)", 156, "14g P \u00B7 1g K \u00B7 11g Ya"),
    Triple("Tam Buğday Ekmeği", 75, "4g P \u00B7 13g K \u00B7 1g Ya"),
    Triple("Peynir (30g)", 85, "6g P \u00B7 1g K \u00B7 7g Ya"),
    Triple("Süt (200ml)", 106, "7g P \u00B7 10g K \u00B7 4g Ya"),
    Triple("Meyve (1 adet)", 60, "1g P \u00B7 15g K \u00B7 0g Ya"),
    Triple("Tavuk Göğsü (100g)", 165, "31g P \u00B7 0g K \u00B7 4g Ya"),
    Triple("Pirinç (1 porsiyon)", 206, "4g P \u00B7 45g K \u00B7 0g Ya"),
    Triple("Makarna (1 porsiyon)", 220, "8g P \u00B7 43g K \u00B7 1g Ya"),
    Triple("Balık (100g)", 206, "22g P \u00B7 0g K \u00B7 12g Ya"),
    Triple("Salata", 20, "1g P \u00B7 4g K \u00B7 0g Ya"),
    Triple("Çikolata (1 kare)", 53, "1g P \u00B7 6g K \u00B7 3g Ya"),
    Triple("Kahve (filtre)", 2, "0g P \u00B7 0g K \u00B7 0g Ya"),
    Triple("Ayran (200ml)", 66, "4g P \u00B7 5g K \u00B7 3g Ya"),
    Triple("Çorba (1 kase)", 80, "5g P \u00B7 10g K \u00B7 2g Ya"),
    Triple("Zeytinyağı (1 yk)", 88, "0g P \u00B7 0g K \u00B7 10g Ya"),
)

@Composable
fun CalorieScreen(vm: MainViewModel) {
    val calorieLogs by vm.calorieLogs.collectAsState()
    val members by vm.members.collectAsState()
    val selectedMember by vm.selectedMemberId
    var showAdd by remember { mutableStateOf(false) }
    var foodName by remember { mutableStateOf("") }
    var calStr by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf("Kahvaltı") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val memberName = members.find { it.id == selectedMember }?.name ?: "Tümü"

    val todayLogs = calorieLogs.filter { it.date == today && (selectedMember == null || it.memberId == selectedMember) }
    val totalCal = todayLogs.sumOf { it.calories }
    val totalP = todayLogs.sumOf { it.protein }
    val totalC = todayLogs.sumOf { it.carbs }
    val totalF = todayLogs.sumOf { it.fat }
    val targetCal = 2000  // Default daily target

    PageScaffold("\uD83C\uDF7D\uFE0F Kalori", "\uD83C\uDF7D\uFE0F") {
        // Member selector
        if (members.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = selectedMember == null, onClick = { vm.selectedMemberId.value = null }, label = { Text("Tümü", style = MaterialTheme.typography.labelSmall) })
                members.take(4).forEach { m ->
                    FilterChip(selected = selectedMember == m.id, onClick = { vm.selectedMemberId.value = m.id }, label = { Text(m.name.take(6), style = MaterialTheme.typography.labelSmall) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Daily summary
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bugün \u2022 $memberName", fontWeight = FontWeight.Bold)
                    Text("$totalCal / $targetCal kcal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (totalCal.toFloat() / targetCal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = when { totalCal > targetCal -> Color(0xFFE74C3C); totalCal > targetCal * 0.8 -> Color(0xFFF39C12); else -> Color(0xFF2ECC71) },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totalP.toInt()}g", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Protein", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totalC.toInt()}g", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Text("Karbonhidrat", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totalF.toInt()}g", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Yağ", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) { Text("+ Yemek Ekle") }

        Spacer(Modifier.height(12.dp))
        Text("Bugünkü Kayıtlar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))

        // Group by meal type
        mealTypes.forEach { mt ->
            val mealLogs = todayLogs.filter { it.mealType == mt }
            if (mealLogs.isNotEmpty()) {
                Text(mt, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
                mealLogs.forEach { log ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.foodName, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                Text("${log.protein.toInt()}P \u00B7 ${log.carbs.toInt()}K \u00B7 ${log.fat.toInt()}Ya", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${log.calories} kcal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { vm.deleteCalorie(log) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "Sil", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Yemek Ekle") },
                text = {
                    Column {
                        Text("Öğün:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            mealTypes.forEach { m ->
                                FilterChip(selected = mealType == m, onClick = { mealType = m }, label = { Text(m, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        Text("Sık Kullanılanlar:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        commonFoods.take(8).forEach { (name, cal, macros) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable { foodName = name; calStr = cal.toString() }) {
                                Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text("${cal} kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(value = foodName, onValueChange = { foodName = it }, label = { Text("Yemek Adı") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = calStr, onValueChange = { calStr = it }, label = { Text("Kalori (kcal)") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = protein, onValueChange = { protein = it }, modifier = Modifier.weight(1f), label = { Text("Protein") }, singleLine = true)
                            OutlinedTextField(value = carbs, onValueChange = { carbs = it }, modifier = Modifier.weight(1f), label = { Text("Karb.") }, singleLine = true)
                            OutlinedTextField(value = fat, onValueChange = { fat = it }, modifier = Modifier.weight(1f), label = { Text("Yağ") }, singleLine = true)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = {
                    if (foodName.isNotBlank() && calStr.toIntOrNull() != null) {
                        vm.addCalorie(selectedMember ?: "", mealType, foodName, calStr.toInt(), protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0)
                        foodName = ""; calStr = ""; protein = ""; carbs = ""; fat = ""; showAdd = false
                    }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } }
            )
        }
    }
}
