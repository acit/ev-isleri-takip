package com.aile.takip.data.model

import androidx.room.*

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: String = "Genel",
    val priority: String = "orta",
    val assignee: String = "",
    val status: String = "bekleyen",
    val dueDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Genel",
    val quantity: Int = 1,
    val unit: String = "adet",
    val minStock: Int = 0,
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val category: String,
    val monthlyLimit: Double,
    val spentAmount: Double = 0.0,
    val monthYear: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val budgetId: String = "",
    val category: String,
    val amount: Double,
    val description: String = "",
    val expenseDate: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String = "Genel",
    val dueDate: String = "",
    val notes: String = "",
    val imageBase64: String? = null,
    val status: String = "pending",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val senderId: String,
    val content: String,
    val channel: String = "genel",
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping")
data class ShoppingItem(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int = 1,
    val category: String = "Market",
    val checked: Boolean = false,
    val addedBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val role: String = "uye",
    val color: String = "#3498DB",
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "meal_plans")
data class MealPlan(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val dayOfWeek: Int,
    val mealType: String,
    val dish: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

// ========== YENİ MODELLER ==========

@Entity(tableName = "sports_clubs")
data class SportsClub(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: String = "Spor Salonu",  // Spor Salonu, Yüzme, Futbol, Yoga, Pilates vb.
    val address: String = "",
    val phone: String = "",
    val membershipStart: String = "",
    val membershipEnd: String = "",
    val monthlyFee: Double = 0.0,
    val isActive: Boolean = true,
    val memberId: String = "",  // Hangi aile üyesi
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val clubId: String = "",
    val memberId: String = "",
    val workoutType: String = "",  // Kardiyo, Ağırlık, Yoga, Yüzme vb.
    val duration: Int = 0,  // dakika
    val caloriesBurned: Int = 0,
    val date: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "calorie_logs")
data class CalorieLog(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val memberId: String = "",
    val mealType: String = "",  // Kahvaltı, Öğle, Akşam, Atıştırmalık
    val foodName: String = "",
    val calories: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val servingSize: String = "",  // 1 porsiyon, 200g vb.
    val date: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "menstrual_cycles")
data class MenstrualCycle(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val memberId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val cycleLength: Int = 28,  // gün
    val periodLength: Int = 5,  // gün
    val symptoms: String = "",  // kramp, baş ağrısı, şişkinlik vb. (JSON array)
    val mood: String = "",  // iyi, kötü, stresli, mutlu
    val flow: String = "",  // hafif, orta, ağır
    val notes: String = "",
    val isPersonal: Boolean = true,  // Kişisel mi aileyle paylaşılacak mı
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_auth")
data class UserAuth(
    @PrimaryKey val id: String = "main_user",
    val pin: String = "",
    val biometricEnabled: Boolean = false,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_events")
data class SyncEvent(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val tableName: String,
    val recordId: String,
    val action: String,  // insert, update, delete
    val data: String = "",  // JSON serialized data
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
