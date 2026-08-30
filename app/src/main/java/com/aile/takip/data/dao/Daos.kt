package com.aile.takip.data.dao

import androidx.room.*
import com.aile.takip.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Task>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: Task)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(t: List<Task>)
    @Delete
    suspend fun delete(t: Task)
    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
    @Query("SELECT * FROM tasks")
    suspend fun getAllOnce(): List<Task>
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY createdAt DESC")
    fun getAll(): Flow<List<InventoryItem>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InventoryItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InventoryItem>)
    @Delete
    suspend fun delete(item: InventoryItem)
    @Query("DELETE FROM inventory")
    suspend fun deleteAll()
    @Query("SELECT * FROM inventory")
    suspend fun getAllOnce(): List<InventoryItem>
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Budget>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(b: Budget)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(b: List<Budget>)
    @Delete
    suspend fun delete(b: Budget)
    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
    @Query("SELECT * FROM budgets")
    suspend fun getAllOnce(): List<Budget>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Expense>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: Expense)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(e: List<Expense>)
    @Delete
    suspend fun delete(e: Expense)
    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
    @Query("SELECT * FROM expenses")
    suspend fun getAllOnce(): List<Expense>
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Invoice>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(i: Invoice)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(i: List<Invoice>)
    @Delete
    suspend fun delete(i: Invoice)
    @Query("DELETE FROM invoices")
    suspend fun deleteAll()
    @Query("SELECT * FROM invoices")
    suspend fun getAllOnce(): List<Invoice>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Message>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(m: Message)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(m: List<Message>)
    @Delete
    suspend fun delete(m: Message)
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
    @Query("SELECT * FROM messages")
    suspend fun getAllOnce(): List<Message>
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ShoppingItem>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: ShoppingItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(s: List<ShoppingItem>)
    @Delete
    suspend fun delete(s: ShoppingItem)
    @Query("DELETE FROM shopping")
    suspend fun deleteAll()
    @Query("SELECT * FROM shopping")
    suspend fun getAllOnce(): List<ShoppingItem>
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM family_members ORDER BY createdAt ASC")
    fun getAll(): Flow<List<FamilyMember>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(m: FamilyMember)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(m: List<FamilyMember>)
    @Delete
    suspend fun delete(m: FamilyMember)
    @Query("DELETE FROM family_members")
    suspend fun deleteAll()
    @Query("SELECT * FROM family_members")
    suspend fun getAllOnce(): List<FamilyMember>
}

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plans ORDER BY dayOfWeek ASC")
    fun getAll(): Flow<List<MealPlan>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mp: MealPlan)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(mp: List<MealPlan>)
    @Delete
    suspend fun delete(mp: MealPlan)
    @Query("DELETE FROM meal_plans")
    suspend fun deleteAll()
    @Query("SELECT * FROM meal_plans")
    suspend fun getAllOnce(): List<MealPlan>
}

// ========== YENİ DAO'LAR ==========

@Dao
interface SportsClubDao {
    @Query("SELECT * FROM sports_clubs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SportsClub>>
    @Query("SELECT * FROM sports_clubs WHERE memberId = :memberId")
    fun getByMember(memberId: String): Flow<List<SportsClub>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: SportsClub)
    @Delete
    suspend fun delete(c: SportsClub)
    @Query("DELETE FROM sports_clubs")
    suspend fun deleteAll()
    @Query("SELECT * FROM sports_clubs")
    suspend fun getAllOnce(): List<SportsClub>
}

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<WorkoutLog>>
    @Query("SELECT * FROM workout_logs WHERE memberId = :memberId ORDER BY createdAt DESC")
    fun getByMember(memberId: String): Flow<List<WorkoutLog>>
    @Query("SELECT * FROM workout_logs WHERE date = :date")
    fun getByDate(date: String): Flow<List<WorkoutLog>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(w: WorkoutLog)
    @Delete
    suspend fun delete(w: WorkoutLog)
    @Query("SELECT * FROM workout_logs")
    suspend fun getAllOnce(): List<WorkoutLog>
}

@Dao
interface CalorieLogDao {
    @Query("SELECT * FROM calorie_logs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CalorieLog>>
    @Query("SELECT * FROM calorie_logs WHERE memberId = :memberId AND date = :date")
    fun getByMemberAndDate(memberId: String, date: String): Flow<List<CalorieLog>>
    @Query("SELECT SUM(calories) FROM calorie_logs WHERE memberId = :memberId AND date = :date")
    fun totalCalories(memberId: String, date: String): Flow<Int?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: CalorieLog)
    @Delete
    suspend fun delete(c: CalorieLog)
    @Query("SELECT * FROM calorie_logs")
    suspend fun getAllOnce(): List<CalorieLog>
}

@Dao
interface MenstrualCycleDao {
    @Query("SELECT * FROM menstrual_cycles ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MenstrualCycle>>
    @Query("SELECT * FROM menstrual_cycles WHERE memberId = :memberId ORDER BY createdAt DESC")
    fun getByMember(memberId: String): Flow<List<MenstrualCycle>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: MenstrualCycle)
    @Delete
    suspend fun delete(c: MenstrualCycle)
    @Query("SELECT * FROM menstrual_cycles")
    suspend fun getAllOnce(): List<MenstrualCycle>
}

@Dao
interface AuthDao {
    @Query("SELECT * FROM user_auth WHERE id = 'main_user'")
    fun get(): Flow<UserAuth?>
    @Query("SELECT * FROM user_auth WHERE id = 'main_user'")
    suspend fun getOnce(): UserAuth?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(u: UserAuth)
}

@Dao
interface SyncEventDao {
    @Query("SELECT * FROM sync_events WHERE synced = 0 ORDER BY createdAt ASC")
    fun getUnsynced(): Flow<List<SyncEvent>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: SyncEvent)
    @Query("UPDATE sync_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
    @Query("DELETE FROM sync_events WHERE synced = 1")
    suspend fun deleteSynced()
}
