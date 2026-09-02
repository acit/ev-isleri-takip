package com.aile.takip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.aile.takip.R
import com.aile.takip.notification.ReminderWorker
import com.aile.takip.ui.screens.*
import com.aile.takip.ui.theme.*
import com.aile.takip.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply saved locale
        com.aile.takip.utils.LocaleHelper.onAttach(this)

        // Schedule reminder check worker
        ReminderWorker.schedulePeriodicCheck(this)

        setContent {
            val vm: MainViewModel = viewModel()
            val useDynamicColor by vm.useDynamicColor
            val useSystemTheme by vm.useSystemTheme
            val isDarkMode by vm.isDarkMode
            
            // Determine dark theme based on user preference or system setting
            val darkTheme = when {
                useSystemTheme -> isSystemInDarkTheme()
                else -> isDarkMode
            }

            AileTakipTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor
            ) {
                MainScreen(vm)
            }
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val isLoggedIn by vm.isAuthenticated

    if (isLoggedIn.not()) {
        LoginScreen(vm)
    } else {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Triple("dashboard", androidx.compose.ui.res.stringResource(R.string.nav_home), "\uD83C\uDFE0"),
                        Triple("tasks", androidx.compose.ui.res.stringResource(R.string.nav_tasks), "\uD83D\uDCCB"),
                        Triple("shopping", androidx.compose.ui.res.stringResource(R.string.nav_shopping), "\uD83D\uDED2"),
                        Triple("messages", androidx.compose.ui.res.stringResource(R.string.nav_messages), "\uD83D\uDCAC"),
                        Triple("profile", androidx.compose.ui.res.stringResource(R.string.nav_profile), "\uD83D\uDC64"),
                    )
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    items.forEach { (route, title, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Text(icon) },
                            label = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(navController, startDestination = "dashboard", Modifier.padding(padding)) {
                composable("dashboard") { DashboardScreen(navController, vm) }
                composable("tasks") { TasksScreen(vm) }
                composable("shopping") { ShoppingScreen(vm = vm, navController = navController) }
                composable("messages") { MessagesScreen(vm) }
                composable("profile") { ProfileScreen(vm) }
                composable("notes") { NotesScreen(vm) }
                composable("reminders") { ReminderScreen(vm) }
                composable("health") { HealthDashboardScreen(vm) }
                composable("budget") { BudgetScreen(vm) }
                composable("inventory") { InventoryScreen(vm) }
                composable("calendar") { CalendarScreen() }
                composable("calorie") { CalorieScreen(vm) }
                composable("sports") { SportsClubScreen(vm) }
                composable("invoices") { InvoiceScreen(vm) }
                composable("mealplan") { MealPlanScreen(vm) }
                composable("meal-plan") { MealPlanScreen(vm) }
                composable("gamification") { GamificationScreen(vm) }
                composable("mental") { MentalLoadScreen(vm) }
                composable("mental-load") { MentalLoadScreen(vm) }
                composable("calorie") { CalorieScreen(vm) }
                composable("calories") { CalorieScreen(vm) }
                composable("menstrual") { MenstrualCycleScreen(vm) }
                composable("sync") { SyncSettingsScreen(vm, navController) }
                composable("sync-settings") { SyncSettingsScreen(vm, navController) }
                composable("scanner/{mode}") { backStackEntry ->
                    val mode = backStackEntry.arguments?.getString("mode") ?: "any"
                    BarcodeScannerScreen(
                        scanMode = when (mode) {
                            "barcode" -> ScanMode.BARCODE
                            "qr" -> ScanMode.QR_CODE
                            else -> ScanMode.ANY
                        },
                        onResult = { result ->
                            vm.lastScanResult.value = result.rawValue
                            navController.previousBackStackEntry?.savedStateHandle?.set("scan_result", result.rawValue)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
