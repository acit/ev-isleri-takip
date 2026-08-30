package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel

private val dayNames = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
private val mealTypes = listOf("Kahvaltı", "Öğle", "Akşam")
private val mealIcons = mapOf("Kahvaltı" to "\u2615", "Öğle" to "\uD83C\uDF5D", "Akşam" to "\uD83C\uDF72")

@Composable
fun MealPlanScreen(vm: MainViewModel) {
    val plans by vm.mealPlans.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableIntStateOf(0) }
    var selectedMeal by remember { mutableStateOf("Kahvaltı") }
    var dish by remember { mutableStateOf("") }

    PageScaffold("\uD83C\uDF7D\uFE0F", "\uD83C\uDF7D\uFE0F") {
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
            Text("+ Yemek Ekle")
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dayNames.forEachIndexed { dayIndex, dayName ->
                val dayMeals = plans.filter { it.dayOfWeek == dayIndex }
                item(key = "day_$dayIndex") {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(dayName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            if (dayMeals.isEmpty()) {
                                Text("Henüz yemek planlanmamış", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            dayMeals.forEach { mp ->
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(mealIcons[mp.mealType] ?: "\uD83C\uDF7D\uFE0F", modifier = Modifier.width(24.dp))
                                    Text("${mp.mealType}: ${mp.dish}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    IconButton(onClick = { vm.deleteMealPlan(mp) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, "Sil", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                title = { Text("Yemek Ekle") },
                text = {
                    Column {
                        Text("Gün:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            dayNames.take(5).forEachIndexed { i, d ->
                                FilterChip(selected = selectedDay == i, onClick = { selectedDay = i }, label = { Text(d.take(3), style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Öğün:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            mealTypes.forEach { m ->
                                FilterChip(selected = selectedMeal == m, onClick = { selectedMeal = m }, label = { Text(m, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = dish, onValueChange = { dish = it }, label = { Text("Yemek Adı") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    if (dish.isNotBlank()) { vm.addMealPlan(selectedDay, selectedMeal, dish); dish = ""; showAdd = false }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("İptal") } }
            )
        }
    }
}
