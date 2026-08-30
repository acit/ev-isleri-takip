package com.aile.takip.data.repository

import com.aile.takip.data.dao.*
import com.aile.takip.data.model.*
import kotlinx.coroutines.flow.Flow

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
) {
    // Tasks
    val tasks: Flow<List<Task>> = taskDao.getAll()
    suspend fun upsertTask(t: Task) = taskDao.upsert(t)
    suspend fun deleteTask(t: Task) = taskDao.delete(t)

    // Inventory
    val inventory: Flow<List<InventoryItem>> = inventoryDao.getAll()
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
    val messages: Flow<List<Message>> = messageDao.getAll()
    suspend fun upsertMessage(m: Message) = messageDao.upsert(m)
    suspend fun deleteMessage(m: Message) = messageDao.delete(m)

    // Shopping
    val shoppingItems: Flow<List<ShoppingItem>> = shoppingDao.getAll()
    suspend fun upsertShopping(s: ShoppingItem) = shoppingDao.upsert(s)
    suspend fun deleteShopping(s: ShoppingItem) = shoppingDao.delete(s)

    // Family Members
    val members: Flow<List<FamilyMember>> = memberDao.getAll()
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
}
