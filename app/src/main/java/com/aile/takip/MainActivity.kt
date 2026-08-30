package com.aile.takip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.aile.takip.notification.ReminderWorker
import com.aile.takip.ui.screens.*
import com.aile.takip.ui.theme.*
import com.aile.takip.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule reminder check worker
        ReminderWorker.schedulePeriodicCheck(this)

        setContent {
            var darkMode by remember { mutableStateOf(false) }
            AileTakipTheme(darkTheme = darkMode) {
                MainScreen(darkMode) { darkMode = it }
            }
        }
    }
}

@Composable
fun MainScreen(darkMode: Boolean, onToggleDark: (Boolean) -> Unit) {
    val vm: MainViewModel = viewModel()
    val isLoggedIn by vm.isAuthenticated

    if (isLoggedIn.not()) {
        LoginScreen(vm)
    } else {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Triple("dashboard", "Ana Sayfa", "\uD83C\uDFE0"),
                        Triple("tasks", "Görevler", "\uD83D\uDCCB"),
                        Triple("invoices", "Faturalar", "\uD83E\uDDFE"),
                        Triple("shopping", "Alışveriş", "\uD83D\uDED2"),
                        Triple("messages", "Mesajlar", "\uD83D\uDCAC"),
                    )
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Text(icon, style = MaterialTheme.typography.titleLarge) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
                composable("dashboard") { DashboardScreen(navController, vm) }
                composable("tasks") { TasksScreen(vm) }
                composable("inventory") { InventoryScreen(vm) }
                composable("budget") { BudgetScreen(vm) }
                composable("invoices") { InvoiceScreen(vm) }
                composable("messages") { MessagesScreen(vm) }
                composable("shopping") { ShoppingScreen(vm) }
                composable("calendar") { CalendarScreen() }
                composable("profile") { ProfileScreen(darkMode, onToggleDark, vm) }
                composable("meal-plan") { MealPlanScreen(vm) }
                composable("gamification") { GamificationScreen(vm) }
                composable("mental-load") { MentalLoadScreen(vm) }
                composable("notes") { NotesScreen(vm) }
                composable("reminders") { ReminderScreen(vm) }
                composable("health") { HealthDashboardScreen(vm) }
                // Yeni modüller
                composable("sports") { SportsClubScreen(vm) }
                composable("calories") { CalorieScreen(vm) }
                composable("menstrual") { MenstrualCycleScreen(vm) }
                composable("sync-settings") { SyncSettingsScreen(vm) }
            }
        }
    }
}
