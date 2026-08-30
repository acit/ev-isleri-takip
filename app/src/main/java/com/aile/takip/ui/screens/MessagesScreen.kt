package com.aile.takip.ui.screens

import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.ui.graphics.asImageBitmap
import com.aile.takip.utils.BitmapCache
import com.aile.takip.utils.rememberBase64Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aile.takip.data.model.Attachment
import com.aile.takip.data.model.Message
import com.aile.takip.ui.components.PageScaffold
import com.aile.takip.ui.viewmodel.MainViewModel
import com.aile.takip.utils.AttachmentHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val emojiList = listOf(
    "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE01", "\uD83E\uDD14",
    "\uD83D\uDE4F", "\uD83D\uDC4D", "\uD83D\uDC4E", "\u2764\uFE0F", "\uD83D\uDC94",
    "\uD83D\uDE22", "\uD83D\uDE31", "\uD83E\uDD2F", "\uD83D\uDC40", "\uD83C\uDF89",
    "\uD83C\uDFE0", "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66",
    "\uD83C\uDF7D\uFE0F", "\uD83D\uDED2", "\uD83D\uDCCB", "\uD83E\uDDFE",
    "\uD83D\uDCB0", "\u23F0", "\u2705", "\u274C", "\uD83D\uDD25",
    "\uD83C\uDF1F", "\uD83D\uDCAA", "\uD83D\uDE80", "\uD83C\uDFC6", "\uD83C\uDFAF"
)

private val quickReplies = listOf(
    "Tamam \u2705", "Teşekkürler \uD83D\uDE4F", "Yoldayım \uD83D\uDE97",
    "Geliyorum \uD83C\uDFC3", "Bekliyorum \u23F3", "Güle güle \uD83D\uDC4B",
    "Önemli \u26A0\uFE0F", "Acil \uD83D\uDD25", "Mükemmel \uD83C\uDF1F"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(vm: MainViewModel) {
    val messages by vm.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    var showQuickReplies by remember { mutableStateOf(false) }
    var pendingAttachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = AttachmentHelper.uriToBase64(context, it)
            if (base64 != null) {
                val fileName = uri.lastPathSegment ?: "dosya"
                val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                pendingAttachments = pendingAttachments + Attachment(
                    fileName = fileName,
                    mimeType = mimeType,
                    base64Data = base64
                )
            }
        }
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        if (inputText.isNotBlank() || pendingAttachments.isNotEmpty()) {
            val attachmentsJson = if (pendingAttachments.isNotEmpty()) Gson().toJson(pendingAttachments) else ""
            vm.sendMessage(inputText.trim(), attachments = attachmentsJson)
            inputText = ""
            pendingAttachments = emptyList()
            showEmoji = false
            showQuickReplies = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Page header
        Column(modifier = Modifier.padding(16.dp)) {
            Text("\uD83D\uDCAC Mesajlar", style = MaterialTheme.typography.headlineMedium)
            Text("\${messages.size} mesaj", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\uD83D\uDCAC", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Henüz mesaj yok", style = MaterialTheme.typography.titleMedium)
                            Text("İlk mesajı gönderin!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == "self"
                val timeStr = remember(msg.createdAt) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.createdAt))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 280.dp),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        if (!isMe) {
                            Text(
                                msg.senderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = if (isMe) 16.dp else 4.dp,
                                topEnd = if (isMe) 4.dp else 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            ),
                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = if (isMe) 0.dp else 1.dp
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    msg.content,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Text(
                            timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Emoji picker panel
        AnimatedVisibility(visible = showEmoji) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Emoji Seç", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(emojiList) { emoji ->
                            Surface(
                                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { inputText += emoji },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick replies panel
        AnimatedVisibility(visible = showQuickReplies) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickReplies) { reply ->
                    SuggestionChip(
                        onClick = { inputText = reply.replace(Regex(" \\S+$"), ""); sendMessage() },
                        label = { Text(reply, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Pending attachments preview
        if (pendingAttachments.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingAttachments.size) { index ->
                    val attachment = pendingAttachments[index]
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        // Show thumbnail for images (uses BitmapCache for performance)
                        val bitmap = rememberBase64Bitmap(attachment.base64Data, maxWidth = 200)
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap,
                                contentDescription = attachment.fileName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Remove button
                        IconButton(
                            onClick = {
                                pendingAttachments = pendingAttachments.toMutableList().apply { removeAt(index) }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kaldır",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Input area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Emoji toggle
                    IconButton(onClick = { showEmoji = !showEmoji; showQuickReplies = false }) {
                        Icon(
                            if (showEmoji) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                            contentDescription = "Emoji",
                            tint = if (showEmoji) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick replies toggle
                    IconButton(onClick = { showQuickReplies = !showQuickReplies; showEmoji = false }) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Hızlı Yanıt",
                            tint = if (showQuickReplies) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Attachment button
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Ekle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Main text input — full keyboard support
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 120.dp),
                        placeholder = { Text("Mesajınızı yazın...") },
                        // Keyboard options — Turkish keyboard, multiline, done action
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        // IME action — send on keyboard send button
                        keyboardActions = KeyboardActions(
                            onSend = { sendMessage() }
                        ),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Send button
                    FilledIconButton(
                        onClick = { sendMessage() },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier.padding(start = 4.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Keyboard hint bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Klavye: emojis \u2022 sesli \u2022 hızlı yanıt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "\${inputText.length}/500",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (inputText.length > 450) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
