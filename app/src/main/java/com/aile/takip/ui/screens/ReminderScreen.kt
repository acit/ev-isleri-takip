package com.aile.takip.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aile.takip.data.model.Reminder
import com.aile.takip.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

val reminderCategories = listOf("Genel", "Görev", "Fatura", "Etkinlik", "Sağlık")
val reminderPriorities = listOf("düşük", "orta", "yüksek")
val repeatTypes = listOf("once", "daily", "weekly", "monthly", "custom")
val alarmSounds = listOf("default", "alarm", "notification", "ringtone", "urgent")
val alarmSoundLabels = mapOf(
    "default" to "Varsayılan",
    "alarm" to "Alarm",
    "notification" to "Bildirim",
    "ringtone" to "Zil Sesi",
    "urgent" to "Acil"
)
val reminderDayNames = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
val reminderDayValues = listOf(2, 3, 4, 5, 6, 7, 1)  // Calendar.MONDAY=2 ... SUNDAY=1

@Composable
fun ReminderScreen(vm: MainViewModel) {
    val activeReminders by vm.activeReminders.collectAsState()
    val allReminders by vm.allReminders.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    val filteredReminders = when (selectedTab) {
        0 -> activeReminders
        1 -> allReminders.filter { it.isCompleted }
        2 -> allReminders
        else -> activeReminders
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83D\uDD14 Hatırlatıcılar", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Yeni Hatırlatıcı")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Stats
        Text(
            "${activeReminders.size} aktif • ${allReminders.count { it.isCompleted }} tamamlanan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Aktif (${activeReminders.size})")
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Tamamlanan")
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Tümü")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Reminders List
        if (filteredReminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDD14", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (selectedTab) {
                            0 -> "Aktif hatırlatıcı yok"
                            1 -> "Henüz tamamlanan hatırlatıcı yok"
                            else -> "Hatırlatıcı eklenmemiş"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Yeni hatırlatıcı eklemek için + butonuna tıklayın",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredReminders) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onComplete = { vm.completeReminder(reminder) },
                        onSnooze = { vm.snoozeReminder(reminder) },
                        onEdit = { editingReminder = reminder },
                        onDelete = { vm.deleteReminder(reminder) }
                    )
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || editingReminder != null) {
        ReminderDialog(
            reminder = editingReminder,
            onDismiss = {
                showAddDialog = false
                editingReminder = null
            },
            onSave = { title, description, time, repeat, repeatDays, interval, endDate, category, priority, alarmSound, vibrate, snoozeMinutes ->
                if (editingReminder != null) {
                    vm.updateReminder(editingReminder!!.copy(
                        title = title,
                        description = description,
                        reminderTime = time,
                        repeatType = repeat,
                        repeatDays = repeatDays,
                        repeatInterval = interval,
                        repeatEndDate = endDate,
                        category = category,
                        priority = priority,
                        alarmSound = alarmSound,
                        vibrate = vibrate,
                        snoozeMinutes = snoozeMinutes,
                        nextFireAt = time
                    ))
                } else {
                    vm.addReminder(title, description, time, repeat, category, priority, alarmSound, vibrate, snoozeMinutes, repeatDays, interval, endDate)
                }
                showAddDialog = false
                editingReminder = null
            }
        )
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val isOverdue = !reminder.isCompleted && reminder.reminderTime < System.currentTimeMillis()
    val isSnoozed = reminder.isSnoozed && reminder.snoozeUntil > System.currentTimeMillis()

    val priorityColor = when (reminder.priority) {
        "yüksek" -> Color(0xFFE74C3C)
        "orta" -> Color(0xFFF39C12)
        "düşük" -> Color(0xFF2ECC71)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                reminder.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isOverdue -> Color(0xFFFFEBEE)
                isSnoozed -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )

            Spacer(Modifier.width(12.dp))

            // Complete button
            IconButton(
                onClick = onComplete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (reminder.isCompleted) "Tamamlandı" else "Tamamla",
                    tint = if (reminder.isCompleted) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        reminder.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            reminder.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (reminder.repeatType != "once") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                when (reminder.repeatType) {
                                    "daily" -> "Günlük"
                                    "weekly" -> "Haftalık"
                                    "monthly" -> "Aylık"
                                    else -> reminder.repeatType
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (reminder.description.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        reminder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = when {
                                isOverdue -> Color(0xFFE74C3C)
                                isSnoozed -> Color(0xFFF39C12)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            dateFormat.format(Date(reminder.reminderTime)),
                            fontSize = 11.sp,
                            color = when {
                                isOverdue -> Color(0xFFE74C3C)
                                isSnoozed -> Color(0xFFF39C12)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (reminder.createdBy.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                reminder.createdBy,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Menü")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!reminder.isCompleted) {
                        DropdownMenuItem(
                            text = { Text("15 dk Ertele") },
                            leadingIcon = { Icon(Icons.Default.Snooze, null) },
                            onClick = { onSnooze(); showMenu = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Düzenle") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { onEdit(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    reminder: Reminder?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String, String, Int, Long, String, String, String, Boolean, Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var description by remember { mutableStateOf(reminder?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(reminder?.category ?: "Genel") }
    var selectedPriority by remember { mutableStateOf(reminder?.priority ?: "orta") }
    var selectedRepeat by remember { mutableStateOf(reminder?.repeatType ?: "once") }
    var selectedAlarmSound by remember { mutableStateOf(reminder?.alarmSound ?: "default") }
    var vibrate by remember { mutableStateOf(reminder?.vibrate ?: true) }
    var snoozeMinutes by remember { mutableIntStateOf(reminder?.snoozeMinutes ?: 15) }
    var repeatInterval by remember { mutableIntStateOf(reminder?.repeatInterval ?: 1) }
    var repeatDays by remember { mutableStateOf(reminder?.repeatDays ?: "") }
    var repeatEndDate by remember { mutableLongStateOf(reminder?.repeatEndDate ?: 0L) }

    // Set initial time from reminder or default to tomorrow 9am
    var reminderTime by remember {
        mutableLongStateOf(
            reminder?.reminderTime ?: run {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
        )
    }

    val timeFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reminder != null) "Hatırlatıcıyı Düzenle" else "Yeni Hatırlatıcı") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Başlık") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama (isteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(Modifier.height(16.dp))

                // Date & Time Picker
                Text("Hatırlatma Zamanı", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, minute)
                                            reminderTime = calendar.timeInMillis
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(timeFormat.format(Date(reminderTime)))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Repeat Type
                Text("Tekrarlama", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeatTypes.forEach { repeat ->
                        FilterChip(
                            selected = selectedRepeat == repeat,
                            onClick = { selectedRepeat = repeat },
                            label = {
                                Text(
                                    when (repeat) {
                                        "once" -> "Tek Sefer"
                                        "daily" -> "Günlük"
                                        "weekly" -> "Haftalık"
                                        "monthly" -> "Aylık"
                                        "custom" -> "Özel"
                                        else -> repeat
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                // Repeat interval (for daily/weekly/monthly/custom)
                if (selectedRepeat != "once") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Her", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { if (repeatInterval > 1) repeatInterval-- }) {
                            Icon(Icons.Default.Remove, "Azalt", modifier = Modifier.size(16.dp))
                        }
                        Text("$repeatInterval", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { repeatInterval++ }) {
                            Icon(Icons.Default.Add, "Artır", modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when (selectedRepeat) {
                                "daily" -> if (repeatInterval == 1) "gün" else "günde bir"
                                "weekly" -> if (repeatInterval == 1) "hafta" else "haftada bir"
                                "monthly" -> if (repeatInterval == 1) "ay" else "ayda bir"
                                else -> "döngü"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Weekly: Day selection
                    if (selectedRepeat == "weekly" || selectedRepeat == "custom") {
                        Spacer(Modifier.height(8.dp))
                        Text("Hangi Günler", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            reminderDayNames.forEachIndexed { index, day ->
                                val dayValue = reminderDayValues[index]
                                val isSelected = repeatDays.split(",").contains(dayValue.toString())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val currentDays = if (repeatDays.isEmpty()) emptyList() else repeatDays.split(",")
                                        val newDays = if (isSelected) {
                                            currentDays.filter { it != dayValue.toString() }
                                        } else {
                                            currentDays + dayValue.toString()
                                        }
                                        repeatDays = newDays.joinToString(",")
                                    },
                                    label = { Text(day, fontSize = 10.sp) },
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // End date
                    Spacer(Modifier.height(8.dp))
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                                    calendar.set(Calendar.MINUTE, 59)
                                    repeatEndDate = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (repeatEndDate > 0) "Bitiş: ${dateFormat.format(Date(repeatEndDate))}" else "Süresiz (bitiş tarihi yok)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Category
                Text("Kategori", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Priority
                Text("Öncelik", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderPriorities.forEach { priority ->
                        val color = when (priority) {
                            "yüksek" -> Color(0xFFE74C3C)
                            "orta" -> Color(0xFFF39C12)
                            "düşük" -> Color(0xFF2ECC71)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Alarm Sound
                Text("Alarm Sesi", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alarmSounds.forEach { sound ->
                        FilterChip(
                            selected = selectedAlarmSound == sound,
                            onClick = { selectedAlarmSound = sound },
                            label = {
                                Text(
                                    alarmSoundLabels[sound] ?: sound,
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    when (sound) {
                                        "alarm" -> Icons.Default.Alarm
                                        "notification" -> Icons.Default.Notifications
                                        "ringtone" -> Icons.Default.MusicNote
                                        "urgent" -> Icons.Default.Warning
                                        else -> Icons.Default.VolumeUp
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Vibration & Snooze
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text("Titreşim", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                Spacer(Modifier.height(8.dp))

                // Snooze duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Snooze, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Erteleme süresi:", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    listOf(5, 10, 15, 30).forEach { mins ->
                        FilterChip(
                            selected = snoozeMinutes == mins,
                            onClick = { snoozeMinutes = mins },
                            label = { Text("${mins}dk", fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title, description, reminderTime, selectedRepeat,
                        repeatDays, repeatInterval, repeatEndDate,
                        selectedCategory, selectedPriority,
                        selectedAlarmSound, vibrate, snoozeMinutes
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
