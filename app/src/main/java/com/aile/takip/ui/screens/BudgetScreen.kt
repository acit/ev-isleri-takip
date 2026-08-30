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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aile.takip.data.model.Budget
import com.aile.takip.data.model.Expense
import com.aile.takip.ui.components.*
import com.aile.takip.ui.viewmodel.MainViewModel

@Composable
fun BudgetScreen(vm: MainViewModel) {
    val budgets by vm.budgets.collectAsState()
    val expenses by vm.expenses.collectAsState()
    var showAddBudget by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var catName by remember { mutableStateOf("") }
    var limitStr by remember { mutableStateOf("") }
    var expCat by remember { mutableStateOf("") }
    var expAmt by remember { mutableStateOf("") }
    var expDesc by remember { mutableStateOf("") }

    val totalLimit = budgets.sumOf { it.monthlyLimit }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val pct = if (totalLimit > 0) (totalSpent / totalLimit).coerceIn(0.0, 1.0).toFloat() else 0f

    PageScaffold("\uD83D\uDCB0", "\uD83D\uDCB0") {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Aylık Bütçe", fontWeight = FontWeight.SemiBold)
                    Text("\u20BA${totalSpent.toInt()} / \u20BA${totalLimit.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = when { pct > 0.8f -> Color(0xFFE74C3C); pct > 0.6f -> Color(0xFFF39C12); else -> Color(0xFF2ECC71) },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAddBudget = true }, shape = RoundedCornerShape(10.dp)) { Text("+ Bütçe") }
            OutlinedButton(onClick = { showAddExpense = true }, shape = RoundedCornerShape(10.dp)) { Text("+ Harcama") }
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle("Bütçeler (${budgets.size})")
        budgets.forEach { b ->
            val bSpent = expenses.filter { it.category == b.category }.sumOf { it.amount }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(b.category, fontWeight = FontWeight.SemiBold)
                        Text("\u20BA${bSpent.toInt()} / \u20BA${b.monthlyLimit.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (bSpent / b.monthlyLimit).coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    IconButton(onClick = { vm.deleteBudget(b) }) {
                        Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (showAddBudget) {
            AlertDialog(
                onDismissRequest = { showAddBudget = false },
                title = { Text("Bütçe Ekle") },
                text = {
                    Column {
                        OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Kategori") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = limitStr, onValueChange = { limitStr = it }, label = { Text("Aylık Limit (\u20BA)") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val limit = limitStr.toDoubleOrNull()
                    if (catName.isNotBlank() && limit != null) {
                        vm.addBudget(catName, limit, "2026"); catName = ""; limitStr = ""; showAddBudget = false
                    }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showAddBudget = false }) { Text("İptal") } }
            )
        }
        if (showAddExpense) {
            AlertDialog(
                onDismissRequest = { showAddExpense = false },
                title = { Text("Harcama Ekle") },
                text = {
                    Column {
                        OutlinedTextField(value = expCat, onValueChange = { expCat = it }, label = { Text("Kategori") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = expAmt, onValueChange = { expAmt = it }, label = { Text("Tutar (\u20BA)") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = expDesc, onValueChange = { expDesc = it }, label = { Text("Açıklama") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    val amt = expAmt.toDoubleOrNull()
                    if (expCat.isNotBlank() && amt != null) {
                        vm.addExpense(expCat, amt, expDesc, "2026"); expCat = ""; expAmt = ""; expDesc = ""; showAddExpense = false
                    }
                }) { Text("Ekle") } },
                dismissButton = { TextButton(onClick = { showAddExpense = false }) { Text("İptal") } }
            )
        }
    }
}
