import Foundation
import SwiftData

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
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, senderName: String, senderId: String, content: String, channel: String = "genel") {
        self.id = id
        self.senderName = senderName
        self.senderId = senderId
        self.content = content
        self.channel = channel
        self.read = false
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
    var createdBy: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, title: String, content: String = "", category: String = "Genel", color: String = "#3498DB", isPinned: Bool = false, createdBy: String = "") {
        self.id = id
        self.title = title
        self.content = content
        self.category = category
        self.color = color
        self.isPinned = isPinned
        self.isArchived = false
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
    var repeatType: String
    var repeatDays: String
    var category: String
    var priority: String
    var alarmSound: String
    var vibrate: Bool
    var isCompleted: Bool
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
        self.category = category
        self.priority = priority
        self.alarmSound = alarmSound
        self.vibrate = true
        self.isCompleted = false
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
    var status: String
    var createdBy: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, title: String, amount: Double, category: String = "Genel", dueDate: String = "", notes: String = "", status: String = "pending", createdBy: String = "") {
        self.id = id
        self.title = title
        self.amount = amount
        self.category = category
        self.dueDate = dueDate
        self.notes = notes
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
    var category: String
    var amount: Double
    var description: String
    var expenseDate: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, category: String, amount: Double, description: String = "", expenseDate: String) {
        self.id = id
        self.category = category
        self.amount = amount
        self.description = description
        self.expenseDate = expenseDate
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}

// MARK: - Workout Log
@Model
final class WorkoutLog {
    var id: String
    var memberId: String
    var workoutType: String
    var duration: Int
    var caloriesBurned: Int
    var date: String
    var notes: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, memberId: String, workoutType: String, duration: Int, caloriesBurned: Int, date: String, notes: String = "") {
        self.id = id
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
    var date: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, memberId: String, mealType: String, foodName: String, calories: Int, protein: Double = 0, carbs: Double = 0, fat: Double = 0, date: String) {
        self.id = id
        self.memberId = memberId
        self.mealType = mealType
        self.foodName = foodName
        self.calories = calories
        self.protein = protein
        self.carbs = carbs
        self.fat = fat
        self.date = date
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
    var date: String
    var createdAt: Date
    var syncVersion: Date
    
    init(id: String = UUID().uuidString, memberId: String, bedtime: Date, wakeTime: Date, quality: String = "orta", interruptions: Int = 0, date: String) {
        self.id = id
        self.memberId = memberId
        self.bedtime = bedtime
        self.wakeTime = wakeTime
        self.durationMinutes = Int(wakeTime.timeIntervalSince(bedtime) / 60)
        self.quality = quality
        self.interruptions = interruptions
        self.date = date
        self.createdAt = Date()
        self.syncVersion = Date()
    }
}
