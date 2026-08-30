package com.aile.takip.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aile.takip.AileTakipApp
import com.aile.takip.data.model.*
import com.aile.takip.data.repository.FamilyRepository
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.aile.takip.sync.FirebaseSyncService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as AileTakipApp).db
    private val repo = FamilyRepository(
        db.taskDao(), db.inventoryDao(), db.budgetDao(), db.expenseDao(),
        db.invoiceDao(), db.messageDao(), db.shoppingDao(),
        db.memberDao(), db.mealPlanDao(),
        db.sportsClubDao(), db.workoutLogDao(), db.calorieLogDao(),
        db.menstrualCycleDao(), db.authDao(), db.syncEventDao()
    )

    private val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Firebase Sync
    private val syncService = FirebaseSyncService(db)
    val syncState = syncService.syncState
    val lastSyncTime = syncService.lastSyncTime
    val syncedTables = syncService.syncedTables
    val syncEnabled = mutableStateOf(false)
    val familyGroupId = mutableStateOf("")

    // Auth state
    val auth = repo.auth.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    var isAuthenticated = mutableStateOf(false)
    var selectedMemberId = mutableStateOf<String?>(null)

    // Existing data
    val tasks = repo.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inventory = repo.inventory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repo.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repo.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val invoices = repo.invoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val messages = repo.messages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shoppingItems = repo.shoppingItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val members = repo.members.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mealPlans = repo.mealPlans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // New data
    val sportsClubs = repo.sportsClubs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val workoutLogs = repo.workoutLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val calorieLogs = repo.calorieLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val menstrualCycles = repo.menstrualCycles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unsyncedEvents = repo.unsyncedEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Check if PIN exists
        viewModelScope.launch {
            val a = repo.getAuthOnce()
            if (a == null || a.pin.isEmpty()) {
                isAuthenticated.value = true // No PIN set = auto-login
            }
        }
    }

    // ===== AUTH =====
    fun login(pin: String) {
        viewModelScope.launch {
            val a = repo.getAuthOnce()
            if (a == null || a.pin == pin) isAuthenticated.value = true
        }
    }
    fun setupPin(pin: String, name: String, email: String) {
        viewModelScope.launch { repo.upsertAuth(UserAuth(pin = pin, name = name, email = email)) }
    }
    fun resetPin() {
        viewModelScope.launch { repo.upsertAuth(UserAuth(id = "main_user", pin = "", name = "", email = "")); isAuthenticated.value = true }
    }
    fun logout() { isAuthenticated.value = false }

    // ===== TASKS =====
    fun addTask(title: String, description: String = "", category: String = "Genel", priority: String = "orta", assignee: String = "", dueDate: String = "") {
        viewModelScope.launch {
            repo.upsertTask(Task(title = title, description = description, category = category, priority = priority, assignee = assignee, dueDate = dueDate))
            addSyncEvent("tasks", "insert")
        }
    }
    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val newStatus = if (task.status == "tamamlanan") "bekleyen" else "tamamlanan"
            repo.upsertTask(task.copy(status = newStatus, completedAt = if (newStatus == "tamamlanan") System.currentTimeMillis() else null))
            addSyncEvent("tasks", "update")
        }
    }
    fun deleteTask(task: Task) { viewModelScope.launch { repo.deleteTask(task); addSyncEvent("tasks", "delete") } }

    // ===== INVENTORY =====
    fun addInventory(name: String, category: String = "Genel", quantity: Int = 1, unit: String = "adet", minStock: Int = 0, location: String = "") {
        viewModelScope.launch { repo.upsertInventory(InventoryItem(name = name, category = category, quantity = quantity, unit = unit, minStock = minStock, location = location)); addSyncEvent("inventory", "insert") }
    }
    fun updateInventory(item: InventoryItem) { viewModelScope.launch { repo.upsertInventory(item); addSyncEvent("inventory", "update") } }
    fun deleteInventory(item: InventoryItem) { viewModelScope.launch { repo.deleteInventory(item); addSyncEvent("inventory", "delete") } }

    // ===== BUDGETS =====
    fun addBudget(category: String, limit: Double, monthYear: String) {
        viewModelScope.launch { repo.upsertBudget(Budget(category = category, monthlyLimit = limit, monthYear = monthYear)); addSyncEvent("budgets", "insert") }
    }
    fun deleteBudget(b: Budget) { viewModelScope.launch { repo.deleteBudget(b); addSyncEvent("budgets", "delete") } }

    // ===== EXPENSES =====
    fun addExpense(category: String, amount: Double, description: String = "", date: String, budgetId: String = "") {
        viewModelScope.launch { repo.upsertExpense(Expense(category = category, amount = amount, description = description, expenseDate = date, budgetId = budgetId)); addSyncEvent("expenses", "insert") }
    }
    fun deleteExpense(e: Expense) { viewModelScope.launch { repo.deleteExpense(e) } }

    // ===== INVOICES =====
    fun addInvoice(title: String, amount: Double, category: String = "Genel", dueDate: String = "", notes: String = "", imageBase64: String? = null) {
        viewModelScope.launch { repo.upsertInvoice(Invoice(title = title, amount = amount, category = category, dueDate = dueDate, notes = notes, imageBase64 = imageBase64)); addSyncEvent("invoices", "insert") }
    }
    fun toggleInvoiceStatus(invoice: Invoice) {
        viewModelScope.launch {
            val newStatus = if (invoice.status == "paid") "pending" else "paid"
            repo.upsertInvoice(invoice.copy(status = newStatus)); addSyncEvent("invoices", "update")
        }
    }
    fun deleteInvoice(i: Invoice) { viewModelScope.launch { repo.deleteInvoice(i); addSyncEvent("invoices", "delete") } }

    // ===== MESSAGES =====
    fun sendMessage(content: String, senderName: String = "Ben") {
        viewModelScope.launch { repo.upsertMessage(Message(senderName = senderName, senderId = "self", content = content)); addSyncEvent("messages", "insert") }
    }
    fun deleteMessage(m: Message) { viewModelScope.launch { repo.deleteMessage(m) } }

    // ===== SHOPPING =====
    fun addShoppingItem(name: String, quantity: Int = 1, category: String = "Market", addedBy: String = "") {
        viewModelScope.launch { repo.upsertShopping(ShoppingItem(name = name, quantity = quantity, category = category, addedBy = addedBy)); addSyncEvent("shopping", "insert") }
    }
    fun toggleShoppingItem(item: ShoppingItem) {
        viewModelScope.launch { repo.upsertShopping(item.copy(checked = !item.checked)); addSyncEvent("shopping", "update") }
    }
    fun deleteShopping(item: ShoppingItem) { viewModelScope.launch { repo.deleteShopping(item); addSyncEvent("shopping", "delete") } }

    // ===== MEMBERS =====
    fun addMember(name: String, role: String = "Üye", color: String = "#3498DB") {
        viewModelScope.launch { repo.upsertMember(FamilyMember(name = name, role = role, color = color)); addSyncEvent("family_members", "insert") }
    }
    fun addPoints(member: FamilyMember, points: Int) {
        viewModelScope.launch { repo.upsertMember(member.copy(points = member.points + points)); addSyncEvent("family_members", "update") }
    }
    fun deleteMember(m: FamilyMember) { viewModelScope.launch { repo.deleteMember(m); addSyncEvent("family_members", "delete") } }

    // ===== MEAL PLANS =====
    fun addMealPlan(dayOfWeek: Int, mealType: String, dish: String, notes: String = "") {
        viewModelScope.launch { repo.upsertMealPlan(MealPlan(dayOfWeek = dayOfWeek, mealType = mealType, dish = dish, notes = notes)); addSyncEvent("meal_plans", "insert") }
    }
    fun deleteMealPlan(mp: MealPlan) { viewModelScope.launch { repo.deleteMealPlan(mp); addSyncEvent("meal_plans", "delete") } }

    // ===== SPORTS CLUBS =====
    fun addClub(name: String, type: String, address: String = "", phone: String = "", fee: Double = 0.0, memberId: String = "", startDate: String = "", endDate: String = "") {
        viewModelScope.launch {
            repo.upsertClub(SportsClub(name = name, type = type, address = address, phone = phone, monthlyFee = fee, memberId = memberId, membershipStart = startDate, membershipEnd = endDate))
            addSyncEvent("sports_clubs", "insert")
        }
    }
    fun deleteClub(c: SportsClub) { viewModelScope.launch { repo.deleteClub(c); addSyncEvent("sports_clubs", "delete") } }

    // ===== WORKOUT LOGS =====
    fun addWorkout(clubId: String, memberId: String, workoutType: String, duration: Int, calories: Int, date: String, notes: String = "") {
        viewModelScope.launch {
            repo.upsertWorkout(WorkoutLog(clubId = clubId, memberId = memberId, workoutType = workoutType, duration = duration, caloriesBurned = calories, date = date, notes = notes))
            addSyncEvent("workout_logs", "insert")
        }
    }
    fun deleteWorkout(w: WorkoutLog) { viewModelScope.launch { repo.deleteWorkout(w) } }

    // ===== CALORIE LOGS =====
    fun addCalorie(memberId: String, mealType: String, foodName: String, calories: Int, protein: Double = 0.0, carbs: Double = 0.0, fat: Double = 0.0, serving: String = "", date: String = todayStr) {
        viewModelScope.launch {
            repo.upsertCalorie(CalorieLog(memberId = memberId, mealType = mealType, foodName = foodName, calories = calories, protein = protein, carbs = carbs, fat = fat, servingSize = serving, date = date))
            addSyncEvent("calorie_logs", "insert")
        }
    }
    fun deleteCalorie(c: CalorieLog) { viewModelScope.launch { repo.deleteCalorie(c) } }

    // ===== MENSTRUAL CYCLES =====
    fun addCycle(memberId: String, startDate: String, cycleLength: Int = 28, periodLength: Int = 5, symptoms: String = "", mood: String = "", flow: String = "", isPersonal: Boolean = true, notes: String = "") {
        viewModelScope.launch {
            repo.upsertCycle(MenstrualCycle(memberId = memberId, startDate = startDate, cycleLength = cycleLength, periodLength = periodLength, symptoms = symptoms, mood = mood, flow = flow, isPersonal = isPersonal, notes = notes))
            addSyncEvent("menstrual_cycles", "insert")
        }
    }
    fun deleteCycle(c: MenstrualCycle) { viewModelScope.launch { repo.deleteCycle(c) } }

    // ===== SYNC =====
    private fun addSyncEvent(table: String, action: String) {
        viewModelScope.launch {
            repo.upsertSyncEvent(SyncEvent(tableName = table, recordId = UUID.randomUUID().toString(), action = action))
        }
    }

    // Export all data as JSON for sync
    fun exportAllData(): String {
        val data = mapOf(
            "tasks" to tasks.value,
            "inventory" to inventory.value,
            "budgets" to budgets.value,
            "expenses" to expenses.value,
            "invoices" to invoices.value,
            "messages" to messages.value,
            "shopping" to shoppingItems.value,
            "members" to members.value,
            "mealPlans" to mealPlans.value,
            "sportsClubs" to sportsClubs.value,
            "workoutLogs" to workoutLogs.value,
            "calorieLogs" to calorieLogs.value,
            "menstrualCycles" to menstrualCycles.value,
            "exportDate" to todayStr,
            "appVersion" to "3.1.0"
        )
        return com.google.gson.Gson().toJson(data)
    }

    // ===== FIREBASE SYNC =====
    fun connectToFirebase(groupId: String) {
        familyGroupId.value = groupId
        viewModelScope.launch {
            syncEnabled.value = syncService.connect(groupId)
        }
    }

    fun syncToFirebase() {
        viewModelScope.launch {
            syncService.pushAll()
        }
    }

    fun disconnectFirebase() {
        syncService.disconnect()
        syncEnabled.value = false
    }

    override fun onCleared() {
        super.onCleared()
        syncService.stopListening()
    }
}
