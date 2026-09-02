package com.aile.takip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showEditExpenseDialog by remember { mutableStateOf(false) }
    
    // Budget form
    var catName by remember { mutableStateOf("") }
    var limitStr by remember { mutableStateOf("") }
    
    // Expense form
    var expCat by remember { mutableStateOf("") }
    var expAmt by remember { mutableStateOf("") }
    var expDesc by remember { mutableStateOf("") }

    val totalLimit = budgets.sumOf { it.monthlyLimit }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val pct = if (totalLimit > 0) (totalSpent / totalLimit).coerceIn(0.0, 1.0).toFloat() else 0f

    PageScaffold("\uD83D\uDCB0", "\uD83D\uDCB0") {
        // Total budget card
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
        
        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAddBudget = true }, shape = RoundedCornerShape(10.dp)) { Text("+ Bütçe") }
            OutlinedButton(onClick = { showAddExpense = true }, shape = RoundedCornerShape(10.dp)) { Text("+ Harcama") }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Budget list
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
                    IconButton(onClick = { 
                        editingBudget = b
                        catName = b.category
                        limitStr = b.monthlyLimit.toString()
                        showEditBudgetDialog = true 
                    }) {
                        Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { vm.deleteBudget(b) }) {
                        Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // Expense list
        SectionTitle("Son Harcamalar (${expenses.size})")
        expenses.take(10).forEach { e ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(e.category, fontWeight = FontWeight.Medium)
                        Text("\u20BA${e.amount.toInt()} - ${e.description.ifEmpty { "Açıklama yok" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { 
                        editingExpense = e
                        expCat = e.category
                        expAmt = e.amount.toString()
                        expDesc = e.description
                        showEditExpenseDialog = true 
                    }) {
                        Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { vm.deleteExpense(e) }) {
                        Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Add Budget Dialog
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
        
        // Add Expense Dialog
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
        
        // Edit Budget Dialog
        if (showEditBudgetDialog && editingBudget != null) {
            AlertDialog(
                onDismissRequest = { 
                    showEditBudgetDialog = false
                    editingBudget = null
                    catName = ""
                    limitStr = ""
                },
                title = { Text("Bütçeyi Düzenle") },
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
                        editingBudget?.let { budget ->
                            vm.updateBudget(budget.copy(category = catName, monthlyLimit = limit))
                        }
                        showEditBudgetDialog = false
                        editingBudget = null
                        catName = ""
                        limitStr = ""
                    }
                }) { Text("Kaydet") } },
                dismissButton = { TextButton(onClick = { 
                    showEditBudgetDialog = false
                    editingBudget = null
                    catName = ""
                    limitStr = ""
                }) { Text("İptal") } }
            )
        }
        
        // Edit Expense Dialog
        if (showEditExpenseDialog && editingExpense != null) {
            AlertDialog(
                onDismissRequest = { 
                    showEditExpenseDialog = false
                    editingExpense = null
                    expCat = ""
                    expAmt = ""
                    expDesc = ""
                },
                title = { Text("Harcamayı Düzenle") },
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
                        editingExpense?.let { expense ->
                            vm.updateExpense(expense.copy(category = expCat, amount = amt, description = expDesc))
                        }
                        showEditExpenseDialog = false
                        editingExpense = null
                        expCat = ""
                        expAmt = ""
                        expDesc = ""
                    }
                }) { Text("Kaydet") } },
                dismissButton = { TextButton(onClick = { 
                    showEditExpenseDialog = false
                    editingExpense = null
                    expCat = ""
                    expAmt = ""
                    expDesc = ""
                }) { Text("İptal") } }
            )
        }
    }
}
