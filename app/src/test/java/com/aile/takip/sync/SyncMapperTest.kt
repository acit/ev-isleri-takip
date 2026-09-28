package com.aile.takip.sync

import com.aile.takip.data.db.AppDatabase
import com.aile.takip.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * `toMap` → `parse` turunun **kayıpsız** olduğunu doğrular.
 *
 * Senkronizasyonda bir kayıt gönderilip geri alındığında hiçbir alan
 * kaybolmamalı. Eksik alan (ör. ürün fotoğrafı veya barkod) her birleştirmede
 * sessizce silinirdi; bu test o tür hataları yakalar.
 */
class SyncMapperTest {

    private val service = FirebaseSyncService(mock<AppDatabase>())

    private fun <T> assertRoundTrip(
        entity: String,
        original: T,
        toMap: (T) -> Map<String, Any>,
        parse: (Map<String, Any>) -> T?
    ) {
        assertEquals("$entity kaydı round-trip'te değişti", original, parse(toMap(original)))
    }

    @Test
    fun `task survives a sync round trip`() {
        assertRoundTrip(
            "Task",
            Task(
                id = "t1", title = "Faturaları öde", description = "Elektrik + su",
                category = "Ev", priority = "yüksek", assignee = "Mehmet", status = "devam",
                dueDate = "2026-10-01", createdAt = 111L, completedAt = 222L, syncVersion = 333L
            ),
            service::taskToMap, service::parseTask
        )
    }

    @Test
    fun `task with no completion time survives a sync round trip`() {
        assertRoundTrip(
            "Task",
            Task(id = "t2", title = "Tamamlanmamış", completedAt = null, syncVersion = 5L),
            service::taskToMap, service::parseTask
        )
    }

    @Test
    fun `shopping item keeps barcode price and photo`() {
        assertRoundTrip(
            "ShoppingItem",
            ShoppingItem(
                id = "s1", name = "Süt", quantity = 3, category = "Market", checked = true,
                addedBy = "Ayşe", barcode = "8690000000012", brand = "Sütaş",
                description = "1 L tam yağlı", imageBase64 = "AAABBBCCC", unitPrice = 32.5,
                totalPrice = 97.5, store = "Migros", notes = "İndirimde al",
                lastPurchaseDate = "2026-09-20", createdAt = 10L, syncVersion = 20L
            ),
            service::shoppingToMap, service::parseShopping
        )
    }

    @Test
    fun `inventory item keeps photo price history and averages`() {
        assertRoundTrip(
            "InventoryItem",
            InventoryItem(
                id = "i1", name = "Zeytinyağı", category = "Mutfak", quantity = 2, unit = "şişe",
                minStock = 1, location = "Kiler", notes = "Soğuk sıkım",
                imageBase64 = "ZZZZ", lastPrice = 210.0, lastStore = "Carrefour",
                lastPurchaseDate = "2026-09-01", averagePrice = 195.5,
                priceHistory = "[{\"price\":210.0}]", createdAt = 30L, syncVersion = 40L
            ),
            service::inventoryToMap, service::parseInventory
        )
    }

    @Test
    fun `invoice keeps its attached photo`() {
        assertRoundTrip(
            "Invoice",
            Invoice(
                id = "f1", title = "Elektrik", amount = 480.75, category = "Fatura",
                dueDate = "2026-10-05", notes = "Otomatik ödeme", imageBase64 = "QkJC",
                status = "pending", createdBy = "Mehmet", createdAt = 50L, syncVersion = 60L
            ),
            service::invoiceToMap, service::parseInvoice
        )
    }

    @Test
    fun `invoice without a photo stays without a photo`() {
        assertRoundTrip(
            "Invoice",
            Invoice(id = "f2", title = "Su", amount = 90.0, imageBase64 = null, syncVersion = 7L),
            service::invoiceToMap, service::parseInvoice
        )
    }

    @Test
    fun `message keeps its attachments`() {
        assertRoundTrip(
            "Message",
            Message(
                id = "m1", senderName = "Zeynep", senderId = "u2", content = "Fotoğrafa bak",
                channel = "genel", read = true, attachments = "[{\"fileName\":\"a.jpg\"}]",
                createdAt = 70L, syncVersion = 80L
            ),
            service::messageToMap, service::parseMessage
        )
    }

    @Test
    fun `note keeps its attachments and flags`() {
        assertRoundTrip(
            "Note",
            Note(
                id = "n1", title = "Alışveriş fikirleri", content = "Kamp malzemeleri",
                category = "Fikir", color = "#FF9800", isPinned = true, isArchived = false,
                attachments = "[{\"id\":\"x\"}]", createdBy = "Ayşe", createdAt = 90L, syncVersion = 100L
            ),
            service::noteToMap, service::parseNote
        )
    }

