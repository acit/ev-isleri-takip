import Foundation
import SwiftData

// MARK: - Attachment (NEW)
@Model
final class Attachment {
    var id: String
    var fileName: String
    var mimeType: String
    var base64Data: String
    var fileSize: Int64
    var createdAt: Date
    
    init(id: String = UUID().uuidString, fileName: String = "", mimeType: String = "image/jpeg", base64Data: String = "", fileSize: Int64 = 0) {
        self.id = id
        self.fileName = fileName
        self.mimeType = mimeType
        self.base64Data = base64Data
        self.fileSize = fileSize
        self.createdAt = Date()
    }
}

// MARK: - Family Member
@Model
final class FamilyMember {
    var id: String
    var name: String
    var role: String
    var color: String
    var points: Int
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, name: String, role: String = "Üye", color: String = "#3498DB", points: Int = 0) {
        self.id = id
        self.name = name
        self.role = role
        self.color = color
        self.points = points
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Task
@Model
final class Task {
    var id: String
    var title: String
    var description: String
    var category: String
    var priority: String
    var assignee: String
    var status: String
    var dueDate: String
    var createdAt: Date
    var completedAt: Date?
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, title: String, description: String = "", category: String = "Genel", priority: String = "orta", assignee: String = "", status: String = "bekleyen", dueDate: String = "") {
        self.id = id
        self.title = title
        self.description = description
        self.category = category
        self.priority = priority
        self.assignee = assignee
        self.status = status
        self.dueDate = dueDate
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Shopping Item
@Model
final class ShoppingItem {
    var id: String
    var name: String
    var quantity: Int
    var category: String
    var checked: Bool
    var addedBy: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, name: String, quantity: Int = 1, category: String = "Market", checked: Bool = false, addedBy: String = "") {
        self.id = id
        self.name = name
        self.quantity = quantity
        self.category = category
        self.checked = checked
        self.addedBy = addedBy
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Message
@Model
final class Message {
    var id: String
    var senderName: String
    var senderId: String
    var content: String
    var channel: String
    var read: Bool
    var attachments: String  // JSON array of Attachment objects
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, senderName: String, senderId: String, content: String, channel: String = "genel", attachments: String = "") {
        self.id = id
        self.senderName = senderName
        self.senderId = senderId
        self.content = content
        self.channel = channel
        self.read = false
        self.attachments = attachments
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Note
@Model
final class Note {
    var id: String
    var title: String
    var content: String
    var category: String
    var color: String
    var isPinned: Bool
    var isArchived: Bool
    var attachments: String  // JSON array of Attachment objects
    var createdBy: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, title: String, content: String = "", category: String = "Genel", color: String = "#3498DB", isPinned: Bool = false, attachments: String = "", createdBy: String = "") {
        self.id = id
        self.title = title
        self.content = content
        self.category = category
        self.color = color
        self.isPinned = isPinned
        self.isArchived = false
        self.attachments = attachments
        self.createdBy = createdBy
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Reminder
@Model
final class Reminder {
    var id: String
    var title: String
    var description: String
    var reminderTime: Date
    var repeatType: String      // once, daily, weekly, monthly, custom
    var repeatDays: String      // Custom: "1,3,5"
    var repeatInterval: Int     // Her X günde bir
    var repeatEndDate: Date?    // Bitiş tarihi (nil = süresiz)
    var category: String
    var priority: String
    var alarmSound: String      // default, alarm, bell, chime, urgent
    var vibrate: Bool
    var snoozeMinutes: Int      // Erteleme süresi (dk)
    var isCompleted: Bool
    var isSnoozed: Bool
    var snoozeUntil: Date?
    var lastFiredAt: Date?
    var nextFireAt: Date?
    var linkedId: String        // İlişkili görev/fatura ID
    var linkedType: String      // task, invoice, note vb.
    var createdBy: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, title: String, description: String = "", reminderTime: Date, repeatType: String = "once", category: String = "Genel", priority: String = "orta", alarmSound: String = "default", createdBy: String = "") {
        self.id = id
        self.title = title
        self.description = description
        self.reminderTime = reminderTime
        self.repeatType = repeatType
        self.repeatDays = ""
        self.repeatInterval = 1
        self.repeatEndDate = nil
        self.category = category
        self.priority = priority
        self.alarmSound = alarmSound
        self.vibrate = true
        self.snoozeMinutes = 15
        self.isCompleted = false
        self.isSnoozed = false
        self.snoozeUntil = nil
        self.lastFiredAt = nil
        self.nextFireAt = nil
        self.linkedId = ""
        self.linkedType = ""
        self.createdBy = createdBy
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Invoice
@Model
final class Invoice {
    var id: String
    var title: String
    var amount: Double
    var category: String
    var dueDate: String
    var notes: String
    var imageBase64: String?
    var status: String
    var createdBy: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, title: String, amount: Double, category: String = "Genel", dueDate: String = "", notes: String = "", imageBase64: String? = nil, status: String = "pending", createdBy: String = "") {
        self.id = id
        self.title = title
        self.amount = amount
        self.category = category
        self.dueDate = dueDate
        self.notes = notes
        self.imageBase64 = imageBase64
        self.status = status
        self.createdBy = createdBy
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Budget
@Model
final class Budget {
    var id: String
    var category: String
    var monthlyLimit: Double
    var spentAmount: Double
    var monthYear: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, category: String, monthlyLimit: Double, spentAmount: Double = 0, monthYear: String) {
        self.id = id
        self.category = category
        self.monthlyLimit = monthlyLimit
        self.spentAmount = spentAmount
        self.monthYear = monthYear
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Expense
@Model
final class Expense {
    var id: String
    var budgetId: String
    var category: String
    var amount: Double
    var description: String
    var expenseDate: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, budgetId: String = "", category: String, amount: Double, description: String = "", expenseDate: String) {
        self.id = id
        self.budgetId = budgetId
        self.category = category
        self.amount = amount
        self.description = description
        self.expenseDate = expenseDate
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Inventory Item (NEW)
@Model
final class InventoryItem {
    var id: String
    var name: String
    var category: String
    var quantity: Int
    var unit: String
    var minStock: Int
    var location: String
    var notes: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, name: String, category: String = "Genel", quantity: Int = 1, unit: String = "adet", minStock: Int = 0, location: String = "", notes: String = "") {
        self.id = id
        self.name = name
        self.category = category
        self.quantity = quantity
        self.unit = unit
        self.minStock = minStock
        self.location = location
        self.notes = notes
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Meal Plan (NEW)
@Model
final class MealPlan {
    var id: String
    var dayOfWeek: Int
    var mealType: String
    var dish: String
    var notes: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, dayOfWeek: Int, mealType: String, dish: String, notes: String = "") {
        self.id = id
        self.dayOfWeek = dayOfWeek
        self.mealType = mealType
        self.dish = dish
        self.notes = notes
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Sports Club (NEW)
@Model
final class SportsClub {
    var id: String
    var name: String
    var type: String
    var address: String
    var phone: String
    var membershipStart: String
    var membershipEnd: String
    var monthlyFee: Double
    var isActive: Bool
    var memberId: String
    var notes: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, name: String, type: String = "Spor Salonu", address: String = "", phone: String = "", membershipStart: String = "", membershipEnd: String = "", monthlyFee: Double = 0, isActive: Bool = true, memberId: String = "", notes: String = "") {
        self.id = id
        self.name = name
        self.type = type
        self.address = address
        self.phone = phone
        self.membershipStart = membershipStart
        self.membershipEnd = membershipEnd
        self.monthlyFee = monthlyFee
        self.isActive = isActive
        self.memberId = memberId
        self.notes = notes
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Workout Log
@Model
final class WorkoutLog {
    var id: String
    var clubId: String
    var memberId: String
    var workoutType: String
    var duration: Int
    var caloriesBurned: Int
    var date: String
    var notes: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, clubId: String = "", memberId: String, workoutType: String, duration: Int, caloriesBurned: Int, date: String, notes: String = "") {
        self.id = id
        self.clubId = clubId
        self.memberId = memberId
        self.workoutType = workoutType
        self.duration = duration
        self.caloriesBurned = caloriesBurned
        self.date = date
        self.notes = notes
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Calorie Log
@Model
final class CalorieLog {
    var id: String
    var memberId: String
    var mealType: String
    var foodName: String
    var calories: Int
    var protein: Double
    var carbs: Double
    var fat: Double
    var servingSize: String
    var date: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, memberId: String, mealType: String, foodName: String, calories: Int, protein: Double = 0, carbs: Double = 0, fat: Double = 0, servingSize: String = "", date: String) {
        self.id = id
        self.memberId = memberId
        self.mealType = mealType
        self.foodName = foodName
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.servingSize = servingSize
        self.date = date
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Menstrual Cycle (NEW)
@Model
final class MenstrualCycle {
    var id: String
    var memberId: String
    var startDate: String
    var endDate: String
    var cycleLength: Int
    var periodLength: Int
    var symptoms: String
    var mood: String
    var flow: String
    var notes: String
    var isPersonal: Bool
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, memberId: String, startDate: String = "", endDate: String = "", cycleLength: Int = 28, periodLength: Int = 5, symptoms: String = "", mood: String = "", flow: String = "", notes: String = "", isPersonal: Bool = true) {
        self.id = id
        self.memberId = memberId
        self.startDate = startDate
        self.endDate = endDate
        self.cycleLength = cycleLength
        self.periodLength = periodLength
        self.symptoms = symptoms
        self.mood = mood
        self.flow = flow
        self.notes = notes
        self.isPersonal = isPersonal
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Water Log
@Model
final class WaterLog {
    var id: String
    var memberId: String
    var amountMl: Int
    var drinkType: String
    var date: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, memberId: String, amountMl: Int = 250, drinkType: String = "Su", date: String) {
        self.id = id
        self.memberId = memberId
        self.amountMl = amountMl
        self.drinkType = drinkType
        self.date = date
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Sleep Log
@Model
final class SleepLog {
    var id: String
    var memberId: String
    var bedtime: Date
    var wakeTime: Date
    var durationMinutes: Int
    var quality: String
    var interruptions: Int
    var notes: String
    var date: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, memberId: String, bedtime: Date, wakeTime: Date, quality: String = "orta", interruptions: Int = 0, notes: String = "", date: String) {
        self.id = id
        self.memberId = memberId
        self.bedtime = bedtime
        self.wakeTime = wakeTime
        self.durationMinutes = Int(wakeTime.timeIntervalSince(bedtime) / 60)
        self.quality = quality
        self.interruptions = interruptions
        self.notes = notes
        self.date = date
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - User Auth (NEW)
@Model
final class UserAuth {
    var id: String
    var pin: String
    var biometricEnabled: Bool
    var name: String
    var email: String
    var phone: String
    var createdAt: Date
    
    init(id: String = "main_user", pin: String = "", biometricEnabled: Bool = false, name: String = "", email: String = "", phone: String = "") {
        self.id = id
        self.pin = pin
        self.biometricEnabled = biometricEnabled
        self.name = name
        self.email = email
        self.phone = phone
        self.createdAt = Date()
    }
}

// MARK: - Sync Event (NEW)
@Model
final class SyncEvent {
    var id: String
    var tableName: String
    var recordId: String
    var action: String
    var data: String
    var deviceId: String
    var createdAt: Date
    var synced: Bool
    
    init(id: String = UUID().uuidString, tableName: String, recordId: String, action: String, data: String = "", deviceId: String = "", synced: Bool = false) {
        self.id = id
        self.tableName = tableName
        self.recordId = recordId
        self.action = action
        self.data = data
        self.deviceId = deviceId
        self.createdAt = Date()
        self.synced = synced
    }
}
