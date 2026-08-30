package com.aile.takip.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aile.takip.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// Goal constants
const val DAILY_WATER_GOAL_ML = 2500
const val DAILY_CALORIE_GOAL = 2000
const val DAILY_SLEEP_GOAL_MINUTES = 480  // 8 hours
const val DAILY_EXERCISE_GOAL_MINUTES = 30

@Composable
fun HealthDashboardScreen(vm: MainViewModel) {
    val members by vm.members.collectAsState()
    val calorieLogs by vm.calorieLogs.collectAsState()
    val workoutLogs by vm.workoutLogs.collectAsState()
    val waterLogs by vm.waterLogs.collectAsState()
    val sleepLogs by vm.sleepLogs.collectAsState()

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Today's data
    val todayCalories = calorieLogs.filter { it.date == todayStr }.sumOf { it.calories }
    val todayWater = waterLogs.filter { it.date == todayStr }.sumOf { it.amountMl }
    val todayWorkouts = workoutLogs.filter { it.date == todayStr }
    val todayExerciseMinutes = todayWorkouts.sumOf { it.duration }
    val todayCaloriesBurned = todayWorkouts.sumOf { it.caloriesBurned }
    val todaySleep = sleepLogs.filter { it.date == todayStr }

    // Show add dialogs
    var showAddWater by remember { mutableStateOf(false) }
    var showAddCalorie by remember { mutableStateOf(false) }
    var showAddSleep by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text("\uD83D\uDCCA Sağlık Paneli", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Bugünün özeti — $todayStr",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // ===== SUMMARY CARDS =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Water Card
            HealthSummaryCard(
                modifier = Modifier.weight(1f),
                icon = "\uD83D\uDCA7",
                value = "${todayWater}ml",
                label = "Su",
                progress = (todayWater.toFloat() / DAILY_WATER_GOAL_ML).coerceIn(0f, 1f),
                color = Color(0xFF3498DB),
                goal = "${DAILY_WATER_GOAL_ML / 1000}L hedef",
                onClick = { showAddWater = true }
            )

            // Calories Card
            HealthSummaryCard(
                modifier = Modifier.weight(1f),
                icon = "\uD83D\uDD25",
                value = "$todayCalories",
                label = "Kalori",
                progress = (todayCalories.toFloat() / DAILY_CALORIE_GOAL).coerceIn(0f, 1f),
                color = Color(0xFFE74C3C),
                goal = "$DAILY_CALORIE_GOAL kcal",
                onClick = { showAddCalorie = true }
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sleep Card
            val sleepHours = todaySleep.firstOrNull()?.let { it.durationMinutes / 60f } ?: 0f
            HealthSummaryCard(
                modifier = Modifier.weight(1f),
                icon = "\uD83D\uDCA4",
                value = String.format("%.1f saat", sleepHours),
                label = "Uyku",
                progress = (todaySleep.firstOrNull()?.durationMinutes?.toFloat()?.div(DAILY_SLEEP_GOAL_MINUTES))?.coerceIn(0f, 1f) ?: 0f,
                color = Color(0xFF9B59B6),
                goal = "8 saat hedef",
                onClick = { showAddSleep = true }
            )

            // Exercise Card
            HealthSummaryCard(
                modifier = Modifier.weight(1f),
                icon = "\uD83C\uDFC3",
                value = "$todayExerciseMinutes dk",
                label = "Egzersiz",
                progress = (todayExerciseMinutes.toFloat() / DAILY_EXERCISE_GOAL_MINUTES).coerceIn(0f, 1f),
                color = Color(0xFF2ECC71),
                goal = "${todayCaloriesBurned} kcal yandı",
                onClick = {}
            )
        }

        Spacer(Modifier.height(20.dp))

        // ===== CALORIE BREAKDOWN =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("\uD83C\uDF7D\uFE0F Kalori Dağılımı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                val todayMeals = calorieLogs.filter { it.date == todayStr }
                val totalProtein = todayMeals.sumOf { it.protein }
                val totalCarbs = todayMeals.sumOf { it.carbs }
                val totalFat = todayMeals.sumOf { it.fat }

                if (todayMeals.isEmpty()) {
                    Text(
                        "Bugün henüz kalori kaydı yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    MacroBar("Protein", totalProtein, Color(0xFFE74C3C))
                    Spacer(Modifier.height(8.dp))
                    MacroBar("Karbonhidrat", totalCarbs, Color(0xFFF39C12))
                    Spacer(Modifier.height(8.dp))
                    MacroBar("Yağ", totalFat, Color(0xFF3498DB))

                    Spacer(Modifier.height(12.dp))

                    // Meal list
                    todayMeals.groupBy { it.mealType }.forEach { (mealType, logs) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mealType, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${logs.sumOf { it.calories }} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== RECENT ACTIVITY =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("\uD83D\uDCC5 Son Aktiviteler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                val allActivities = mutableListOf<Triple<String, String, String>>() // icon, text, time

                // Today's workouts
                todayWorkouts.forEach { w ->
                    allActivities.add(Triple("\uD83C\uDFC3", "${w.workoutType} — ${w.duration} dk", "${w.caloriesBurned} kcal yandı"))
                }

                // Today's water
                val waterCount = waterLogs.count { it.date == todayStr }
                if (waterCount > 0) {
                    allActivities.add(Triple("\uD83D\uDCA7", "$waterCount bardak su içildi", "${todayWater}ml toplam"))
                }

                // Today's sleep
                todaySleep.firstOrNull()?.let { s ->
                    val quality = when (s.quality) {
                        "çok iyi" -> "\u2B50\u2B50\u2B50"
                        "iyi" -> "\u2B50\u2B50"
                        "orta" -> "\u2B50"
                        else -> "\u274C"
                    }
                    allActivities.add(Triple("\uD83D\uDCA4", "Uyku: ${s.durationMinutes / 60} saat ${s.durationMinutes % 60} dk", "$quality kalite"))
                }

                if (allActivities.isEmpty()) {
                    Text(
                        "Bugün henüz aktivite yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    allActivities.forEach { (icon, text, detail) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text, style = MaterialTheme.typography.bodyMedium)
                                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ===== WATER QUICK ADD =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3498DB).copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("\uD83D\uDCA7 Hızlı Su Ekle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(150, 250, 330, 500).forEach { amount ->
                        FilledTonalButton(
                            onClick = { vm.addWater("", amountMl = amount) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${amount}ml", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // ===== DIALOGS =====

    if (showAddWater) {
        AddWaterDialog(
            onDismiss = { showAddWater = false },
            onSave = { amount, type ->
                vm.addWater("", amountMl = amount, drinkType = type)
                showAddWater = false
            }
        )
    }

    if (showAddCalorie) {
        AddCalorieDialog(
            onDismiss = { showAddCalorie = false },
            onSave = { mealType, food, calories, protein, carbs, fat, serving ->
                vm.addCalorie("", mealType = mealType, foodName = food, calories = calories,
                    protein = protein, carbs = carbs, fat = fat, serving = serving)
                showAddCalorie = false
            }
        )
    }

    if (showAddSleep) {
        AddSleepDialog(
            onDismiss = { showAddSleep = false },
            onSave = { bedtime, wakeTime, quality, interruptions, notes ->
                vm.addSleep("", bedtime = bedtime, wakeTime = wakeTime,
                    quality = quality, interruptions = interruptions, notes = notes)
                showAddSleep = false
            }
        )
    }
}

@Composable
fun HealthSummaryCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    progress: Float,
    color: Color,
    goal: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(4.dp))
            Text(goal, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MacroBar(name: String, value: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
            progress = { (value / 100).coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Spacer(Modifier.width(8.dp))
        Text(String.format("%.0fg", value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWaterDialog(
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    var amount by remember { mutableIntStateOf(250) }
    var drinkType by remember { mutableStateOf("Su") }

    val drinkTypes = listOf("Su", "Çay", "Kahve", "Meyve Suyu", "Süt", "Diğer")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Su / İçecek Ekle") },
        text = {
            Column {
                Text("Miktar (ml)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 150, 250, 330, 500).forEach { m ->
                        FilterChip(
                            selected = amount == m,
                            onClick = { amount = m },
                            label = { Text("${m}ml", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("İçecek Türü", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    drinkTypes.take(3).forEach { type ->
                        FilterChip(
                            selected = drinkType == type,
                            onClick = { drinkType = type },
                            label = { Text(type, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    drinkTypes.drop(3).forEach { type ->
                        FilterChip(
                            selected = drinkType == type,
                            onClick = { drinkType = type },
                            label = { Text(type, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(amount, drinkType) }) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalorieDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Double, Double, Double, String) -> Unit
) {
    var mealType by remember { mutableStateOf("Kahvaltı") }
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var serving by remember { mutableStateOf("1 porsiyon") }

    val mealTypes = listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yiyecek / Kalori Ekle") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Öğün", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mealTypes.forEach { type ->
                        FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type, fontSize = 11.sp) })
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = foodName, onValueChange = { foodName = it }, label = { Text("Yiyecek adı") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = calories, onValueChange = { calories = it.filter { c -> c.isDigit() } }, label = { Text("Kalori (kcal)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Karb (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Yağ (g)") }, modifier = Modifier.weight(1f), singleLine = true)
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = serving, onValueChange = { serving = it }, label = { Text("Porsiyon") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(mealType, foodName, calories.toIntOrNull() ?: 0,
                        protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0,
                        fat.toDoubleOrNull() ?: 0.0, serving)
                },
                enabled = foodName.isNotBlank() && calories.isNotBlank()
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSleepDialog(
    onDismiss: () -> Unit,
    onSave: (Long, Long, String, Int, String) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    var bedtimeHour by remember { mutableIntStateOf(23) }
    var bedtimeMinute by remember { mutableIntStateOf(0) }
    var wakeHour by remember { mutableIntStateOf(7) }
    var wakeMinute by remember { mutableIntStateOf(0) }
    var quality by remember { mutableStateOf("iyi") }
    var interruptions by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }

    val qualities = listOf("kötü", "orta", "iyi", "çok iyi")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uyku Kaydı") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Bedtime
                Text("Yatma Saati", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable {
                        TimePickerDialog(context, { _, h, m ->
                            bedtimeHour = h; bedtimeMinute = m
                        }, bedtimeHour, bedtimeMinute, true).show()
                    }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bedtime, null, tint = Color(0xFF9B59B6))
                        Spacer(Modifier.width(12.dp))
                        Text(String.format("%02d:%02d", bedtimeHour, bedtimeMinute))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Wake time
                Text("Kalkma Saati", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable {
                        TimePickerDialog(context, { _, h, m ->
                            wakeHour = h; wakeMinute = m
                        }, wakeHour, wakeMinute, true).show()
                    }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, null, tint = Color(0xFFF39C12))
                        Spacer(Modifier.width(12.dp))
                        Text(String.format("%02d:%02d", wakeHour, wakeMinute))
                    }
                }

                // Calculated duration
                Spacer(Modifier.height(8.dp))
                val durationText = remember(bedtimeHour, bedtimeMinute, wakeHour, wakeMinute) {
                    var bedMinutes = bedtimeHour * 60 + bedtimeMinute
                    var wakeMinutes = wakeHour * 60 + wakeMinute
                    if (wakeMinutes <= bedMinutes) wakeMinutes += 24 * 60
                    val total = wakeMinutes - bedMinutes
                    "${total / 60} saat ${total % 60} dk"
                }
                Text("Süre: $durationText", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(16.dp))

                // Quality
                Text("Kalite", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    qualities.forEach { q ->
                        val emoji = when(q) { "kötü" -> "\u274C"; "orta" -> "\uD83D\uDE10"; "iyi" -> "\uD83D\uDE0A"; "çok iyi" -> "\u2B50\u2B50"; else -> "" }
                        FilterChip(selected = quality == q, onClick = { quality = q }, label = { Text("$emoji $q", fontSize = 11.sp) })
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Interruptions
                Text("Uyanma Sayısı", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (interruptions > 0) interruptions-- }) {
                        Icon(Icons.Default.Remove, "Azalt")
                    }
                    Text("$interruptions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(onClick = { interruptions++ }) {
                        Icon(Icons.Default.Add, "Artır")
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notlar (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                calendar.set(Calendar.HOUR_OF_DAY, bedtimeHour); calendar.set(Calendar.MINUTE, bedtimeMinute)
                val bedTime = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, wakeHour); calendar.set(Calendar.MINUTE, wakeMinute)
                val wakeTime = calendar.timeInMillis
                onSave(bedTime, wakeTime, quality, interruptions, notes)
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
