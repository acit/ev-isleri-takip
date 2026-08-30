package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aile.takip.ui.components.PageScaffold
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen() {
    val today = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr")).format(Date())

    val days = remember {
        (0..6).map { offset ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, offset)
            val dayName = SimpleDateFormat("EEE", Locale("tr")).format(c.time)
            val dayNum = c.get(Calendar.DAY_OF_MONTH).toString()
            val isToday = offset == 0
            Triple(dayName, dayNum, isToday)
        }
    }

    PageScaffold("\uD83D\uDCC5", "\uD83D\uDCC5") {
        Text(today, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                days.forEach { (dayName, dayNum, isToday) ->
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(
                        containerColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dayName, style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dayNum, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Bugünün Olayları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        val events = listOf(
            Triple("Okul kermesi", "10:00", Color(0xFF3498DB)),
            Triple("Diş hekimi", "14:30", Color(0xFFE74C3C)),
            Triple("Aile yemeği", "19:00", Color(0xFF2ECC71))
        )
        events.forEach { (event, time, color) ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Surface(modifier = Modifier.size(4.dp, 32.dp), color = color, shape = RoundedCornerShape(2.dp)) {}
                    Spacer(Modifier.width(12.dp))
                    Text(time, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(50.dp))
                    Text(event, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
