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
import com.aile.takip.data.model.SportsClub
import com.aile.takip.data.model.WorkoutLog
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

private val clubTypes = listOf("Spor Salonu", "Yüzme", "Futbol", "Yoga", "Pilates", "Koşu", "Bisiklet", "Dans", "MMA", "Diğer")
private val workoutTypes = listOf("Kardiyo", "Ağırlık", "Yoga", "Yüzme", "Koşu", "HIIT", "Stretching", "Spor Salonu", "Diğer")

@Composable
fun SportsClubScreen(vm: MainViewModel) {
    val clubs by vm.sportsClubs.collectAsState()
    val workouts by vm.workoutLogs.collectAsState()
    val members by vm.members.collectAsState()
    var showAddClub by remember { mutableStateOf(false) }
    var showAddWorkout by remember { mutableStateOf(false) }
    var clubName by remember { mutableStateOf("") }
    var clubType by remember { mutableStateOf("Spor Salonu") }
    var clubFee by remember { mutableStateOf("") }
    var clubAddress by remember { mutableStateOf("") }
    var selectedClubId by remember { mutableStateOf("") }
    var workoutType by remember { mutableStateOf("Kardiyo") }
    var duration by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var workoutNotes by remember { mutableStateOf("") }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayWorkouts = workouts.filter { it.date == today }
    val todayCalories = todayWorkouts.sumOf { it.caloriesBurned }
    val todayDuration = todayWorkouts.sumOf { it.duration }

    PageScaffold("\uD83C\uDFC3", "\uD83C\uDFC3") {
        // Today's summary
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCAA", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    Text("${todayWorkouts.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Antrenman", style = MaterialTheme.typography.labelSmall)
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u23F1\uFE0F", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    Text("${todayDuration} dk", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("Süre", style = MaterialTheme.typography.labelSmall)
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDD25", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    Text("$todayCalories", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Text("Kalori", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAddClub = true }, shape = RoundedCornerShape(10.dp)) { Text("+ Klüp") }
            OutlinedButton(onClick = { showAddWorkout = true }, shape = RoundedCornerShape(10.dp)) { Text("+ Antrenman") }
        }

        // Clubs
        if (clubs.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Spor Klüpleri (${clubs.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            clubs.forEach { club ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) { Text("\uD83C\uDFC3") }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(club.name, fontWeight = FontWeight.SemiBold)
                            Text("${club.type} \u00B7 \u20BA${club.monthlyFee.toInt()}/ay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.deleteClub(club) }) {
                            Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Recent workouts
        Spacer(Modifier.height(12.dp))
        Text("Son Antrenmanlar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))

        if (workouts.isEmpty()) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83C\uDFC3", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz antrenman kaydı yok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(workouts.take(20), key = { it.id }) { w ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(w.workoutType, fontWeight = FontWeight.SemiBold)
                            Text("${w.duration} dk \u00B7 ${w.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("\uD83D\uDD25 ${w.caloriesBurned} kcal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            IconButton(onClick = { vm.deleteWorkout(w) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "Sil", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Add Club Dialog
        if (showAddClub) {
            AlertDialog(onDismissRequest = { showAddClub = false }, title = { Text("Spor Klübü Ekle") },
                text = {
                    Column {
                        OutlinedTextField(value = clubName, onValueChange = { clubName = it }, label = { Text("Klüp Adı") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        Text("Tür:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            clubTypes.take(4).forEach { t ->
                                FilterChip(selected = clubType == t, onClick = { clubType = t }, label = { Text(t, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = clubFee, onValueChange = { clubFee = it }, label = { Text("Aylık Ücret (\u20BA)") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = clubAddress, onValueChange = { clubAddress = it }, label = { Text("Adres") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    if (clubName.isNotBlank()) { vm.addClub(clubName, clubType, clubAddress, fee = clubFee.toDoubleOrNull() ?: 0.0); clubName = ""; clubFee = ""; clubAddress = ""; showAddClub = false }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showAddClub = false }) { Text("İptal") } }
            )
        }

        // Add Workout Dialog
        if (showAddWorkout) {
            AlertDialog(onDismissRequest = { showAddWorkout = false }, title = { Text("Antrenman Ekle") },
                text = {
                    Column {
                        Text("Tür:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            workoutTypes.take(4).forEach { t ->
                                FilterChip(selected = workoutType == t, onClick = { workoutType = t }, label = { Text(t, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Süre (dakika)") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Kalori (kcal)") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = workoutNotes, onValueChange = { workoutNotes = it }, label = { Text("Notlar") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    if (duration.isNotBlank()) { vm.addWorkout(selectedClubId, "", workoutType, duration.toIntOrNull() ?: 0, calories.toIntOrNull() ?: 0, today, workoutNotes); duration = ""; calories = ""; workoutNotes = ""; showAddWorkout = false }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showAddWorkout = false }) { Text("İptal") } }
            )
        }
    }
}
