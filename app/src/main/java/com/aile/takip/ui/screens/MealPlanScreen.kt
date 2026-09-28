package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
    var expandedSuggestions by remember { mutableStateOf(false) }
    var addedMessage by remember { mutableStateOf<String?>(null) }

    val suggestions = vm.smartSuggestions.value
    val isLoadingSuggestions = vm.isLoadingSuggestions.value
    val marketDiff = vm.marketDiff.value
    val isGeneratingMarketList = vm.isGeneratingMarketList.value

    // Ekrana girince / plan değişince önerileri yenile
    androidx.compose.runtime.LaunchedEffect(plans.size) {
        vm.loadSmartSuggestions()
    }

    PageScaffold("\uD83C\uDF7D\uFE0F", "\uD83C\uDF7D\uFE0F") {
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
            Text("+ Yemek Ekle")
        }
        Spacer(Modifier.height(12.dp))

        // ═══ AKILLI ALIŞVERİŞ ÖNERİSİ ═══
        if (isLoadingSuggestions) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Envanter kontrol ediliyor…", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
        } else if (!suggestions.isNullOrEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧠", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Akıllı Öneri: ${suggestions.size} malzeme eksik",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Yemek planın envanterle karşılaştırıldı",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { expandedSuggestions = !expandedSuggestions }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (expandedSuggestions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                "Genişlet", modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // İlk 3 malzeme her zaman görünür
                    val visible = if (expandedSuggestions) suggestions else suggestions.take(3)
                    visible.forEach { s ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${s.name} — ${s.missing} adet lazım",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${s.usedByDishes.take(2).joinToString(", ")}" + if (s.usedByDishes.size > 2) " +${s.usedByDishes.size - 2} yemek" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    vm.addSuggestionsToShopping(listOf(s.name)) { added ->
                                        addedMessage = if (added > 0) "${s.name} listeye eklendi" else "${s.name} zaten listede"
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Ekle", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (suggestions.size > 3) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (expandedSuggestions) "" else "+${suggestions.size - 3} malzeme daha…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            vm.addSuggestionsToShopping(emptyList()) { added ->
                                addedMessage = if (added > 0) "$added malzeme alışveriş listesine eklendi ✅" else "Tüm eksikler zaten listede"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Hepsini Alışveriş Listesine Ekle")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ═══ HAFTALIK MARKET LİSTESİ + KARŞILAŞTIRMA ═══
        if (!suggestions.isNullOrEmpty()) {
            OutlinedButton(
                onClick = {
                    vm.generateMarketList { diff ->
                        addedMessage = diff?.let { d ->
                            "Market listesi kaydedildi (${d.currentItems.size} malzeme)"
                        } ?: "Market listesi oluşturulamadı"
                    }
                },
                enabled = !isGeneratingMarketList,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isGeneratingMarketList) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("📋 Haftalık Market Listesi Oluştur")
            }
            Spacer(Modifier.height(8.dp))
        }

        marketDiff?.let { diff ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                    Text(
                        "📋 Haftalık Market Listesi — ${diff.currentWeek}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (diff.previousWeek == null)
                            "İlk kaydedilen hafta — sonraki hafta karşılaştırma için temel olacak"
                        else "Önceki hafta (${diff.previousWeek}) ile karşılaştırma:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = {}, label = { Text("🔴 Yeni: ${diff.newItems.size}", style = MaterialTheme.typography.labelSmall) })
                        AssistChip(onClick = {}, label = { Text("🔁 Tekrar: ${diff.carriedOver.size}", style = MaterialTheme.typography.labelSmall) })
                        AssistChip(onClick = {}, label = { Text("⬇️ Düşen: ${diff.droppedItems.size}", style = MaterialTheme.typography.labelSmall) })
                    }
                    if (diff.currentItems.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            diff.currentItems.take(5).joinToString("\n") { "• ${it.name} ×${it.quantity}" } +
                                if (diff.currentItems.size > 5) "\n+${diff.currentItems.size - 5} malzeme daha" else "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (diff.repeatRate > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Geçen haftanın %{diff.repeatRate}’i bu hafta da gerekiyor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Ekleme bildirimi
        addedMessage?.let { msg ->
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✅", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { addedMessage = null }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Close, "Kapat", modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

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
