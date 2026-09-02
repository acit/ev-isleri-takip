package com.aile.takip.sync

import com.aile.takip.data.model.*
import com.aile.takip.data.repository.FamilyRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cross-feature synchronization coordinator.
 * Manages relationships between Tasks↔Reminders, Shopping↔Inventory,
 * MealPlan↔Inventory, Calories↔Health, Tasks↔Records.
 */
class SyncCoordinator(private val repo: FamilyRepository) {

    private val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ============================================
    // 1. TASK ↔ REMINDER SYNC
    // ============================================

    /**
     * When a task with dueDate is created, auto-create a reminder.
     */
    suspend fun onTaskCreated(task: Task) {
        if (task.dueDate.isNotEmpty() && task.status != "tamamlanan") {
            val dueTime = parseDateToTimestamp(task.dueDate)
            if (dueTime > System.currentTimeMillis()) {
                repo.upsertReminder(
                    Reminder(
                        title = "Görev: ${task.title}",
                        description = task.description,
                        reminderTime = dueTime - (24 * 60 * 60 * 1000), // 1 gun oncesinden hatirlat
                        repeatType = "once",
                        category = "Görev",
                        priority = task.priority,
                        linkedId = task.id,
                        linkedType = "task",
                        createdBy = task.assignee,
                        nextFireAt = dueTime - (24 * 60 * 60 * 1000)
                    )
                )
            }
        }
    }

    /**
     * When a task is completed, cancel its linked reminder.
     */
    suspend fun onTaskCompleted(task: Task) {
        val reminders = repo.allReminders.first()
        reminders.filter { it.linkedId == task.id && it.linkedType == "task" && !it.isCompleted }
            .forEach { reminder ->
                repo.upsertReminder(reminder.copy(isCompleted = true))
            }
    }

    /**
     * When a reminder fires for a task, update task status.
     */
    suspend fun onReminderFired(reminder: Reminder) {
        if (reminder.linkedType == "task" && reminder.linkedId.isNotEmpty()) {
            val tasks = repo.tasks.first()
            tasks.find { it.id == reminder.linkedId }?.let { task ->
                if (task.status != "tamamlanan") {
                    repo.upsertTask(task.copy(status = "devam"))
                }
            }
        }
    }

    // ============================================
    // 2. SHOPPING ↔ INVENTORY SYNC
    // ============================================

    /**
     * When a shopping item is checked (bought), add to inventory.
     */
    suspend fun onShoppingItemBought(item: ShoppingItem) {
        val inventory = repo.inventory.first()
        val existing = inventory.find {
            it.name.equals(item.name, ignoreCase = true)
        }

        if (existing != null) {
            repo.upsertInventory(existing.copy(quantity = existing.quantity + item.quantity))
        } else {
            repo.upsertInventory(
                InventoryItem(
                    name = item.name,
                    category = item.category,
                    quantity = item.quantity,
                    unit = "adet"
                )
            )
        }
    }

    /**
     * Check inventory and suggest low-stock items for shopping list.
     */
    suspend fun getLowStockSuggestions(): List<String> {
        val inventory = repo.inventory.first()
        val shopping = repo.shoppingItems.first()
        val shoppingNames = shopping.map { it.name.lowercase() }.toSet()

        return inventory.filter { item ->
            item.minStock > 0 && item.quantity <= item.minStock &&
                    item.name.lowercase() !in shoppingNames
        }.map { it.name }
    }

    /**
     * Auto-add low stock items to shopping list.
     */
    suspend fun autoAddLowStockToShopping() {
        val suggestions = getLowStockSuggestions()
        suggestions.forEach { name ->
            repo.upsertShopping(
                ShoppingItem(
                    name = name,
                    quantity = 1,
                    category = "Otomatik",
                    addedBy = "Stok Sistemi"
                )
            )
        }
    }

    // ============================================
    // 3. MEAL PLAN ↔ INVENTORY SYNC
    // ============================================

