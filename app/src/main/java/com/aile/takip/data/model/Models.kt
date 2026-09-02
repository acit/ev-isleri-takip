package com.aile.takip.data.model

import androidx.room.*

// ========== EK (ATTACHMENT) ==========
data class Attachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String = "",
    val mimeType: String = "image/jpeg",  // image/*, application/pdf, vb.
    val base64Data: String = "",  // Fotoğraf için Base64 encoded data
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

// Fiyat gecmisi kaydi
data class PriceRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val price: Double,
    val store: String,
    val date: String,  // yyyy-MM-dd format
    val quantity: Int = 1,
    val notes: String = ""
)

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
    val imageBase64: String = "",  // Urun resmi (Base64)
    val lastPrice: Double = 0.0,  // Son alisveris fiyati
    val lastStore: String = "",  // Son alisveris magazasi
    val lastPurchaseDate: String = "",  // Son alisveris tarihi
    val averagePrice: Double = 0.0,  // Ortalama fiyat
    val priceHistory: String = "",  // JSON array of PriceRecord objects
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
    val attachments: String = "",  // JSON array of Attachment objects
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
    // Yeni urun bilgileri
    val barcode: String = "",  // Barkod numarasi
    val brand: String = "",  // Marka
    val description: String = "",  // Aciklama
    val imageBase64: String = "",  // Urun resmi
    val unitPrice: Double = 0.0,  // Birim fiyat
    val totalPrice: Double = 0.0,  // Toplam fiyat (miktar x birim)
    val store: String = "",  // Alisveris yapilan magaza
    val notes: String = "",  // Notlar
    val lastPurchaseDate: String = "",  // Son alisveris tarihi
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

// ========== AİLE NOTLARI ==========

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String = "",
    val category: String = "Genel",  // Genel, Alışveriş, Tarif, Fikir, Önemli
    val color: String = "#3498DB",  // Renk kodu
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val attachments: String = "",  // JSON array of Attachment objects
    val createdBy: String = "",  // Hangi aile üyesi
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

// ========== HATIRLATICILAR ==========

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val reminderTime: Long,  // Hatırlatma zamanı (timestamp)
    val repeatType: String = "once",  // once, daily, weekly, monthly, custom
    val repeatDays: String = "",  // Custom için: "1,3,5" (Pazartesi, Çarşamba, Cuma)
    val repeatInterval: Int = 1,  // interval (her 2 günde bir vb.)
    val repeatEndDate: Long = 0L,  // Bitiş tarihi (0 = süresiz)
    val category: String = "Genel",  // Genel, Görev, Fatura, Etkinlik, Sağlık
    val priority: String = "orta",  // düşük, orta, yüksek
    val alarmSound: String = "default",  // default, alarm, bell, chime, urgent
    val vibrate: Boolean = true,  // Titreşim
    val snoozeMinutes: Int = 15,  // Erteleme süresi (dakika)
    val isCompleted: Boolean = false,
    val isSnoozed: Boolean = false,
    val snoozeUntil: Long = 0L,  // Erteleme bitiş zamanı
    val lastFiredAt: Long = 0L,  // Son ateşlenme zamanı
    val nextFireAt: Long = 0L,  // Sonraki ateşlenme zamanı
    val linkedId: String = "",  // İlişkili görev/fatura ID'si
    val linkedType: String = "",  // task, invoice, note vb.
    val createdBy: String = "",  // Hangi aile üyesi
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

// ========== SU TÜKETİMİ ==========

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val memberId: String = "",
    val amountMl: Int = 250,  // mililitre cinsinden
    val drinkType: String = "Su",  // Su, Çay, Kahve, Meyve Suyu vb.
    val date: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)

// ========== UYKU TAKİBİ ==========

@Entity(tableName = "sleep_logs")
data class SleepLog(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val memberId: String = "",
    val bedtime: Long = 0L,  // Yatma zamanı (timestamp)
    val wakeTime: Long = 0L,  // Kalkma zamanı (timestamp)
    val durationMinutes: Int = 0,  // Toplam uyku süresi (dakika)
    val quality: String = "orta",  // kötü, orta, iyi, çok iyi
    val interruptions: Int = 0,  // Uyanma sayısı
    val notes: String = "",
    val date: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncVersion: Long = System.currentTimeMillis()
)
