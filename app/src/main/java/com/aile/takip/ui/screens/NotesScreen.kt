package com.aile.takip.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aile.takip.data.model.Attachment
import com.aile.takip.data.model.Note
import com.aile.takip.ui.viewmodel.MainViewModel
import com.aile.takip.utils.AttachmentHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

val noteColors = listOf(
    "#3498DB", "#2ECC71", "#E74C3C", "#F39C12",
    "#9B59B6", "#1ABC9C", "#E91E63", "#607D8B"
)

val noteCategories = listOf("Genel", "Alışveriş", "Tarif", "Fikir", "Önemli", "Plan", "Not")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(vm: MainViewModel) {
    val notes by vm.notes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val filteredNotes = notes.filter { note ->
        val matchesSearch = searchQuery.isEmpty() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || note.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83D\uDCDD Notlar", style = MaterialTheme.typography.headlineMedium)
            Row {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Menü")
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Yeni Not")
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("\uD83D\uDCCB Arşiv (${notes.count { it.isArchived }})") },
                    onClick = { showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("\uD83D\uDD04 Tümünü Güncelle") },
                    onClick = { showMenu = false }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Stats
        Text(
            "${notes.size} not • ${notes.count { it.isPinned }} sabitlenmiş",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Notlarda ara...") },
            leadingIcon = { Icon(Icons.Default.Search, "Ara") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, "Temizle")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Tümü") }
            )
            noteCategories.take(4).forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = if (selectedCategory == category) null else category
                    },
                    label = { Text(category) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Notes Grid
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCDD", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz not yok", style = MaterialTheme.typography.titleMedium)
                    Text("Yeni not eklemek için + butonuna tıklayın",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNotes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { editingNote = note },
                        onPin = { vm.togglePinNote(note) },
                        onArchive = { vm.archiveNote(note) },
                        onDelete = { vm.deleteNote(note) }
                    )
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || editingNote != null) {
        NoteDialog(
            note = editingNote,
            onDismiss = {
                showAddDialog = false
                editingNote = null
            },
            onSave = { title, content, category, color, attachmentsJson ->
                if (editingNote != null) {
                    vm.updateNote(editingNote!!.copy(
                        title = title,
                        content = content,
                        category = category,
                        color = color,
                        attachments = attachmentsJson
                    ))
                } else {
                    vm.addNote(title, content, category, color, attachmentsJson)
                }
                showAddDialog = false
                editingNote = null
            }
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val noteColor = try {
        Color(android.graphics.Color.parseColor(note.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = noteColor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = noteColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        note.category,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = noteColor
                    )
                }

                Row {
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Sabitlenmiş",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menü",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Sabitlemeyi Kaldır" else "Sabitlen") },
                                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                                onClick = { onPin(); showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isArchived) "Arşivden Çıkar" else "Arşivle") },
                                leadingIcon = { Icon(Icons.Default.Archive, null) },
                                onClick = { onArchive(); showMenu = false }
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

            Spacer(Modifier.height(8.dp))

            Text(
                note.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (note.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Attachment indicator
            if (note.attachments.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Attachment,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        try {
                            val type = object : TypeToken<List<Attachment>>() {}.type
                            val attList = Gson().fromJson<List<Attachment>>(note.attachments, type)
                            "${attList.size} ek"
                        } catch (e: Exception) { "Ek var" },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    note.createdBy.ifEmpty { "Aile" },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
                        .format(java.util.Date(note.createdAt)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDialog(
    note: Note?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var selectedCategory by remember { mutableStateOf(note?.category ?: "Genel") }
    var selectedColor by remember { mutableStateOf(note?.color ?: noteColors[0]) }
    var attachments by remember {
        mutableStateOf(
            if (note?.attachments?.isNotEmpty() == true) {
                try {
                    val type = object : TypeToken<List<Attachment>>() {}.type
                    Gson().fromJson<List<Attachment>>(note.attachments, type)
                } catch (e: Exception) { emptyList() }
            } else emptyList()
        )
    }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = AttachmentHelper.uriToBase64(context, it)
            if (base64 != null) {
                val fileName = uri.lastPathSegment ?: "dosya"
                val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                attachments = attachments + Attachment(
                    fileName = fileName,
                    mimeType = mimeType,
                    base64Data = base64,
                    fileSize = base64.length.toLong() / 1337 // approx size
                )
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Camera photo saved to temp file
            // The URI is handled by the caller
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note != null) "Notu Düzenle" else "Yeni Not") },
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
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("İçerik") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 6
                )

                Spacer(Modifier.height(16.dp))

                Text("Kategori", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    noteCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Renk", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    noteColors.forEach { colorHex ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color)
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorHex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Seçili",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

                Spacer(Modifier.height(16.dp))

                // Attachments
                Text("Ekler", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                // Existing attachments
                attachments.forEachIndexed { index, attachment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (attachment.mimeType.startsWith("image/")) Icons.Default.Image else Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(attachment.fileName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        IconButton(
                            onClick = { attachments = attachments.toMutableList().apply { removeAt(index) } },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "Kaldır", modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Add attachment buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Photo, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Galeri", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dosya", fontSize = 12.sp)
                    }
                }
        },
        confirmButton = {
            Button(
                onClick = {
                    val attachmentsJson = Gson().toJson(attachments)
                    onSave(title, content, selectedCategory, selectedColor, attachmentsJson)
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