    /**
     * Check if meal plan ingredients are available in inventory.
     */
    suspend fun checkMealPlanIngredients(dish: String): List<MissingIngredient> {
        val inventory = repo.inventory.first()
        val missing = mutableListOf<MissingIngredient>()

        // Simple ingredient matching (in real app, use ingredient database)
        val requiredIngredients = parseDishIngredients(dish)

        requiredIngredients.forEach { (ingredient, qty) ->
            val inStock = inventory.find {
                it.name.lowercase().contains(ingredient.lowercase())
            }
            if (inStock == null || inStock.quantity < qty) {
                missing.add(
                    MissingIngredient(
                        name = ingredient,
                        needed = qty,
                        inStock = inStock?.quantity ?: 0
                    )
                )
            }
        }

        return missing
    }

    /**
     * When a meal is planned, check inventory and add missing items to shopping.
     */
    suspend fun onMealPlanCreated(mealPlan: MealPlan) {
        val missing = checkMealPlanIngredients(mealPlan.dish)
        val shopping = repo.shoppingItems.first()
        val shoppingNames = shopping.map { it.name.lowercase() }.toSet()

        missing.forEach { ingredient ->
            if (ingredient.name.lowercase() !in shoppingNames) {
                repo.upsertShopping(
                    ShoppingItem(
                        name = ingredient.name,
                        quantity = ingredient.needed - ingredient.inStock,
                        category = "Yemek İçin",
                        addedBy = "Yemek Planı"
                    )
                )
            }
        }
    }

    /**
     * When inventory is consumed, update stock and suggest shopping.
     */
    suspend fun onInventoryConsumed(item: InventoryItem, quantityUsed: Int) {
        val newQuantity = maxOf(0, item.quantity - quantityUsed)
        repo.upsertInventory(item.copy(quantity = newQuantity))

        if (item.minStock > 0 && newQuantity <= item.minStock) {
            autoAddLowStockToShopping()
        }
    }

    // ============================================
    // 4. CALORIES ↔ HEALTH SYNC
    // ============================================

    /**
     * Calculate daily calorie summary for a member.
     */
    suspend fun getDailyCalorieSummary(memberId: String, date: String = todayStr): CalorieSummary {
        val calorieLogs = repo.calorieLogs.first()
        val workoutLogs = repo.workoutLogs.first()

        val dayMeals = calorieLogs.filter { it.memberId == memberId && it.date == date }
        val dayWorkouts = workoutLogs.filter { it.memberId == memberId && it.date == date }

        val totalCalories = dayMeals.sumOf { it.calories }
        val totalProtein = dayMeals.sumOf { it.protein }
        val totalCarbs = dayMeals.sumOf { it.carbs }
        val totalFat = dayMeals.sumOf { it.fat }
        val caloriesBurned = dayWorkouts.sumOf { it.caloriesBurned }

        return CalorieSummary(
            memberId = memberId,
            date = date,
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalCarbs = totalCarbs,
            totalFat = totalFat,
            caloriesBurned = caloriesBurned,
            netCalories = totalCalories - caloriesBurned,
            mealCount = dayMeals.size,
            workoutCount = dayWorkouts.size
        )
    }

