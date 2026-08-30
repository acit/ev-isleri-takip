package com.aile.takip.data.repository

import com.aile.takip.data.dao.*
import com.aile.takip.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class FamilyRepository(
    private val taskDao: TaskDao,
    private val inventoryDao: InventoryDao,
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao,
    private val invoiceDao: InvoiceDao,
    private val messageDao: MessageDao,
    private val shoppingDao: ShoppingDao,
    private val memberDao: MemberDao,
    private val mealPlanDao: MealPlanDao,
    private val sportsClubDao: SportsClubDao,
    private val workoutLogDao: WorkoutLogDao,
    private val calorieLogDao: CalorieLogDao,
    private val menstrualCycleDao: MenstrualCycleDao,
    private val authDao: AuthDao,
    private val syncEventDao: SyncEventDao,
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao,
    private val waterLogDao: WaterLogDao,
    private val sleepLogDao: SleepLogDao,
) {
    // Tasks
    val tasks: Flow<List<Task>> = taskDao.getAll().distinctUntilChanged()
    suspend fun upsertTask(t: Task) = taskDao.upsert(t)
    suspend fun deleteTask(t: Task) = taskDao.delete(t)

    // Inventory
    val inventory: Flow<List<InventoryItem>> = inventoryDao.getAll().distinctUntilChanged()
    suspend fun upsertInventory(i: InventoryItem) = inventoryDao.upsert(i)
    suspend fun deleteInventory(i: InventoryItem) = inventoryDao.delete(i)

    // Budgets
    val budgets: Flow<List<Budget>> = budgetDao.getAll()
    suspend fun upsertBudget(b: Budget) = budgetDao.upsert(b)
    suspend fun deleteBudget(b: Budget) = budgetDao.delete(b)

    // Expenses
    val expenses: Flow<List<Expense>> = expenseDao.getAll()
    suspend fun upsertExpense(e: Expense) = expenseDao.upsert(e)
    suspend fun deleteExpense(e: Expense) = expenseDao.delete(e)

    // Invoices
    val invoices: Flow<List<Invoice>> = invoiceDao.getAll()
    suspend fun upsertInvoice(i: Invoice) = invoiceDao.upsert(i)
    suspend fun deleteInvoice(i: Invoice) = invoiceDao.delete(i)

    // Messages
    val messages: Flow<List<Message>> = messageDao.getAll().distinctUntilChanged()
    suspend fun upsertMessage(m: Message) = messageDao.upsert(m)
    suspend fun deleteMessage(m: Message) = messageDao.delete(m)

    // Shopping
    val shoppingItems: Flow<List<ShoppingItem>> = shoppingDao.getAll().distinctUntilChanged()
    suspend fun upsertShopping(s: ShoppingItem) = shoppingDao.upsert(s)
    suspend fun deleteShopping(s: ShoppingItem) = shoppingDao.delete(s)

    // Family Members
    val members: Flow<List<FamilyMember>> = memberDao.getAll().distinctUntilChanged()
    suspend fun upsertMember(m: FamilyMember) = memberDao.upsert(m)
    suspend fun deleteMember(m: FamilyMember) = memberDao.delete(m)

    // Meal Plans
    val mealPlans: Flow<List<MealPlan>> = mealPlanDao.getAll()
    suspend fun upsertMealPlan(mp: MealPlan) = mealPlanDao.upsert(mp)
    suspend fun deleteMealPlan(mp: MealPlan) = mealPlanDao.delete(mp)

    // ========== YENİ ==========

    // Sports Clubs
    val sportsClubs: Flow<List<SportsClub>> = sportsClubDao.getAll()
    fun getClubsByMember(memberId: String) = sportsClubDao.getByMember(memberId)
    suspend fun upsertClub(c: SportsClub) = sportsClubDao.upsert(c)
    suspend fun deleteClub(c: SportsClub) = sportsClubDao.delete(c)

    // Workout Logs
    val workoutLogs: Flow<List<WorkoutLog>> = workoutLogDao.getAll()
    fun getWorkoutsByMember(memberId: String) = workoutLogDao.getByMember(memberId)
    fun getWorkoutsByDate(date: String) = workoutLogDao.getByDate(date)
    suspend fun upsertWorkout(w: WorkoutLog) = workoutLogDao.upsert(w)
    suspend fun deleteWorkout(w: WorkoutLog) = workoutLogDao.delete(w)

    // Calorie Logs
    val calorieLogs: Flow<List<CalorieLog>> = calorieLogDao.getAll()
    fun getCaloriesByMemberAndDate(memberId: String, date: String) = calorieLogDao.getByMemberAndDate(memberId, date)
    fun totalCalories(memberId: String, date: String) = calorieLogDao.totalCalories(memberId, date)
    suspend fun upsertCalorie(c: CalorieLog) = calorieLogDao.upsert(c)
    suspend fun deleteCalorie(c: CalorieLog) = calorieLogDao.delete(c)

    // Menstrual Cycles
    val menstrualCycles: Flow<List<MenstrualCycle>> = menstrualCycleDao.getAll()
    fun getCyclesByMember(memberId: String) = menstrualCycleDao.getByMember(memberId)
    suspend fun upsertCycle(c: MenstrualCycle) = menstrualCycleDao.upsert(c)
    suspend fun deleteCycle(c: MenstrualCycle) = menstrualCycleDao.delete(c)

    // Auth
    val auth: Flow<UserAuth?> = authDao.get()
    suspend fun getAuthOnce() = authDao.getOnce()
    suspend fun upsertAuth(u: UserAuth) = authDao.upsert(u)

    // Sync Events
    val unsyncedEvents: Flow<List<SyncEvent>> = syncEventDao.getUnsynced()
    suspend fun upsertSyncEvent(s: SyncEvent) = syncEventDao.upsert(s)
    suspend fun markSynced(id: String) = syncEventDao.markSynced(id)

    // ========== AİLE NOTLARI ==========
    val notes: Flow<List<Note>> = noteDao.getAll().distinctUntilChanged()
    fun searchNotes(query: String) = noteDao.search(query)
    fun getArchivedNotes() = noteDao.getArchived()
    fun getNotesByCategory(category: String) = noteDao.getByCategory(category)
    suspend fun upsertNote(n: Note) = noteDao.upsert(n)
    suspend fun deleteNote(n: Note) = noteDao.delete(n)
    suspend fun getAllNotesOnce() = noteDao.getAllOnce()

    // ========== HATIRLATICILAR ==========
    val activeReminders: Flow<List<Reminder>> = reminderDao.getActive().distinctUntilChanged()
    val allReminders: Flow<List<Reminder>> = reminderDao.getAll().distinctUntilChanged()
    val completedReminders: Flow<List<Reminder>> = reminderDao.getCompleted()
    fun getActiveByCategory(category: String) = reminderDao.getActiveByCategory(category)
    suspend fun getDueReminders(now: Long) = reminderDao.getDueReminders(now)
    suspend fun getSnoozedReminders(now: Long) = reminderDao.getSnoozedReminders(now)
    suspend fun upsertReminder(r: Reminder) = reminderDao.upsert(r)
    suspend fun deleteReminder(r: Reminder) = reminderDao.delete(r)
    suspend fun getAllRemindersOnce() = reminderDao.getAllOnce()

    // ========== SU TÜKETİMİ ==========
    val waterLogs: Flow<List<WaterLog>> = waterLogDao.getAll()
    fun getWaterByMemberAndDate(memberId: String, date: String) = waterLogDao.getByMemberAndDate(memberId, date)
    fun totalWater(memberId: String, date: String) = waterLogDao.totalAmount(memberId, date)
    suspend fun upsertWater(w: WaterLog) = waterLogDao.upsert(w)
    suspend fun deleteWater(w: WaterLog) = waterLogDao.delete(w)
    suspend fun getAllWaterOnce() = waterLogDao.getAllOnce()

    // ========== UYKU TAKİBİ ==========
    val sleepLogs: Flow<List<SleepLog>> = sleepLogDao.getAll()
    fun getSleepByMember(memberId: String) = sleepLogDao.getByMember(memberId)
    fun getSleepByDate(date: String) = sleepLogDao.getByDate(date)
    suspend fun upsertSleep(s: SleepLog) = sleepLogDao.upsert(s)
    suspend fun deleteSleep(s: SleepLog) = sleepLogDao.delete(s)
    suspend fun getAllSleepOnce() = sleepLogDao.getAllOnce()
}
