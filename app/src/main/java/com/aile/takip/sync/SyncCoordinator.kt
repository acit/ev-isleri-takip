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

    private companion object {
        /** Görev hatırlatıcısı: son tarihten 1 gün önce. */
        const val REMINDER_LEAD_TASK_MS = 24 * 60 * 60 * 1000L
        /** Fatura hatırlatıcısı: son ödemeden 2 gün önce. */
        const val REMINDER_LEAD_INVOICE_MS = 2 * 24 * 60 * 60 * 1000L
    }

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
                        reminderTime = dueTime - REMINDER_LEAD_TASK_MS, // 1 gun oncesinden hatirlat
                        repeatType = "once",
                        category = "Görev",
                        priority = task.priority,
                        linkedId = task.id,
                        linkedType = "task",
                        createdBy = task.assignee,
                        nextFireAt = dueTime - REMINDER_LEAD_TASK_MS
                    )
                )
            }
        }
    }

    /**
     * Görev güncellenince bağlı hatırlatıcıyı da günceller.
     * Başlık/açıklama/tarih değişimi hatırlatıcıya yansır; tamamlandıysa iptal edilir.
     */
    suspend fun onTaskUpdated(task: Task) {
        val reminders = repo.allReminders.first()
        reminders.filter { it.linkedId == task.id && it.linkedType == "task" && !it.isCompleted }
            .forEach { reminder ->
                if (task.status == "tamamlanan") {
                    repo.upsertReminder(reminder.copy(isCompleted = true))
                } else {
                    val dueTime = parseDateToTimestamp(task.dueDate)
                    val fireAt = dueTime - REMINDER_LEAD_TASK_MS
                    repo.upsertReminder(
                        reminder.copy(
                            title = "Görev: ${task.title}",
                            description = task.description,
                            reminderTime = fireAt,
                            nextFireAt = fireAt,
                            priority = task.priority
                        )
                    )
                }
            }
    }

    /**
     * Görev silinince bağlı hatırlatıcıları da siler.
     */
    suspend fun onTaskDeleted(task: Task) {
        val reminders = repo.allReminders.first()
        reminders.filter { it.linkedId == task.id && it.linkedType == "task" }
            .forEach { reminder -> repo.deleteReminder(reminder) }
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
     * Envanter kaydı düşük stok eşiğine düştüğünde otomatik hatırlatıcı oluşturur.
     * Aynı envanter için aktif hatırlatıcı varsa tekrar oluşturmaz.
     */
    suspend fun onInventoryLowStock(item: InventoryItem) {
        if (item.minStock <= 0 || item.quantity > item.minStock) return
        val reminders = repo.allReminders.first()
        val alreadyActive = reminders.any {
            it.linkedId == item.id && it.linkedType == "inventory" && !it.isCompleted
        }
        if (alreadyActive) return
        repo.upsertReminder(
            Reminder(
                title = "Stok az: ${item.name}",
                description = "Kalan: ${item.quantity} ${item.unit} • Eşik: ${item.minStock} ${item.unit} — alışveriş listesine eklendi",
                reminderTime = System.currentTimeMillis(),
                category = "Stok",
                priority = "orta",
                linkedId = item.id,
                linkedType = "inventory",
                nextFireAt = System.currentTimeMillis()
            )
        )
        // Düşük stok ürünü alışveriş listesine de düşür (varsa tekrar eklemez)
        autoAddLowStockToShopping()
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
     * Gerçek eşleştirme: Türkçe normalizasyon + karşılıklı kelime kapsaması ([IngredientMatcher]).
     */
    suspend fun checkMealPlanIngredients(dish: String): List<MissingIngredient> {
        val inventory = repo.inventory.first()
        val invPairs = inventory.map { it.name to it.quantity }
        return com.aile.takip.utils.IngredientMatcher.missingFor(dish, invPairs)
            .map { MissingIngredient(name = it.requirement.name, needed = it.requirement.needed, inStock = it.inStock) }
    }

    /**
     * Tüm yemek planı için eksik malzeme özetini üretir.
     *
     * Aynı malzeme birden fazla yemekte gerekiyorsa gereksinimler TOPLANIR;
     * envanterdeki toplam stok bir kez düşülür. Sonuç yemek başına gerekçe içerir
     * ("Hangi yemek için lazım?") ve UI'da tekil/toplu ekleme için kullanılır.
     */
    suspend fun weeklyMissingIngredients(): List<AggregateMissing> {
        val plans = repo.mealPlans.first()
        if (plans.isEmpty()) return emptyList()
        val inventory = repo.inventory.first()
        val invPairs = inventory.map { it.name to it.quantity }.toMutableList()

        // malzeme -> (toplam gereken, hangi yemekler)
        val needed = LinkedHashMap<String, RequirementAccumulator>()

        for (plan in plans) {
            val reqs = com.aile.takip.utils.IngredientMatcher.extractIngredients(plan.dish)
            for (req in reqs) {
                val entry = needed.getOrPut(req.name) { RequirementAccumulator() }
                entry.totalNeeded += req.needed
                if (plan.dish !in entry.dishes) entry.dishes.add(plan.dish)
            }
        }

        return needed.map { (name, acc) ->
            val stock = com.aile.takip.utils.IngredientMatcher.matchWithInventory(
                listOf(com.aile.takip.utils.IngredientMatcher.Requirement(name, acc.totalNeeded)),
                invPairs
            ).firstOrNull()
            AggregateMissing(
                name = name,
                needed = acc.totalNeeded,
                inStock = stock?.inStock ?: 0,
                usedByDishes = acc.dishes.toList()
            )
        }.filter { it.missing > 0 }
            .sortedWith(compareByDescending<AggregateMissing> { it.missing }.thenBy { it.name })
    }

    /** Map getOrPut için biriktirici. */
    private class RequirementAccumulator {
        var totalNeeded: Int = 0
        val dishes: MutableList<String> = mutableListOf()
    }

    /**
     * Bu haftanın market listesini üretir (eksiklerden) ve DataStore'a kaydeder;
     * önceki haftayla karşılaştırma özetini döner.
     */
    suspend fun generateWeeklyMarketList(
        persistence: MarketListPersistence
    ): com.aile.takip.utils.MarketListStore.WeekDiff {
        val missing = weeklyMissingIngredients()
        val now = System.currentTimeMillis()
        val weekKey = com.aile.takip.utils.MarketListStore.currentWeekKey(now)

        val current = com.aile.takip.utils.MarketListStore.buildList(
            weekKey = weekKey,
            now = now,
            exhausted = missing.map { Triple(it.name, it.missing, it.usedByDishes) }
        )
        persistence.save(current)

        val previous = persistence.get(
            com.aile.takip.utils.MarketListStore.previousWeekKey(now)
        )
        return com.aile.takip.utils.MarketListStore.compare(current, previous)
    }

    /**
     * Eksik malzemeleri alışveriş listesine ekler (zaten listede olanlar atlanır).
     * [names] boşsa TÜM eksikler eklenir. Eklenen malzeme sayısını döner.
     */
    suspend fun addMissingToShopping(names: Collection<String> = emptyList()): Int {
        val missing = weeklyMissingIngredients()
            .filter { names.isEmpty() || it.name in names }
        val shopping = repo.shoppingItems.first()
        val existing = shopping.map { it.name.lowercase() }.toSet()

        var added = 0
        for (m in missing) {
            if (m.name.lowercase() in existing) continue
            repo.upsertShopping(
                ShoppingItem(
                    name = m.name,
                    quantity = m.missing,
                    category = "Yemek İçin",
                    addedBy = "Akıllı Öneri"
                )
            )
            added++
        }
        return added
    }

    /**
     * When a meal is planned, check inventory and add missing items to shopping.
     * (Otomatik eklenmez; yalnızca hatırlatılır — kullanıcı MealPlanScreen'den
     * tekil veya toplu ekler. Bu, istenmeyen liste kirliliğini önler.)
     */
    suspend fun onMealPlanCreated(mealPlan: MealPlan) {
        // bilinçli olarak boş: öneriler kullanıcı onayıyla eklenir
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
     * Fatura güncellenince bağlı hatırlatıcıyı yeniler; ödendiyse iptal eder.
     */
    suspend fun onInvoiceUpdated(invoice: Invoice) {
        val reminders = repo.allReminders.first()
        reminders.filter { it.linkedId == invoice.id && it.linkedType == "invoice" && !it.isCompleted }
            .forEach { reminder ->
                if (invoice.status == "paid") {
                    repo.upsertReminder(reminder.copy(isCompleted = true))
                } else {
                    val dueTime = parseDateToTimestamp(invoice.dueDate)
                    val fireAt = dueTime - REMINDER_LEAD_INVOICE_MS
                    repo.upsertReminder(
                        reminder.copy(
                            title = "Fatura Öde: ${invoice.title}",
                            description = "${invoice.amount} TL - ${invoice.category}",
                            reminderTime = fireAt,
                            nextFireAt = fireAt,
                            priority = "yüksek"
                        )
                    )
                }
            }
    }

    /**
     * Fatura silinince bağlı hatırlatıcıları siler.
     */
    suspend fun onInvoiceDeleted(invoice: Invoice) {
        val reminders = repo.allReminders.first()
        reminders.filter { it.linkedId == invoice.id && it.linkedType == "invoice" }
            .forEach { reminder -> repo.deleteReminder(reminder) }
    }

    /**
     * When an invoice is created, optionally create a task for payment.
     */
    suspend fun onInvoiceCreated(invoice: Invoice) {
        if (invoice.dueDate.isNotEmpty() && invoice.status == "pending") {
            val dueTime = parseDateToTimestamp(invoice.dueDate)
            val reminderTime = dueTime - REMINDER_LEAD_INVOICE_MS

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

/**
 * Haftalık eksik malzeme özeti: birden fazla yemekte gereken malzemeler toplanır,
 * envanter stokları bir kez düşülür ve hangi yemekler için gerektiği saklanır.
 */
data class AggregateMissing(
    val name: String,
    val needed: Int,
    val inStock: Int,
    val usedByDishes: List<String>
) {
    val missing: Int get() = maxOf(0, needed - inStock)
}