    /**
     * Get weekly calorie trend for a member.
     */
    suspend fun getWeeklyCalorieTrend(memberId: String): List<CalorieSummary> {
        val cal = Calendar.getInstance()
        return (0 downTo -6).map { daysAgo ->
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, daysAgo)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            getDailyCalorieSummary(memberId, date)
        }
    }

    /**
     * Check if member is meeting calorie goals.
     */
    suspend fun getCalorieGoalStatus(memberId: String): CalorieGoalStatus {
        val summary = getDailyCalorieSummary(memberId)
        val weeklyTrend = getWeeklyCalorieTrend(memberId)
        val avgCalories = weeklyTrend.map { it.totalCalories }.average()

        return CalorieGoalStatus(
            memberId = memberId,
            todayNetCalories = summary.netCalories,
            weeklyAvgCalories = avgCalories,
            totalProtein = summary.totalProtein,
            totalCarbs = summary.totalCarbs,
            totalFat = summary.totalFat,
            workoutStreak = calculateWorkoutStreak(memberId),
            waterIntakeMl = repo.totalWater(memberId, todayStr).first() ?: 0
        )
    }

    private suspend fun calculateWorkoutStreak(memberId: String): Int {
        val workoutLogs = repo.workoutLogs.first()
        var streak = 0
        val cal = Calendar.getInstance()

        for (i in 0..30) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            val hasWorkout = workoutLogs.any { it.memberId == memberId && it.date == date }
            if (hasWorkout) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    // ============================================
    // 5. TASKS ↔ RECORDS SYNC
    // ============================================

    /**
     * When an invoice is created, optionally create a task for payment.
     */
    suspend fun onInvoiceCreated(invoice: Invoice) {
        if (invoice.dueDate.isNotEmpty() && invoice.status == "pending") {
            val dueTime = parseDateToTimestamp(invoice.dueDate)
            val reminderTime = dueTime - (2 * 24 * 60 * 60 * 1000) // 2 gun oncesinden

            if (reminderTime > System.currentTimeMillis()) {
                repo.upsertReminder(
                    Reminder(
                        title = "Fatura Öde: ${invoice.title}",
                        description = "${invoice.amount} TL - ${invoice.category}",
                        reminderTime = reminderTime,
                        repeatType = "once",
                        category = "Fatura",
                        priority = "yüksek",
                        linkedId = invoice.id,
                        linkedType = "invoice",
                        createdBy = invoice.createdBy,
                        nextFireAt = reminderTime
                    )
                )
            }
        }
    }

    /**
     * When an expense is recorded, update budget.
     */
    suspend fun onExpenseRecorded(expense: Expense) {
        if (expense.budgetId.isNotEmpty()) {
            val budgets = repo.budgets.first()
            budgets.find { it.id == expense.budgetId }?.let { budget ->
                repo.upsertBudget(budget.copy(spentAmount = budget.spentAmount + expense.amount))
            }
        }
    }

    /**
     * Get linked records for a task.
     */
    suspend fun getLinkedRecords(taskId: String): LinkedRecords {
        val reminders = repo.allReminders.first()
        val invoices = repo.invoices.first()

        return LinkedRecords(
            taskId = taskId,
            reminders = reminders.filter { it.linkedId == taskId && it.linkedType == "task" },
            invoices = invoices.filter { it.id == taskId }
        )
    }

    // ============================================
    // HELPER FUNCTIONS
    // ============================================

    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun parseDishIngredients(dish: String): List<Pair<String, Int>> {
        // Simple ingredient parsing - in real app, use a recipe database
        val ingredients = mutableListOf<Pair<String, Int>>()

        val commonIngredients = mapOf(
            "makarna" to listOf("Makarna", "Zeytinyağı", "Tuz"),
            "pilav" to listOf("Pirinç", "Tereyağı", "Tuz"),
            "salata" to listOf("Marul", "Domates", "Salatalık", "Zeytinyağı"),
            "çorba" to listOf("Tavuk", "Soğan", "Havuç", "Tuz"),
            "karnıyarık" to listOf("Patlıcan", "Kıyma", "Soğan", "Domates"),
            "izmir köfte" to listOf("Kıyma", "Patates", "Soğan", "Domates"),
            "tavuk" to listOf("Tavuk", "Tuz", "Baharat"),
            "balık" to listOf("Balık", "Limon", "Tuz"),
            "omlet" to listOf("Yumurta", "Peynir", "Maydanoz"),
            "pankek" to listOf("Un", "Yumurta", "Süt", "Şeker")
        )

        commonIngredients.forEach { (key, items) ->
            if (dish.lowercase().contains(key)) {
                items.forEach { item ->
                    ingredients.add(item to 1)
                }
            }
        }

        return ingredients.ifEmpty {
            listOf("Malzeme" to 1)
        }
    }
}

// ============================================
// DATA CLASSES
// ============================================

data class MissingIngredient(
    val name: String,
    val needed: Int,
    val inStock: Int
)

data class CalorieSummary(
    val memberId: String,
    val date: String,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val caloriesBurned: Int,
    val netCalories: Int,
    val mealCount: Int,
    val workoutCount: Int
)

data class CalorieGoalStatus(
    val memberId: String,
    val todayNetCalories: Int,
    val weeklyAvgCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val workoutStreak: Int,
    val waterIntakeMl: Int
)

data class LinkedRecords(
    val taskId: String,
    val reminders: List<Reminder>,
    val invoices: List<Invoice>
)
