package com.aile.takip.sync

import com.aile.takip.data.db.AppDatabase
import com.aile.takip.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSyncService(private val db: AppDatabase) {

    private var database: FirebaseDatabase? = null
    private var auth: FirebaseAuth? = null
    private var familyRef: DatabaseReference? = null
    private val listenerCleanups = mutableListOf<() -> Unit>()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Disconnected)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime

    private val _syncedTables = MutableStateFlow(emptySet<String>())
    val syncedTables: StateFlow<Set<String>> = _syncedTables

    sealed class SyncState {
        data object Disconnected : SyncState()
        data object Connecting : SyncState()
        data object Connected : SyncState()
        data class Syncing(val table: String = "") : SyncState()
        data class Error(val message: String) : SyncState()
    }

    suspend fun connect(familyGroupId: String): Boolean {
        return try {
            _syncState.value = SyncState.Connecting
            try {
                // Firebase projesinden otomatik olarak URL'yi alir
                database = FirebaseDatabase.getInstance()
                auth = FirebaseAuth.getInstance()
            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Firebase baslatilamadi: ${e.message}")
                return false
            }
            if (auth?.currentUser == null) {
                auth?.signInAnonymously()?.await()
            }
            familyRef = database?.getReference("aile_grubu")?.child(familyGroupId)
            database?.getReference(".info/connected")?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    _syncState.value = if (connected) SyncState.Connected else SyncState.Disconnected
                }
                override fun onCancelled(error: DatabaseError) {
                    _syncState.value = SyncState.Error(error.message)
                }
            })
            startListening()
            _syncState.value = SyncState.Connected
            true
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Bağlantı hatası")
            false
        }
    }

    private fun startListening() {
        stopListening()
        val tableNames = listOf("members", "tasks", "shopping", "messages", "invoices", "budgets", "inventory", "meal_plans", "sports_clubs", "workout_logs", "calorie_logs", "menstrual_cycles", "notes", "reminders", "water_logs", "sleep_logs")
        for (tableName in tableNames) {
            val ref = familyRef?.child(tableName) ?: continue
            val listener = ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _syncState.value = SyncState.Syncing(tableName)
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            for (child in snapshot.children) {
                                val data = child.value as? Map<*, *> ?: continue
                                @Suppress("UNCHECKED_CAST")
                                val map = data as Map<String, Any>
                                when (tableName) {
                                    "members" -> parseMember(map)?.let { db.memberDao().upsert(it) }
                                    "tasks" -> parseTask(map)?.let { db.taskDao().upsert(it) }
                                    "shopping" -> parseShopping(map)?.let { db.shoppingDao().upsert(it) }
                                    "messages" -> parseMessage(map)?.let { db.messageDao().upsert(it) }
                                    "invoices" -> parseInvoice(map)?.let { db.invoiceDao().upsert(it) }
                                    "budgets" -> parseBudget(map)?.let { db.budgetDao().upsert(it) }
                                    "inventory" -> parseInventory(map)?.let { db.inventoryDao().upsert(it) }
                                    "meal_plans" -> parseMealPlan(map)?.let { db.mealPlanDao().upsert(it) }
                                    "sports_clubs" -> parseSportsClub(map)?.let { db.sportsClubDao().upsert(it) }
                                    "workout_logs" -> parseWorkout(map)?.let { db.workoutLogDao().upsert(it) }
                                    "calorie_logs" -> parseCalorie(map)?.let { db.calorieLogDao().upsert(it) }
                                    "menstrual_cycles" -> parseMenstrualCycle(map)?.let { db.menstrualCycleDao().upsert(it) }
                                    "notes" -> parseNote(map)?.let { db.noteDao().upsert(it) }
                                    "reminders" -> parseReminder(map)?.let { db.reminderDao().upsert(it) }
                                    "water_logs" -> parseWaterLog(map)?.let { db.waterLogDao().upsert(it) }
                                    "sleep_logs" -> parseSleepLog(map)?.let { db.sleepLogDao().upsert(it) }
                                }
                            }
                            _lastSyncTime.value = System.currentTimeMillis()
                            val current = _syncedTables.value.toMutableSet()
                            current.add(tableName)
                            _syncedTables.value = current
                            _syncState.value = SyncState.Connected
                        } catch (e: Exception) {
                            _syncState.value = SyncState.Error(e.message ?: "Senkron hatası")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    _syncState.value = SyncState.Error(error.message)
                }
            })
            listenerCleanups.add { ref.removeEventListener(listener) }
        }
    }

    suspend fun push(table: String, id: String, data: Map<String, Any>) {
        familyRef?.child(table)?.child(id)?.setValue(data)
    }

    suspend fun delete(table: String, id: String) {
        familyRef?.child(table)?.child(id)?.removeValue()
    }

    suspend fun pushAll() {
        _syncState.value = SyncState.Syncing("all")
        val allMembers = db.memberDao().getAllOnce()
        for (m in allMembers) push("members", m.id, memberToMap(m))
        val allTasks = db.taskDao().getAllOnce()
        for (t in allTasks) push("tasks", t.id, taskToMap(t))
        val allShopping = db.shoppingDao().getAllOnce()
        for (s in allShopping) push("shopping", s.id, shoppingToMap(s))
        val allMessages = db.messageDao().getAllOnce()
        for (m in allMessages) push("messages", m.id, messageToMap(m))
        val allInvoices = db.invoiceDao().getAllOnce()
        for (i in allInvoices) push("invoices", i.id, invoiceToMap(i))
        val allBudgets = db.budgetDao().getAllOnce()
        for (b in allBudgets) push("budgets", b.id, budgetToMap(b))
        val allInventory = db.inventoryDao().getAllOnce()
        for (i in allInventory) push("inventory", i.id, inventoryToMap(i))
        val allMeals = db.mealPlanDao().getAllOnce()
        for (mp in allMeals) push("meal_plans", mp.id, mealPlanToMap(mp))
        val allClubs = db.sportsClubDao().getAllOnce()
        for (c in allClubs) push("sports_clubs", c.id, sportsClubToMap(c))
        val allWorkouts = db.workoutLogDao().getAllOnce()
        for (w in allWorkouts) push("workout_logs", w.id, workoutToMap(w))
        val allCalories = db.calorieLogDao().getAllOnce()
        for (c in allCalories) push("calorie_logs", c.id, calorieToMap(c))
        val allCycles = db.menstrualCycleDao().getAllOnce()
        for (c in allCycles) push("menstrual_cycles", c.id, menstrualCycleToMap(c))
        val allNotes = db.noteDao().getAllOnce()
        for (n in allNotes) push("notes", n.id, noteToMap(n))
        val allReminders = db.reminderDao().getAllOnce()
        for (r in allReminders) push("reminders", r.id, reminderToMap(r))
        val allWater = db.waterLogDao().getAllOnce()
        for (w in allWater) push("water_logs", w.id, waterLogToMap(w))
        val allSleep = db.sleepLogDao().getAllOnce()
        for (s in allSleep) push("sleep_logs", s.id, sleepLogToMap(s))
        familyRef?.child("last_sync")?.setValue(System.currentTimeMillis())
        _lastSyncTime.value = System.currentTimeMillis()
        _syncState.value = SyncState.Connected
    }

    fun stopListening() {
        for (cleanup in listenerCleanups) cleanup()
        listenerCleanups.clear()
    }

    fun disconnect() {
        stopListening()
        auth?.signOut()
        _syncState.value = SyncState.Disconnected
    }

    // ========== PARSE (model from firebase map) ==========

    private fun parseMember(d: Map<String, Any>): FamilyMember? {
        val id = d["id"] as? String ?: return null
        return FamilyMember(id = id, name = d["name"] as? String ?: "", role = d["role"] as? String ?: "uye",
            color = d["color"] as? String ?: "#3498DB", points = (d["points"] as? Number)?.toInt() ?: 0,
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L, syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseTask(d: Map<String, Any>): Task? {
        val id = d["id"] as? String ?: return null
        return Task(id = id, title = d["title"] as? String ?: "", description = d["description"] as? String ?: "",
            category = d["category"] as? String ?: "Genel", priority = d["priority"] as? String ?: "orta",
            assignee = d["assignee"] as? String ?: "", status = d["status"] as? String ?: "bekleyen",
            dueDate = d["dueDate"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            completedAt = (d["completedAt"] as? Number)?.toLong()?.let { if (it == 0L) null else it },
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseShopping(d: Map<String, Any>): ShoppingItem? {
        val id = d["id"] as? String ?: return null
        return ShoppingItem(id = id, name = d["name"] as? String ?: "",
            quantity = (d["quantity"] as? Number)?.toInt() ?: 1, category = d["category"] as? String ?: "Market",
            checked = d["checked"] as? Boolean ?: false, addedBy = d["addedBy"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L, syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseMessage(d: Map<String, Any>): Message? {
        val id = d["id"] as? String ?: return null
        return Message(id = id, senderName = d["senderName"] as? String ?: "", senderId = d["senderId"] as? String ?: "",
            content = d["content"] as? String ?: "", channel = d["channel"] as? String ?: "genel",
            read = d["read"] as? Boolean ?: false, createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseInvoice(d: Map<String, Any>): Invoice? {
        val id = d["id"] as? String ?: return null
        return Invoice(id = id, title = d["title"] as? String ?: "",
            amount = (d["amount"] as? Number)?.toDouble() ?: 0.0, category = d["category"] as? String ?: "Genel",
            dueDate = d["dueDate"] as? String ?: "", notes = d["notes"] as? String ?: "",
            status = d["status"] as? String ?: "pending", createdBy = d["createdBy"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L, syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseBudget(d: Map<String, Any>): Budget? {
        val id = d["id"] as? String ?: return null
        return Budget(id = id, category = d["category"] as? String ?: "",
            monthlyLimit = (d["monthlyLimit"] as? Number)?.toDouble() ?: 0.0,
            spentAmount = (d["spentAmount"] as? Number)?.toDouble() ?: 0.0,
            monthYear = d["monthYear"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseInventory(d: Map<String, Any>): InventoryItem? {
        val id = d["id"] as? String ?: return null
        return InventoryItem(id = id, name = d["name"] as? String ?: "", category = d["category"] as? String ?: "Genel",
            quantity = (d["quantity"] as? Number)?.toInt() ?: 1, unit = d["unit"] as? String ?: "adet",
            minStock = (d["minStock"] as? Number)?.toInt() ?: 0, location = d["location"] as? String ?: "",
            notes = d["notes"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseMealPlan(d: Map<String, Any>): MealPlan? {
        val id = d["id"] as? String ?: return null
        return MealPlan(id = id, dayOfWeek = (d["dayOfWeek"] as? Number)?.toInt() ?: 0,
            mealType = d["mealType"] as? String ?: "", dish = d["dish"] as? String ?: "",
            notes = d["notes"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseSportsClub(d: Map<String, Any>): SportsClub? {
        val id = d["id"] as? String ?: return null
        return SportsClub(id = id, name = d["name"] as? String ?: "", type = d["type"] as? String ?: "Spor Salonu",
            address = d["address"] as? String ?: "", phone = d["phone"] as? String ?: "",
            membershipStart = d["membershipStart"] as? String ?: "", membershipEnd = d["membershipEnd"] as? String ?: "",
            monthlyFee = (d["monthlyFee"] as? Number)?.toDouble() ?: 0.0, isActive = d["isActive"] as? Boolean ?: true,
            memberId = d["memberId"] as? String ?: "", notes = d["notes"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L, syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseWorkout(d: Map<String, Any>): WorkoutLog? {
        val id = d["id"] as? String ?: return null
        return WorkoutLog(id = id, clubId = d["clubId"] as? String ?: "", memberId = d["memberId"] as? String ?: "",
            workoutType = d["workoutType"] as? String ?: "", duration = (d["duration"] as? Number)?.toInt() ?: 0,
            caloriesBurned = (d["caloriesBurned"] as? Number)?.toInt() ?: 0, date = d["date"] as? String ?: "",
            notes = d["notes"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseCalorie(d: Map<String, Any>): CalorieLog? {
        val id = d["id"] as? String ?: return null
        return CalorieLog(id = id, memberId = d["memberId"] as? String ?: "", mealType = d["mealType"] as? String ?: "",
            foodName = d["foodName"] as? String ?: "", calories = (d["calories"] as? Number)?.toInt() ?: 0,
            protein = (d["protein"] as? Number)?.toDouble() ?: 0.0, carbs = (d["carbs"] as? Number)?.toDouble() ?: 0.0,
            fat = (d["fat"] as? Number)?.toDouble() ?: 0.0, servingSize = d["servingSize"] as? String ?: "",
            date = d["date"] as? String ?: "", createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    private fun parseMenstrualCycle(d: Map<String, Any>): MenstrualCycle? {
        val id = d["id"] as? String ?: return null
        return MenstrualCycle(id = id, memberId = d["memberId"] as? String ?: "",
            startDate = d["startDate"] as? String ?: "", endDate = d["endDate"] as? String ?: "",
            cycleLength = (d["cycleLength"] as? Number)?.toInt() ?: 28, periodLength = (d["periodLength"] as? Number)?.toInt() ?: 5,
            symptoms = d["symptoms"] as? String ?: "", mood = d["mood"] as? String ?: "",
            flow = d["flow"] as? String ?: "", notes = d["notes"] as? String ?: "",
            isPersonal = d["isPersonal"] as? Boolean ?: true, createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L)
    }

    // ========== TO MAP ==========

    private fun memberToMap(m: FamilyMember) = mapOf("id" to m.id, "name" to m.name, "role" to m.role, "color" to m.color, "points" to m.points, "createdAt" to m.createdAt, "syncVersion" to m.syncVersion)
    private fun taskToMap(t: Task) = mapOf("id" to t.id, "title" to t.title, "description" to t.description, "category" to t.category, "priority" to t.priority, "assignee" to t.assignee, "status" to t.status, "dueDate" to t.dueDate, "createdAt" to t.createdAt, "completedAt" to (t.completedAt ?: 0L), "syncVersion" to t.syncVersion)
    private fun shoppingToMap(s: ShoppingItem) = mapOf("id" to s.id, "name" to s.name, "quantity" to s.quantity, "category" to s.category, "checked" to s.checked, "addedBy" to s.addedBy, "createdAt" to s.createdAt, "syncVersion" to s.syncVersion)
    private fun messageToMap(m: Message) = mapOf("id" to m.id, "senderName" to m.senderName, "senderId" to m.senderId, "content" to m.content, "channel" to m.channel, "read" to m.read, "createdAt" to m.createdAt, "syncVersion" to m.syncVersion)
    private fun invoiceToMap(i: Invoice) = mapOf("id" to i.id, "title" to i.title, "amount" to i.amount, "category" to i.category, "dueDate" to i.dueDate, "notes" to i.notes, "status" to i.status, "createdBy" to i.createdBy, "createdAt" to i.createdAt, "syncVersion" to i.syncVersion)
    private fun budgetToMap(b: Budget) = mapOf("id" to b.id, "category" to b.category, "monthlyLimit" to b.monthlyLimit, "spentAmount" to b.spentAmount, "monthYear" to b.monthYear, "createdAt" to b.createdAt, "syncVersion" to b.syncVersion)
    private fun inventoryToMap(i: InventoryItem) = mapOf("id" to i.id, "name" to i.name, "category" to i.category, "quantity" to i.quantity, "unit" to i.unit, "minStock" to i.minStock, "location" to i.location, "notes" to i.notes, "createdAt" to i.createdAt, "syncVersion" to i.syncVersion)
    private fun mealPlanToMap(mp: MealPlan) = mapOf("id" to mp.id, "dayOfWeek" to mp.dayOfWeek, "mealType" to mp.mealType, "dish" to mp.dish, "notes" to mp.notes, "createdAt" to mp.createdAt, "syncVersion" to mp.syncVersion)
    private fun sportsClubToMap(c: SportsClub) = mapOf("id" to c.id, "name" to c.name, "type" to c.type, "address" to c.address, "phone" to c.phone, "membershipStart" to c.membershipStart, "membershipEnd" to c.membershipEnd, "monthlyFee" to c.monthlyFee, "isActive" to c.isActive, "memberId" to c.memberId, "notes" to c.notes, "createdAt" to c.createdAt, "syncVersion" to c.syncVersion)
    private fun workoutToMap(w: WorkoutLog) = mapOf("id" to w.id, "clubId" to w.clubId, "memberId" to w.memberId, "workoutType" to w.workoutType, "duration" to w.duration, "caloriesBurned" to w.caloriesBurned, "date" to w.date, "notes" to w.notes, "createdAt" to w.createdAt, "syncVersion" to w.syncVersion)
    private fun calorieToMap(c: CalorieLog) = mapOf("id" to c.id, "memberId" to c.memberId, "mealType" to c.mealType, "foodName" to c.foodName, "calories" to c.calories, "protein" to c.protein, "carbs" to c.carbs, "fat" to c.fat, "servingSize" to c.servingSize, "date" to c.date, "createdAt" to c.createdAt, "syncVersion" to c.syncVersion)
    private fun menstrualCycleToMap(c: MenstrualCycle) = mapOf("id" to c.id, "memberId" to c.memberId, "startDate" to c.startDate, "endDate" to c.endDate, "cycleLength" to c.cycleLength, "periodLength" to c.periodLength, "symptoms" to c.symptoms, "mood" to c.mood, "flow" to c.flow, "notes" to c.notes, "isPersonal" to c.isPersonal, "createdAt" to c.createdAt, "syncVersion" to c.syncVersion)

    // ========== NOTES ==========

    private fun parseNote(d: Map<String, Any>): Note? {
        val id = d["id"] as? String ?: return null
        return Note(
            id = id,
            title = d["title"] as? String ?: "",
            content = d["content"] as? String ?: "",
            category = d["category"] as? String ?: "Genel",
            color = d["color"] as? String ?: "#3498DB",
            isPinned = d["isPinned"] as? Boolean ?: false,
            isArchived = d["isArchived"] as? Boolean ?: false,
            createdBy = d["createdBy"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun noteToMap(n: Note) = mapOf(
        "id" to n.id,
        "title" to n.title,
        "content" to n.content,
        "category" to n.category,
        "color" to n.color,
        "isPinned" to n.isPinned,
        "isArchived" to n.isArchived,
        "createdBy" to n.createdBy,
        "createdAt" to n.createdAt,
        "syncVersion" to n.syncVersion
    )

    // ========== REMINDERS ==========

    private fun parseReminder(d: Map<String, Any>): Reminder? {
        val id = d["id"] as? String ?: return null
        return Reminder(
            id = id,
            title = d["title"] as? String ?: "",
            description = d["description"] as? String ?: "",
            reminderTime = (d["reminderTime"] as? Number)?.toLong() ?: 0L,
            repeatType = d["repeatType"] as? String ?: "once",
            category = d["category"] as? String ?: "Genel",
            priority = d["priority"] as? String ?: "orta",
            isCompleted = d["isCompleted"] as? Boolean ?: false,
            isSnoozed = d["isSnoozed"] as? Boolean ?: false,
            snoozeUntil = (d["snoozeUntil"] as? Number)?.toLong() ?: 0L,
            linkedId = d["linkedId"] as? String ?: "",
            linkedType = d["linkedType"] as? String ?: "",
            createdBy = d["createdBy"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun reminderToMap(r: Reminder) = mapOf(
        "id" to r.id,
        "title" to r.title,
        "description" to r.description,
        "reminderTime" to r.reminderTime,
        "repeatType" to r.repeatType,
        "category" to r.category,
        "priority" to r.priority,
        "isCompleted" to r.isCompleted,
        "isSnoozed" to r.isSnoozed,
        "snoozeUntil" to r.snoozeUntil,
        "linkedId" to r.linkedId,
        "linkedType" to r.linkedType,
        "createdBy" to r.createdBy,
        "createdAt" to r.createdAt,
        "syncVersion" to r.syncVersion
    )

    // ========== WATER LOGS ==========

    private fun parseWaterLog(d: Map<String, Any>): WaterLog? {
        val id = d["id"] as? String ?: return null
        return WaterLog(
            id = id,
            memberId = d["memberId"] as? String ?: "",
            amountMl = (d["amountMl"] as? Number)?.toInt() ?: 250,
            drinkType = d["drinkType"] as? String ?: "Su",
            date = d["date"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun waterLogToMap(w: WaterLog) = mapOf(
        "id" to w.id,
        "memberId" to w.memberId,
        "amountMl" to w.amountMl,
        "drinkType" to w.drinkType,
        "date" to w.date,
        "createdAt" to w.createdAt,
        "syncVersion" to w.syncVersion
    )

    // ========== SLEEP LOGS ==========

    private fun parseSleepLog(d: Map<String, Any>): SleepLog? {
        val id = d["id"] as? String ?: return null
        return SleepLog(
            id = id,
            memberId = d["memberId"] as? String ?: "",
            bedtime = (d["bedtime"] as? Number)?.toLong() ?: 0L,
            wakeTime = (d["wakeTime"] as? Number)?.toLong() ?: 0L,
            durationMinutes = (d["durationMinutes"] as? Number)?.toInt() ?: 0,
            quality = d["quality"] as? String ?: "orta",
            interruptions = (d["interruptions"] as? Number)?.toInt() ?: 0,
            notes = d["notes"] as? String ?: "",
            date = d["date"] as? String ?: "",
            createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L,
            syncVersion = (d["syncVersion"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun sleepLogToMap(s: SleepLog) = mapOf(
        "id" to s.id,
        "memberId" to s.memberId,
        "bedtime" to s.bedtime,
        "wakeTime" to s.wakeTime,
        "durationMinutes" to s.durationMinutes,
        "quality" to s.quality,
        "interruptions" to s.interruptions,
        "notes" to s.notes,
        "date" to s.date,
        "createdAt" to s.createdAt,
        "syncVersion" to s.syncVersion
    )
}
