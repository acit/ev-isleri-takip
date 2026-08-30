package com.aile.takip.data.db

import android.content.Context
import androidx.room.*
import com.aile.takip.data.dao.*
import com.aile.takip.data.model.*

@Database(
    entities = [
        Task::class, InventoryItem::class, Budget::class, Expense::class,
        Invoice::class, Message::class, ShoppingItem::class,
        FamilyMember::class, MealPlan::class,
        SportsClub::class, WorkoutLog::class, CalorieLog::class,
        MenstrualCycle::class, UserAuth::class, SyncEvent::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun messageDao(): MessageDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun memberDao(): MemberDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun sportsClubDao(): SportsClubDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun calorieLogDao(): CalorieLogDao
    abstract fun menstrualCycleDao(): MenstrualCycleDao
    abstract fun authDao(): AuthDao
    abstract fun syncEventDao(): SyncEventDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aile_takip.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
