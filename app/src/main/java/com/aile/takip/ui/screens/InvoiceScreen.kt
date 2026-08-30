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
import com.aile.takip.ui.components.*
import com.aile.takip.ui.viewmodel.MainViewModel

@Composable
fun InvoiceScreen(vm: MainViewModel) {
    val invoices by vm.invoices.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Genel") }
    var dueDate by remember { mutableStateOf("") }

    val totalPending = invoices.filter { it.status == "pending" }.sumOf { it.amount }
    val totalPaid = invoices.filter { it.status == "paid" }.sumOf { it.amount }

    PageScaffold("\uD83E\uDDFE", "\uD83E\uDDFE") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("\uD83D\uDCC9", "\u20BA${totalPending.toInt()}", "Bekleyen", Color(0xFFF39C12))
            StatCard("\u2705", "\u20BA${totalPaid.toInt()}", "Ödenen", Color(0xFF2ECC71))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Fatura Ekle")
        }
        Spacer(Modifier.height(12.dp))

        if (invoices.isEmpty()) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83E\uDDFE", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz fatura eklenmemiş", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(invoices, key = { it.id }) { invoice ->
                val isPaid = invoice.status == "paid"
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(invoice.title, fontWeight = FontWeight.SemiBold)
                            Text("${invoice.category} \u00B7 ${invoice.dueDate.ifEmpty { "Tarih yok" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            AssistChip(onClick = { vm.toggleInvoiceStatus(invoice) }, label = { Text(if (isPaid) "\u2705 Ödendi" else "\u23F3 Bekliyor") })
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("\u20BA${invoice.amount.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { vm.deleteInvoice(invoice) }) {
                                Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Fatura Ekle") },
                text = {
                    Column {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Başlık") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Tutar (\u20BA)") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Kategori") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Son Ödeme (GG/AA/YYYY)") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (title.isNotBlank() && amt != null) {
                        vm.addInvoice(title, amt, category, dueDate); title = ""; amount = ""; category = "Genel"; dueDate = ""; showDialog = false
                    }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("İptal") } }
            )
        }
    }
}
