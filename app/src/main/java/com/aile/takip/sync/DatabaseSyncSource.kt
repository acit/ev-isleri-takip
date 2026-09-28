package com.aile.takip.sync

import com.aile.takip.data.db.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Bir tablo akışını senkron satırlarına çevirir. */
private fun <T> Flow<List<T>>.toRows(transform: (T) -> SyncRow): Flow<List<SyncRow>> =
    map { list -> list.map(transform) }

/**
 * Yerel Room tablolarını senkron motoruna kaynak olarak verir.
 *
 * Her tablo için: ilgili DAO akışı + `syncVersion` taşıyan gönderilebilir içerik
 * (`FirebaseSyncService`'teki mapper'lar tek doğruluk kaynağıdır).
 */
class DatabaseSyncSource(
    private val db: AppDatabase,
    private val mappers: FirebaseSyncService
) : SyncSource {

    override fun flows(): Map<String, Flow<List<SyncRow>>> = mapOf(
        "members" to db.memberDao().getAll().toRows { SyncRow(it.id, mappers.memberToMap(it)) },
        "tasks" to db.taskDao().getAll().toRows { SyncRow(it.id, mappers.taskToMap(it)) },
        "shopping" to db.shoppingDao().getAll().toRows { SyncRow(it.id, mappers.shoppingToMap(it)) },
        "messages" to db.messageDao().getAll().toRows { SyncRow(it.id, mappers.messageToMap(it)) },
        "invoices" to db.invoiceDao().getAll().toRows { SyncRow(it.id, mappers.invoiceToMap(it)) },
        "budgets" to db.budgetDao().getAll().toRows { SyncRow(it.id, mappers.budgetToMap(it)) },
        "expenses" to db.expenseDao().getAll().toRows { SyncRow(it.id, mappers.expenseToMap(it)) },
        "inventory" to db.inventoryDao().getAll().toRows { SyncRow(it.id, mappers.inventoryToMap(it)) },
        "meal_plans" to db.mealPlanDao().getAll().toRows { SyncRow(it.id, mappers.mealPlanToMap(it)) },
        "sports_clubs" to db.sportsClubDao().getAll().toRows { SyncRow(it.id, mappers.sportsClubToMap(it)) },
        "workout_logs" to db.workoutLogDao().getAll().toRows { SyncRow(it.id, mappers.workoutToMap(it)) },
        "calorie_logs" to db.calorieLogDao().getAll().toRows { SyncRow(it.id, mappers.calorieToMap(it)) },
        "menstrual_cycles" to db.menstrualCycleDao().getAll().toRows { SyncRow(it.id, mappers.menstrualCycleToMap(it)) },
        "notes" to db.noteDao().getAll().toRows { SyncRow(it.id, mappers.noteToMap(it)) },
        "reminders" to db.reminderDao().getAll().toRows { SyncRow(it.id, mappers.reminderToMap(it)) },
        "water_logs" to db.waterLogDao().getAll().toRows { SyncRow(it.id, mappers.waterLogToMap(it)) },
        "sleep_logs" to db.sleepLogDao().getAll().toRows { SyncRow(it.id, mappers.sleepLogToMap(it)) }
    )

    companion object {
        /** Senkronize edilen tüm tabloların tek doğruluk kaynağı. */
        val TABLE_NAMES = listOf(
            "members", "tasks", "shopping", "messages", "invoices", "budgets", "expenses",
            "inventory", "meal_plans", "sports_clubs", "workout_logs", "calorie_logs",
            "menstrual_cycles", "notes", "reminders", "water_logs", "sleep_logs"
        )
    }
}
