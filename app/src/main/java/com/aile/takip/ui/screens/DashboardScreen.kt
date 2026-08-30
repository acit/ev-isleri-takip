package com.aile.takip.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aile.takip.ui.viewmodel.MainViewModel

data class ModuleCard(val route: String, val icon: String, val title: String, val subtitle: String)

@Composable
fun DashboardScreen(nav: NavController, vm: MainViewModel) {
    // Use derived states from ViewModel instead of collecting raw lists
    // This prevents recomposition when unrelated data changes
    val pendingTasks = vm.pendingTaskCount
    val pendingInvoices = vm.pendingInvoiceCount
    val uncheckedShopping = vm.uncheckedShoppingCount
    val activeNoteCount = vm.activeNoteCount
    val totalMembers = vm.totalMembers
    val mealPlans by vm.mealPlans.collectAsState()
    val clubs by vm.sportsClubs.collectAsState()
    val activeReminders by vm.activeReminders.collectAsState()
    val tasksSize by vm.tasks.collectAsState()
    val shoppingSize by vm.shoppingItems.collectAsState()
    val messagesSize by vm.messages.collectAsState()
    val caloriesSize by vm.calorieLogs.collectAsState()

    val modules = remember(pendingTasks, pendingInvoices, uncheckedShopping, activeNoteCount, totalMembers, mealPlans.size, clubs.size, activeReminders.size, tasksSize.size, shoppingSize.size, messagesSize.size, caloriesSize.size) {
        listOf(
            ModuleCard("tasks", "\uD83D\uDCCB", "Görevler", "$pendingTasks bekleyen"),
            ModuleCard("inventory", "\uD83D\uDCE6", "Envanter", "${tasksSize.size} ürün"),
            ModuleCard("shopping", "\uD83D\uDED2", "Alışveriş", "$uncheckedShopping listede"),
            ModuleCard("budget", "\uD83D\uDCB0", "Bütçe", "Harcama takibi"),
            ModuleCard("invoices", "\uD83E\uDDFE", "Faturalar", "$pendingInvoices bekleyen"),
            ModuleCard("messages", "\uD83D\uDCAC", "Mesajlar", "${messagesSize.size} mesaj"),
            ModuleCard("calendar", "\uD83D\uDCC5", "Takvim", "Olaylar"),
            ModuleCard("meal-plan", "\uD83C\uDF7D\uFE0F", "Yemek", "${mealPlans.size} plan"),
            ModuleCard("sports", "\uD83C\uDFC3", "Spor", "${clubs.size} klüp"),
            ModuleCard("calories", "\uD83D\uDCCA", "Kalori", "${caloriesSize.size} kayıt"),
            ModuleCard("health", "\uD83D\uDCCA", "Sağlık", "Genel bakış"),
            ModuleCard("gamification", "\uD83C\uDFAE", "Oyun", "$totalMembers üye"),
            ModuleCard("mental-load", "\uD83E\uDDE0", "Zihinsel Yük", "Dağılım"),
            ModuleCard("notes", "\uD83D\uDCDD", "Notlar", "$activeNoteCount not"),
            ModuleCard("reminders", "\uD83D\uDD14", "Hatırlatıcılar", "${activeReminders.size} aktif"),
            ModuleCard("menstrual", "\uD83D\uDC95", "Döngü Takibi", "Kişisel"),
            ModuleCard("sync-settings", "\uD83D\uDD04", "Senkron", "Aile verisi"),
            ModuleCard("profile", "\u2699\uFE0F", "Profil", "Ayarlar"),
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("\uD83C\uDFE0 Ana Sayfa", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text("${tasksSize.size} görev, ${shoppingSize.size} ürün, $totalMembers aile üyesi",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(modules, key = { it.route }) { m ->
                Card(
                    modifier = Modifier.clickable { nav.navigate(m.route) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(m.icon, fontSize = 28.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(m.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(m.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