    @Test
    fun `reminder keeps repeat rules and alarm settings`() {
        assertRoundTrip(
            "Reminder",
            Reminder(
                id = "r1", title = "İlaç", description = "Sabah tok karnına",
                reminderTime = 1000L, repeatType = "custom", repeatDays = "1,3,5",
                repeatInterval = 2, repeatEndDate = 9999L, category = "Sağlık",
                priority = "yüksek", alarmSound = "urgent", vibrate = false, snoozeMinutes = 30,
                isCompleted = false, isSnoozed = true, snoozeUntil = 1500L,
                lastFiredAt = 1200L, nextFireAt = 1400L, linkedId = "t1", linkedType = "task",
                createdBy = "Anne", createdAt = 110L, syncVersion = 120L
            ),
            service::reminderToMap, service::parseReminder
        )
    }

    @Test
    fun `member budget and expense survive a sync round trip`() {
        assertRoundTrip(
            "FamilyMember",
            FamilyMember(
                id = "mem1", name = "Emir", role = "oğul", color = "#FF9800",
                points = 120, createdAt = 130L, syncVersion = 140L
            ),
            service::memberToMap, service::parseMember
        )
        assertRoundTrip(
            "Budget",
            Budget(
                id = "b1", category = "Market", monthlyLimit = 8000.0, spentAmount = 3120.4,
                monthYear = "2026-09", createdAt = 150L, syncVersion = 160L
            ),
            service::budgetToMap, service::parseBudget
        )
        assertRoundTrip(
            "Expense",
            Expense(
                id = "e1", budgetId = "b1", category = "Market", amount = 250.75,
                description = "Haftalık alışveriş", expenseDate = "2026-09-22",
                createdAt = 170L, syncVersion = 180L
            ),
            service::expenseToMap, service::parseExpense
        )
    }

    @Test
    fun `health sport and plan records survive a sync round trip`() {
        assertRoundTrip(
            "MealPlan",
            MealPlan(
                id = "mp1", dayOfWeek = 3, mealType = "Akşam", dish = "Karnıyarık",
                notes = "Fırında", createdAt = 190L, syncVersion = 200L
            ),
            service::mealPlanToMap, service::parseMealPlan
        )
        assertRoundTrip(
            "SportsClub",
            SportsClub(
                id = "c1", name = "Yüzme Kursu", type = "Yüzme", address = "Kadıköy",
                phone = "0216", membershipStart = "2026-01-01", membershipEnd = "2026-12-31",
                monthlyFee = 1500.0, isActive = false, memberId = "mem1", notes = "Havuz",
                createdAt = 210L, syncVersion = 220L
            ),
            service::sportsClubToMap, service::parseSportsClub
        )
        assertRoundTrip(
            "WorkoutLog",
            WorkoutLog(
                id = "w1", clubId = "c1", memberId = "mem1", workoutType = "Kardiyo",
                duration = 45, caloriesBurned = 380, date = "2026-09-23", notes = "İyi geçti",
                createdAt = 230L, syncVersion = 240L
            ),
            service::workoutToMap, service::parseWorkout
        )
        assertRoundTrip(
            "CalorieLog",
            CalorieLog(
                id = "cal1", memberId = "mem1", mealType = "Öğle", foodName = "Mercimek çorbası",
                calories = 210, protein = 12.5, carbs = 30.0, fat = 4.5, servingSize = "1 kase",
                date = "2026-09-24", createdAt = 250L, syncVersion = 260L
            ),
            service::calorieToMap, service::parseCalorie
        )
        assertRoundTrip(
            "MenstrualCycle",
            MenstrualCycle(
                id = "cy1", memberId = "mem2", startDate = "2026-09-01", endDate = "2026-09-05",
                cycleLength = 30, periodLength = 6, symptoms = "kramp", mood = "iyi",
                flow = "orta", notes = "—", isPersonal = false, createdAt = 270L, syncVersion = 280L
            ),
            service::menstrualCycleToMap, service::parseMenstrualCycle
        )
        assertRoundTrip(
            "WaterLog",
            WaterLog(
                id = "wl1", memberId = "mem1", amountMl = 500, drinkType = "Su",
                date = "2026-09-25", createdAt = 290L, syncVersion = 300L
            ),
            service::waterLogToMap, service::parseWaterLog
        )
        assertRoundTrip(
            "SleepLog",
            SleepLog(
                id = "sl1", memberId = "mem1", bedtime = 1000L, wakeTime = 2000L,
                durationMinutes = 480, quality = "iyi", interruptions = 2, notes = "Sessiz",
                date = "2026-09-25", createdAt = 310L, syncVersion = 320L
            ),
            service::sleepLogToMap, service::parseSleepLog
        )
    }
}
